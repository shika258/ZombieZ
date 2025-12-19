package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les effets passifs des talents du Chasseur
 * Gere tous les procs et effets des 40 talents Chasseur
 */
public class ChasseurTalentListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // === Cooldowns internes ===
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // === Tracking pour talents specifiques ===

    // Marque du Chasseur - marked enemies
    private final Map<UUID, Set<UUID>> markedEnemies = new ConcurrentHashMap<>();
    private final Map<UUID, Long> markExpiry = new ConcurrentHashMap<>();

    // Rafale - combo tracking
    private final Map<UUID, UUID> lastTargetHit = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();

    // Chasseur Agile - dodge tracking
    private final Map<UUID, Long> lastDodgeTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> dodgeDamageBoost = new ConcurrentHashMap<>();

    // Invisibilite tracking
    private final Map<UUID, Long> invisStartTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> stealthAttacksRemaining = new ConcurrentHashMap<>();

    // Poison tracking
    private final Map<UUID, Map<UUID, Integer>> poisonStacks = new ConcurrentHashMap<>();

    // Surchauffe tracking
    private final Map<UUID, Integer> overheatStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShotTime = new ConcurrentHashMap<>();

    // Gatling mode
    private final Map<UUID, Long> gatlingModeEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> consecutiveShots = new ConcurrentHashMap<>();

    // Bounty Hunter buff
    private final Map<UUID, Long> bountyBuffEnd = new ConcurrentHashMap<>();

    // Sharpshooter - still tracking
    private final Map<UUID, Long> stillSince = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> guaranteedCrit = new ConcurrentHashMap<>();

    // Death Note targets
    private final Map<UUID, Long> deathNoteTargets = new ConcurrentHashMap<>();

    // Movement tracking for Void Walker
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isMoving = new ConcurrentHashMap<>();

    // Cache des joueurs Chasseurs actifs
    private final Set<UUID> activeChasseurs = ConcurrentHashMap.newKeySet();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 2000;

    public ChasseurTalentListener(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        // Note: registration handled by ZombieZPlugin, not here

        startPeriodicTasks();
    }

    private void updateChasseursCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_TTL) return;
        lastCacheUpdate = now;

        activeChasseurs.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassData data = plugin.getClassManager().getClassData(player);
            if (data.hasClass() && data.getSelectedClass() == ClassType.CHASSEUR) {
                activeChasseurs.add(player.getUniqueId());
            }
        }
    }

    // ==================== DEGATS INFLIGES ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        double damage = event.getDamage();
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        boolean isRanged = isRangedAttack(event.getDamager());

        // Track shots
        lastShotTime.put(uuid, System.currentTimeMillis());

        // === TIER 1 ===

        // Tirs Multiples
        Talent multiShot = getActiveTalentIfHas(player, Talent.TalentEffectType.MULTI_SHOT);
        if (multiShot != null && isRanged && !isOnCooldown(uuid, "multi_shot")) {
            if (Math.random() < multiShot.getValue(0)) {
                procMultiShot(player, target, damage * multiShot.getValue(2));
                setCooldown(uuid, "multi_shot", multiShot.getInternalCooldownMs());
            }
        }

        // Oeil de Lynx - crit bonus
        Talent lynxEye = getActiveTalentIfHas(player, Talent.TalentEffectType.LYNX_EYE);
        if (lynxEye != null) {
            // Add crit chance and damage is handled via custom crit system
            if (Math.random() < lynxEye.getValue(0) || guaranteedCrit.getOrDefault(uuid, false)) {
                damage *= (1.5 + lynxEye.getValue(1)); // Base crit + bonus
                guaranteedCrit.put(uuid, false);

                // Tireur d'elite - guaranteed crit used
                // Oeil du Predateur - crit resets dodge
                Talent predatorEye = getActiveTalentIfHas(player, Talent.TalentEffectType.PREDATOR_EYE);
                if (predatorEye != null && Math.random() < predatorEye.getValue(0)) {
                    lastDodgeTime.remove(uuid);
                    if (shouldSendTalentMessage(player)) {
                        player.sendMessage("§b! Esquive prete!");
                    }
                }
            }
        }

        // Dodge damage boost (Chasseur Agile)
        if (dodgeDamageBoost.getOrDefault(uuid, false)) {
            Talent agileHunter = getActiveTalentIfHas(player, Talent.TalentEffectType.AGILE_HUNTER);
            if (agileHunter != null) {
                damage *= (1 + agileHunter.getValue(2));
                dodgeDamageBoost.put(uuid, false);
            }
        }

        // Marque du Chasseur
        Talent hunterMark = getActiveTalentIfHas(player, Talent.TalentEffectType.HUNTER_MARK);
        if (hunterMark != null) {
            applyMark(player, target, (long) hunterMark.getValue(0));
        }

        // Damage amp from mark
        if (isMarked(player, target)) {
            double markDamageBonus = 0.15; // Base
            Talent tracker = getActiveTalentIfHas(player, Talent.TalentEffectType.TRACKER);
            if (tracker != null) {
                markDamageBonus = tracker.getValue(0); // 25%
            }
            damage *= (1 + markDamageBonus);

            // Executeur de Primes
            Talent bountyExecutioner = getActiveTalentIfHas(player, Talent.TalentEffectType.BOUNTY_EXECUTIONER);
            if (bountyExecutioner != null) {
                double threshold = bountyExecutioner.getValue(0);
                double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (target.getHealth() / maxHp < threshold) {
                    damage = target.getHealth() + 1000; // Instakill
                }
            }
        }

        // Fleches Percantes
        Talent piercing = getActiveTalentIfHas(player, Talent.TalentEffectType.PIERCING_ARROWS);
        if (piercing != null && isRanged) {
            procPiercing(player, target, damage * piercing.getValue(1));
        }

        // === TIER 2 ===

        // Rafale - combo
        Talent burstShot = getActiveTalentIfHas(player, Talent.TalentEffectType.BURST_SHOT);
        if (burstShot != null) {
            UUID lastTarget = lastTargetHit.get(uuid);
            if (lastTarget != null && lastTarget.equals(targetUuid)) {
                int combo = comboCounter.merge(uuid, 1, Integer::sum);
                if (combo >= burstShot.getValue(0)) {
                    damage *= (1 + burstShot.getValue(1));
                    comboCounter.put(uuid, 0);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                }
            } else {
                comboCounter.put(uuid, 1);
            }
            lastTargetHit.put(uuid, targetUuid);
        }

        // Sniper - distance bonus
        Talent sniper = getActiveTalentIfHas(player, Talent.TalentEffectType.SNIPER);
        if (sniper != null && isRanged) {
            double distance = player.getLocation().distance(target.getLocation());
            if (distance > sniper.getValue(0)) {
                double bonus = Math.min(sniper.getValue(2), (distance - sniper.getValue(0)) * sniper.getValue(1));
                damage *= (1 + bonus);
            }
        }

        // Fantome - stealth crit bonus
        Talent ghost = getActiveTalentIfHas(player, Talent.TalentEffectType.GHOST);
        if (ghost != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            Long invisStart = invisStartTime.get(uuid);
            if (invisStart != null && System.currentTimeMillis() - invisStart >= ghost.getValue(0)) {
                damage *= (1 + ghost.getValue(1)); // +100% crit damage
            }
        }

        // Venin - poison
        Talent venom = getActiveTalentIfHas(player, Talent.TalentEffectType.VENOM);
        if (venom != null) {
            applyPoison(player, target, damage, venom);
        }

        // Ricochet
        Talent ricochet = getActiveTalentIfHas(player, Talent.TalentEffectType.RICOCHET);
        if (ricochet != null && isRanged && Math.random() < ricochet.getValue(0)) {
            procRicochet(player, target, damage * ricochet.getValue(1), ricochet.getValue(2));
        }

        // === TIER 3 ===

        // Pluie de Fleches
        Talent arrowRain = getActiveTalentIfHas(player, Talent.TalentEffectType.ARROW_RAIN);
        if (arrowRain != null && isRanged && !isOnCooldown(uuid, "arrow_rain")) {
            if (Math.random() < arrowRain.getValue(0)) {
                procArrowRain(player, target.getLocation(), arrowRain);
                setCooldown(uuid, "arrow_rain", arrowRain.getInternalCooldownMs());
            }
        }

        // Tireur d'elite tracking is done in movement
        Talent sharpshooter = getActiveTalentIfHas(player, Talent.TalentEffectType.SHARPSHOOTER);
        if (sharpshooter != null) {
            stillSince.remove(uuid); // Reset still timer on attack
        }

        // === TIER 4 ===

        // Surchauffe
        Talent overheat = getActiveTalentIfHas(player, Talent.TalentEffectType.OVERHEAT);
        if (overheat != null && isRanged) {
            int stacks = overheatStacks.merge(uuid, 1, Integer::sum);
            double maxBonus = overheat.getValue(1);
            double bonus = Math.min(maxBonus, stacks * overheat.getValue(0));
            damage *= (1 + bonus);
        }

        // === TIER 5 ===

        // Spectre - stealth attacks
        Talent spectre = getActiveTalentIfHas(player, Talent.TalentEffectType.SPECTRE);
        if (spectre != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            int remaining = stealthAttacksRemaining.getOrDefault(uuid, (int) spectre.getValue(0));
            if (remaining > 0) {
                stealthAttacksRemaining.put(uuid, remaining - 1);
            } else {
                // Break stealth
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                stealthAttacksRemaining.remove(uuid);
            }
        } else if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // Maitre des Ombres check
            Talent shadowMaster = getActiveTalentIfHas(player, Talent.TalentEffectType.SHADOW_MASTER);
            if (shadowMaster == null) {
                // Break stealth normally
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        // Bounty Hunter buff active
        if (bountyBuffEnd.containsKey(uuid) && System.currentTimeMillis() < bountyBuffEnd.get(uuid)) {
            Talent bountyHunter = getActiveTalentIfHas(player, Talent.TalentEffectType.BOUNTY_HUNTER);
            if (bountyHunter != null) {
                damage *= (1 + bountyHunter.getValue(1));
            }
        }

        // === TIER 6 ===

        // Gatling mode
        if (gatlingModeEnd.containsKey(uuid) && System.currentTimeMillis() < gatlingModeEnd.get(uuid)) {
            // In gatling mode - handled by periodic task for auto attacks
        }

        // Gatling activation
        Talent gatling = getActiveTalentIfHas(player, Talent.TalentEffectType.GATLING);
        if (gatling != null && isRanged) {
            int shots = consecutiveShots.merge(uuid, 1, Integer::sum);
            if (shots >= gatling.getValue(0)) {
                gatlingModeEnd.put(uuid, System.currentTimeMillis() + (long) gatling.getValue(1));
                consecutiveShots.put(uuid, 0);
                if (shouldSendTalentMessage(player)) {
                    player.sendMessage("§c§l+ MODE GATLING ACTIVE!");
                }
                player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 2.0f);
            }
        }

        // === TIER 7 ===

        // Reaper - stealth execute
        Talent stealthReaper = getActiveTalentIfHas(player, Talent.TalentEffectType.STEALTH_REAPER);
        if (stealthReaper != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (target.getHealth() / maxHp < stealthReaper.getValue(0)) {
                damage = target.getHealth() + 1000; // Instakill
            }
        }

        // === TIER 8 ===

        // Void Walker - handled in damage received

        event.setDamage(damage);
    }

    // ==================== DEGATS RECUS ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        double damage = event.getDamage();
        UUID uuid = player.getUniqueId();

        // Void Walker - DR while moving (capped a 45%)
        Talent voidWalker = getActiveTalentIfHas(player, Talent.TalentEffectType.VOID_WALKER);
        if (voidWalker != null && isMoving.getOrDefault(uuid, false)) {
            double dr = Math.min(voidWalker.getValue(0), 0.50); // Cap standardise a 50% pour toutes les classes
            damage *= (1 - dr);
        }

        // Maitre des Ombres - break stealth on damage
        Talent shadowMaster = getActiveTalentIfHas(player, Talent.TalentEffectType.SHADOW_MASTER);
        if (shadowMaster != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            invisStartTime.remove(uuid);
        }

        event.setDamage(damage);
    }

    // ==================== KILLS ====================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!(target.getKiller() instanceof Player player)) return;

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // Predateur Supreme - extend stealth on kill
        Talent supremePredator = getActiveTalentIfHas(player, Talent.TalentEffectType.SUPREME_PREDATOR);
        if (supremePredator != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            PotionEffect invis = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            if (invis != null) {
                int newDuration = Math.min((int)(supremePredator.getValue(1) / 50),
                    invis.getDuration() + (int)(supremePredator.getValue(0) / 50));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, newDuration, 0, false, false));
            }
        }

        // Sentence de Mort - mark explosion
        Talent deathSentence = getActiveTalentIfHas(player, Talent.TalentEffectType.DEATH_SENTENCE);
        if (deathSentence != null && isMarked(player, target)) {
            double damage = event.getEntity().getLastDamageCause() != null ?
                event.getEntity().getLastDamageCause().getFinalDamage() * deathSentence.getValue(0) : 10;
            procMarkExplosion(player, target.getLocation(), damage, deathSentence.getValue(1));
            removeMark(player, target);
        }

        // Pandemie - poison spread on death
        Talent pandemic = getActiveTalentIfHas(player, Talent.TalentEffectType.PANDEMIC);
        if (pandemic != null) {
            Map<UUID, Integer> playerPoisons = poisonStacks.get(uuid);
            if (playerPoisons != null && playerPoisons.containsKey(targetUuid)) {
                int stacks = playerPoisons.get(targetUuid);
                spreadPoison(player, target.getLocation(), stacks, pandemic.getValue(0));
                playerPoisons.remove(targetUuid);
            }
        }

        // Chasseur de Primes - mark kill reward
        Talent bountyHunter = getActiveTalentIfHas(player, Talent.TalentEffectType.BOUNTY_HUNTER);
        if (bountyHunter != null && isMarked(player, target)) {
            // Heal
            double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bountyHunter.getValue(0);
            player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(),
                player.getHealth() + heal));
            // Damage buff
            bountyBuffEnd.put(uuid, System.currentTimeMillis() + (long) bountyHunter.getValue(2));
            if (shouldSendTalentMessage(player)) {
                player.sendMessage("§6+ Prime collectee! +20% degats!");
            }
            removeMark(player, target);
        }

        // Clear poison tracking
        Map<UUID, Integer> playerPoisons = poisonStacks.get(uuid);
        if (playerPoisons != null) {
            playerPoisons.remove(targetUuid);
        }
    }

    // ==================== MOVEMENT ====================

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        UUID uuid = player.getUniqueId();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Track movement for Void Walker
        boolean moved = from.getX() != to.getX() || from.getZ() != to.getZ();
        isMoving.put(uuid, moved);
        lastLocation.put(uuid, to.clone());

        // Sharpshooter - still tracking
        Talent sharpshooter = getActiveTalentIfHas(player, Talent.TalentEffectType.SHARPSHOOTER);
        if (sharpshooter != null) {
            if (moved) {
                stillSince.remove(uuid);
                guaranteedCrit.put(uuid, false);
            } else {
                if (!stillSince.containsKey(uuid)) {
                    stillSince.put(uuid, System.currentTimeMillis());
                } else {
                    long stillTime = System.currentTimeMillis() - stillSince.get(uuid);
                    if (stillTime >= sharpshooter.getValue(0) && !guaranteedCrit.getOrDefault(uuid, false)) {
                        guaranteedCrit.put(uuid, true);
                        if (shouldSendTalentMessage(player)) {
                            player.sendMessage("§e! Critique garanti!");
                        }
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2.0f);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        UUID uuid = player.getUniqueId();

        // Bullet Time activation (crouch + already crouching = toggle)
        Talent bulletTime = getActiveTalentIfHas(player, Talent.TalentEffectType.BULLET_TIME);
        if (bulletTime != null && event.isSneaking() && !isOnCooldown(uuid, "bullet_time")) {
            // Check if jumping (velocity Y > 0)
            if (player.getVelocity().getY() > 0.1) {
                procBulletTime(player, bulletTime);
                setCooldown(uuid, "bullet_time", (long) bulletTime.getValue(2));
            }
        }
    }

    // ==================== TACHES PERIODIQUES OPTIMISEES ====================

    private void startPeriodicTasks() {
        // FAST TICK (10L = 0.5s) - Auto shots
        new BukkitRunnable() {
            @Override
            public void run() {
                updateChasseursCache();
                if (activeChasseurs.isEmpty()) return;

                for (UUID uuid : activeChasseurs) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    // Living Arsenal auto shots
                    Talent livingArsenal = getActiveTalentIfHas(player, Talent.TalentEffectType.LIVING_ARSENAL);
                    if (livingArsenal != null) {
                        procAutoShot(player, livingArsenal);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // NORMAL TICK (20L = 1s) - Cleanup, poison, auras
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChasseurs.isEmpty()) return;
                long now = System.currentTimeMillis();

                // Overheat reset
                lastShotTime.forEach((uuid, time) -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) return;
                    Talent overheat = getActiveTalentIfHas(player, Talent.TalentEffectType.OVERHEAT);
                    if (overheat != null && now - time > overheat.getValue(2)) {
                        overheatStacks.remove(uuid);
                        consecutiveShots.remove(uuid);
                    }
                });

                // Mark expiry cleanup
                markExpiry.entrySet().removeIf(entry -> now >= entry.getValue());

                // Death Note delayed kill
                deathNoteTargets.entrySet().removeIf(entry -> {
                    if (now >= entry.getValue()) {
                        Entity entity = Bukkit.getEntity(entry.getKey());
                        if (entity instanceof LivingEntity target && target.isValid()) {
                            double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                            boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                                    target.getScoreboardTags().contains("elite") ||
                                                    maxHp > 50;
                            if (isBossOrElite) {
                                // Boss/Elite: 30% max HP damage (capped)
                                target.damage(maxHp * 0.30);
                            } else {
                                target.damage(target.getHealth() + 1000);
                            }
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                        }
                        return true;
                    }
                    return false;
                });

                // Poison tick damage
                poisonStacks.forEach((playerUuid, targets) -> {
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player == null) return;

                    targets.forEach((targetUuid, stacks) -> {
                        Entity entity = Bukkit.getEntity(targetUuid);
                        if (entity instanceof LivingEntity target && target.isValid()) {
                            double baseDamage = 2.0 * Math.min(stacks, 10); // Cap stacks a 10

                            Talent deadlyToxins = getActiveTalentIfHas(player, Talent.TalentEffectType.DEADLY_TOXINS);
                            if (deadlyToxins != null) {
                                Talent lynxEye = getActiveTalentIfHas(player, Talent.TalentEffectType.LYNX_EYE);
                                if (lynxEye != null && Math.random() < lynxEye.getValue(0)) {
                                    baseDamage *= 1.5;
                                }
                                if (target instanceof Mob mob) {
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                                        40, Math.min((int)(deadlyToxins.getValue(0) * 10), 2), false, false));
                                }
                            }

                            Talent epidemic = getActiveTalentIfHas(player, Talent.TalentEffectType.EPIDEMIC);
                            if (epidemic != null && stacks >= epidemic.getValue(0)) {
                                baseDamage *= epidemic.getValue(1);
                            }

                            Talent blackPlague = getActiveTalentIfHas(player, Talent.TalentEffectType.BLACK_PLAGUE);
                            if (blackPlague != null) {
                                double heal = baseDamage * blackPlague.getValue(1);
                                heal = Math.min(heal, player.getMaxHealth() * 0.03); // Cap 3%/s
                                player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(),
                                    player.getHealth() + heal));
                            }

                            target.damage(baseDamage, player);
                            target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0),
                                3, 0.2, 0.2, 0.2, 0);
                        }
                    });
                });

                // Toxic Apocalypse aura
                for (UUID uuid : activeChasseurs) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent toxicApoc = getActiveTalentIfHas(player, Talent.TalentEffectType.TOXIC_APOCALYPSE);
                    if (toxicApoc != null) {
                        procToxicAura(player, toxicApoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // SLOW TICK (40L = 2s) - Blight spread
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChasseurs.isEmpty()) return;
                for (UUID uuid : activeChasseurs) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent blight = getActiveTalentIfHas(player, Talent.TalentEffectType.BLIGHT);
                    if (blight != null) {
                        procBlightSpread(player, blight);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);

        // STEEL STORM (15s)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChasseurs.isEmpty()) return;
                for (UUID uuid : activeChasseurs) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent steelStorm = getActiveTalentIfHas(player, Talent.TalentEffectType.STEEL_STORM);
                    if (steelStorm != null && !isOnCooldown(uuid, "steel_storm")) {
                        procSteelStorm(player, steelStorm);
                        setCooldown(uuid, "steel_storm", (long) steelStorm.getValue(0));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 15, 20L * 15);

        // ORBITAL STRIKE (45s)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChasseurs.isEmpty()) return;
                for (UUID uuid : activeChasseurs) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent orbital = getActiveTalentIfHas(player, Talent.TalentEffectType.ORBITAL_STRIKE);
                    if (orbital != null && !isOnCooldown(uuid, "orbital_strike")) {
                        procOrbitalStrike(player, orbital);
                        setCooldown(uuid, "orbital_strike", (long) orbital.getValue(0));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 45, 20L * 45);
    }

    // ==================== PROCS ====================

    private void procMultiShot(Player player, LivingEntity target, double damage) {
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        Vector dir = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();

        // Calculer le vecteur perpendiculaire horizontal (pour le décalage côte à côte)
        Vector horizontal = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        // Décalage horizontal pour les flèches (I I I pattern)
        double spacing = 0.6; // Espacement entre les flèches

        // Son de tir multiple
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.3f);

        // Tirer 2 flèches côte à côte horizontalement
        for (int i = 0; i < 2; i++) {
            // Décalage: -0.5 et +0.5 pour 2 flèches côte à côte
            double offset = (i == 0 ? -1 : 1) * spacing / 2;
            Location spawnLoc = playerLoc.clone().add(horizontal.clone().multiply(offset));

            // Créer une vraie flèche
            Arrow arrow = player.getWorld().spawnArrow(spawnLoc, dir.clone(), 2.5f, 0);
            arrow.setShooter(player);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setDamage(damage / 2); // Dégâts de la flèche (60% répartis)
            arrow.setGravity(true);
            arrow.setCritical(true);

            // Particules de traînée dorée pour distinguer les flèches bonus
            final Arrow finalArrow = arrow;
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (finalArrow.isDead() || finalArrow.isOnGround() || ticks > 60) {
                        this.cancel();
                        return;
                    }
                    // Particules dorées pour les flèches bonus
                    finalArrow.getWorld().spawnParticle(Particle.DUST, finalArrow.getLocation(), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.8f));
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }

    private void procPiercing(Player player, LivingEntity target, double damage) {
        Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        Location behind = target.getLocation().add(dir.multiply(2));

        for (Entity entity : target.getWorld().getNearbyEntities(behind, 2, 2, 2)) {
            if (entity instanceof LivingEntity nearby && entity != player && entity != target) {
                nearby.damage(damage, player);
                nearby.getWorld().spawnParticle(Particle.CRIT, nearby.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                break;
            }
        }
    }

    private void procRicochet(Player player, LivingEntity target, double damage, double range) {
        for (Entity entity : target.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity nearby && entity != player && entity != target) {
                nearby.damage(damage, player);
                nearby.getWorld().playSound(nearby.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.5f);
                nearby.getWorld().spawnParticle(Particle.ENCHANTED_HIT, nearby.getLocation(), 10, 0.3, 0.3, 0.3, 0);
                break;
            }
        }
    }

    private void procArrowRain(Player player, Location center, Talent talent) {
        double damagePerArrow = 5 * talent.getValue(1);
        int arrows = (int) talent.getValue(2);
        double radius = talent.getValue(3);

        // Deluge upgrade
        Talent deluge = getActiveTalentIfHas(player, Talent.TalentEffectType.DELUGE);
        int waves = deluge != null ? 3 : 1;
        boolean pierce = deluge != null;

        // Armageddon upgrade
        Talent armageddon = getActiveTalentIfHas(player, Talent.TalentEffectType.AERIAL_ARMAGEDDON);
        boolean canCrit = armageddon != null;

        // Meteor Shower upgrade
        Talent meteorShower = getActiveTalentIfHas(player, Talent.TalentEffectType.METEOR_SHOWER);
        if (meteorShower != null) {
            procMeteorShower(player, center, meteorShower);
            return;
        }

        // Son d'annonce
        player.getWorld().playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.5f, 0.5f);
        player.getWorld().playSound(center, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 0.8f);

        for (int wave = 0; wave < waves; wave++) {
            int finalWave = wave;
            boolean finalPierce = pierce;
            boolean finalCanCrit = canCrit;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Son de volée de flèches
                center.getWorld().playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

                for (int i = 0; i < arrows; i++) {
                    // Position de chute aléatoire dans le rayon
                    double x = center.getX() + (Math.random() - 0.5) * radius * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * radius * 2;
                    Location impactLoc = new Location(center.getWorld(), x, center.getY(), z);

                    // Spawner une vraie flèche qui tombe du ciel
                    Location spawnLoc = impactLoc.clone().add(0, 15 + Math.random() * 5, 0);
                    int arrowDelay = (int) (Math.random() * 10); // Décalage pour effet de pluie

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Arrow arrow = center.getWorld().spawnArrow(spawnLoc, new Vector(0, -3, 0), 2.0f, 0);
                        arrow.setShooter(player);
                        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        arrow.setDamage(0); // Dégâts gérés manuellement
                        arrow.setGravity(true);
                        arrow.setPierceLevel(finalPierce ? 3 : 0);

                        // Particules de traînée sur la flèche
                        new BukkitRunnable() {
                            int ticks = 0;
                            @Override
                            public void run() {
                                if (arrow.isDead() || arrow.isOnGround() || ticks > 40) {
                                    // Impact au sol
                                    Location loc = arrow.getLocation();
                                    center.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.3, 0.1, 0.3, 0.1);
                                    center.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.2, 0.1, 0.2, 0,
                                        new Particle.DustOptions(Color.fromRGB(139, 90, 43), 1.0f));

                                    // Appliquer les dégâts aux entités proches
                                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                                        if (entity instanceof LivingEntity target && entity != player && !(entity instanceof ArmorStand)) {
                                            double finalDamage = damagePerArrow;

                                            // Crit check
                                            if (finalCanCrit) {
                                                Talent lynxEye = getActiveTalentIfHas(player, Talent.TalentEffectType.LYNX_EYE);
                                                if (lynxEye != null && Math.random() < lynxEye.getValue(0)) {
                                                    finalDamage *= 1.5;
                                                    loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
                                                }
                                            }

                                            target.damage(finalDamage, player);
                                            if (!finalPierce) break;
                                        }
                                    }

                                    arrow.remove();
                                    this.cancel();
                                    return;
                                }
                                // Particules de traînée
                                center.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 1, 0, 0, 0, 0);
                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                    }, arrowDelay);
                }
            }, wave * 25L);
        }
    }

    private void procMeteorShower(Player player, Location center, Talent talent) {
        double damagePerMeteor = 10 * talent.getValue(0);
        int meteors = (int) talent.getValue(1);
        double zoneRadius = talent.getValue(2);
        double explosionRadius = talent.getValue(3);

        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§c§l+ METEOR SHOWER!");
        }

        for (int i = 0; i < meteors; i++) {
            int delay = i * 5;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double x = center.getX() + (Math.random() - 0.5) * zoneRadius * 2;
                double z = center.getZ() + (Math.random() - 0.5) * zoneRadius * 2;
                Location meteorLoc = new Location(center.getWorld(), x, center.getY(), z);

                // Visual
                center.getWorld().spawnParticle(Particle.EXPLOSION, meteorLoc, 1);
                center.getWorld().spawnParticle(Particle.FLAME, meteorLoc, 30, explosionRadius/2, 0.5, explosionRadius/2, 0.1);
                center.getWorld().playSound(meteorLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);

                // Damage
                for (Entity entity : center.getWorld().getNearbyEntities(meteorLoc, explosionRadius, explosionRadius, explosionRadius)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        target.damage(damagePerMeteor, player);
                    }
                }
            }, delay);
        }
    }

    private void procSteelStorm(Player player, Talent talent) {
        Location center = player.getLocation();
        double damagePerArrow = 5 * talent.getValue(1);
        int arrows = (int) talent.getValue(2);
        double radius = talent.getValue(3);

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§e§l+ TEMPETE D'ACIER!");
        }
        player.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.5f);

        for (int i = 0; i < arrows; i++) {
            double x = center.getX() + (Math.random() - 0.5) * radius * 2;
            double z = center.getZ() + (Math.random() - 0.5) * radius * 2;
            Location arrowLoc = new Location(center.getWorld(), x, center.getY(), z);

            center.getWorld().spawnParticle(Particle.CRIT, arrowLoc, 5, 0.1, 0.1, 0.1, 0);

            for (Entity entity : center.getWorld().getNearbyEntities(arrowLoc, 1, 1, 1)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    target.damage(damagePerArrow, player);
                }
            }
        }
    }

    private void procOrbitalStrike(Player player, Talent talent) {
        Location center = player.getLocation();
        double damage = 10 * talent.getValue(1);
        double radius = talent.getValue(2);

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§6§l+ ORBITAL STRIKE!");
        }
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);

        // Warning
        player.getWorld().spawnParticle(Particle.END_ROD, center, 100, radius/2, 5, radius/2, 0);

        // Delayed impact
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, radius/2, 1, radius/2, 0);
            player.getWorld().spawnParticle(Particle.FLASH, center, 3);
            player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);

            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    target.damage(damage, player);
                }
            }
        }, 40L);
    }

    private void procToxicAura(Player player, Talent talent) {
        Location center = player.getLocation();
        double radius = talent.getValue(0);
        double damagePerSecond = 5 * talent.getValue(1);

        // Visual
        player.getWorld().spawnParticle(Particle.ITEM_SLIME, center, 20, radius/2, 1, radius/2, 0);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damagePerSecond, player);

                // Apply poison effects from other talents
                Talent venom = getActiveTalentIfHas(player, Talent.TalentEffectType.VENOM);
                if (venom != null) {
                    applyPoison(player, target, damagePerSecond, venom);
                }
            }
        }
    }

    private void procAutoShot(Player player, Talent talent) {
        double damage = 5 * talent.getValue(1);
        double range = talent.getValue(2);

        LivingEntity closest = null;
        double closestDist = range;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                double dist = player.getLocation().distance(target.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = target;
                }
            }
        }

        if (closest != null) {
            closest.damage(damage, player);
            player.getWorld().spawnParticle(Particle.CRIT, closest.getLocation(), 5, 0.2, 0.2, 0.2, 0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.3f, 1.5f);
        }
    }

    private void procBlightSpread(Player player, Talent talent) {
        double range = talent.getValue(0);
        Map<UUID, Integer> playerPoisons = poisonStacks.get(player.getUniqueId());
        if (playerPoisons == null || playerPoisons.isEmpty()) return;

        Set<UUID> newInfections = new HashSet<>();

        for (Map.Entry<UUID, Integer> entry : playerPoisons.entrySet()) {
            Entity infected = Bukkit.getEntity(entry.getKey());
            if (!(infected instanceof LivingEntity infectedEntity) || !infected.isValid()) continue;

            for (Entity entity : infected.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity target && entity != player &&
                    !playerPoisons.containsKey(target.getUniqueId())) {
                    newInfections.add(target.getUniqueId());
                    playerPoisons.put(target.getUniqueId(), 1);
                    target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation(), 10, 0.3, 0.3, 0.3, 0);
                }
            }
        }
    }

    private void procBulletTime(Player player, Talent talent) {
        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§b§l+ BULLET TIME!");
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);

        long duration = (long) talent.getValue(0);
        double slowFactor = talent.getValue(1);

        // Slow all nearby mobs
        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Mob mob) {
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    (int)(duration / 50), 4, false, false));
            }
        }

        // Visual effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(duration / 50), 1, false, false));
    }

    private void procMarkExplosion(Player player, Location center, double damage, double radius) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }
    }

    // ==================== MARK SYSTEM ====================

    private void applyMark(Player player, LivingEntity target, long duration) {
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Set<UUID> marks = markedEnemies.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        // Legendary Hunter - 5 marks max, infinite duration
        Talent legendaryHunter = getActiveTalentIfHas(player, Talent.TalentEffectType.LEGENDARY_HUNTER);
        if (legendaryHunter != null) {
            if (marks.size() < legendaryHunter.getValue(0)) {
                marks.add(targetUuid);
            }
            // Don't set expiry - infinite duration
        } else {
            marks.clear(); // Only 1 mark at a time normally
            marks.add(targetUuid);
            markExpiry.put(targetUuid, System.currentTimeMillis() + duration);
        }

        // Death Note check
        Talent deathNote = getActiveTalentIfHas(player, Talent.TalentEffectType.DEATH_NOTE);
        if (deathNote != null && !isOnCooldown(uuid, "death_note")) {
            deathNoteTargets.put(targetUuid, System.currentTimeMillis() + (long) deathNote.getValue(0));
            setCooldown(uuid, "death_note", (long) deathNote.getValue(2));
            if (shouldSendTalentMessage(player)) {
                player.sendMessage("§0§l+ DEATH NOTE: §7Cible marquee pour la mort!");
            }
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
        }

        // Visual
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0, 2, 0), 5, 0.2, 0.2, 0.2, 0);
    }

    private boolean isMarked(Player player, LivingEntity target) {
        Set<UUID> marks = markedEnemies.get(player.getUniqueId());
        if (marks == null) return false;

        UUID targetUuid = target.getUniqueId();
        if (!marks.contains(targetUuid)) return false;

        // Check expiry
        Long expiry = markExpiry.get(targetUuid);
        if (expiry != null && System.currentTimeMillis() >= expiry) {
            marks.remove(targetUuid);
            markExpiry.remove(targetUuid);
            return false;
        }

        return true;
    }

    private void removeMark(Player player, LivingEntity target) {
        Set<UUID> marks = markedEnemies.get(player.getUniqueId());
        if (marks != null) {
            marks.remove(target.getUniqueId());
        }
        markExpiry.remove(target.getUniqueId());
    }

    // ==================== POISON SYSTEM ====================

    private void applyPoison(Player player, LivingEntity target, double baseDamage, Talent venom) {
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Map<UUID, Integer> playerPoisons = poisonStacks.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        playerPoisons.merge(targetUuid, 1, Integer::sum);

        // Visual
        target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
    }

    private void spreadPoison(Player player, Location center, int stacks, double range) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof LivingEntity target && entity != player) {
                Map<UUID, Integer> playerPoisons = poisonStacks.computeIfAbsent(player.getUniqueId(),
                    k -> new ConcurrentHashMap<>());
                playerPoisons.put(target.getUniqueId(), stacks);
                target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation(), 15, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    // ==================== UTILITAIRES ====================

    private Player getPlayerFromDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    private boolean isRangedAttack(Entity damager) {
        return damager instanceof Projectile;
    }

    private Talent getActiveTalentIfHas(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentWithEffect(player, effectType);
    }

    private boolean shouldSendTalentMessage(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data != null && data.isTalentMessagesEnabled();
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
}
