package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les effets de talents de l'Occultiste
 * Gere tous les effets passifs et procs des 40 talents
 */
public class OccultisteTalentListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // ==================== TRACKING SYSTEMS ====================

    // Soul orbs par joueur
    private final Map<UUID, Integer> soulOrbs = new ConcurrentHashMap<>();

    // Ennemis en feu (entity UUID -> expiry timestamp)
    private final Map<UUID, Long> burningEnemies = new ConcurrentHashMap<>();

    // Ennemis geles (entity UUID -> freeze start timestamp)
    private final Map<UUID, Long> frozenEnemies = new ConcurrentHashMap<>();

    // Rifts actifs (rift ID -> RiftData)
    private final Map<UUID, RiftData> activeRifts = new ConcurrentHashMap<>();

    // Minions invoques (player UUID -> list of minion UUIDs)
    private final Map<UUID, List<UUID>> playerMinions = new ConcurrentHashMap<>();

    // Cooldowns internes
    private final Map<UUID, Map<String, Long>> internalCooldowns = new ConcurrentHashMap<>();

    // Black Sun actif (player UUID -> end timestamp)
    private final Map<UUID, Long> blackSunActive = new ConcurrentHashMap<>();

    // Time Stasis actif (player UUID -> end timestamp)
    private final Map<UUID, Long> timeStasisActive = new ConcurrentHashMap<>();

    // Erasure charging (player UUID -> charge start timestamp)
    private final Map<UUID, Long> erasureCharging = new ConcurrentHashMap<>();

    // Ice zones (location hash -> expiry timestamp)
    private final Map<String, Long> iceZones = new ConcurrentHashMap<>();

    public OccultisteTalentListener(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;

        // Demarrer les taches periodiques
        startPeriodicTasks();
    }

    // ==================== TACHES PERIODIQUES ====================

    private void startPeriodicTasks() {
        // Fire spread tick (every second)
        new BukkitRunnable() {
            @Override
            public void run() {
                processFireSpread();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Lightning storm tick (every 1.5s)
        new BukkitRunnable() {
            @Override
            public void run() {
                processLightningStorm();
            }
        }.runTaskTimer(plugin, 30L, 30L);

        // Perpetual storm tick (every 0.5s)
        new BukkitRunnable() {
            @Override
            public void run() {
                processPerpetualStorm();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Fire avatar aura tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processFireAvatar();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Inferno nova tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processInferno();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Black sun tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processBlackSun();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Eternal winter aura tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processEternalWinter();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Divine judgment tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processDivineJudgment();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Meteor rain tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processMeteorRain();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Rift tick (damage + pull)
        new BukkitRunnable() {
            @Override
            public void run() {
                processRifts();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Soul regen tick (Eternal Harvest)
        new BukkitRunnable() {
            @Override
            public void run() {
                processSoulRegen();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Blizzard aura tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processBlizzardAura();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Absolute Zero check tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processAbsoluteZero();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Ice zones tick
        new BukkitRunnable() {
            @Override
            public void run() {
                processIceZones();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Cleanup expired data
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredData();
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    // ==================== DAMAGE EVENTS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        Player player = getPlayerAttacker(event);
        if (player == null) return;
        if (!isOccultiste(player)) return;

        LivingEntity target = getTarget(event);
        if (target == null) return;

        double baseDamage = event.getDamage();
        double bonusDamage = 0;

        // Soul Pact - bonus damage per orb
        if (hasTalentEffect(player, Talent.TalentEffectType.SOUL_PACT)) {
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_PACT);
            int orbs = getSoulOrbs(player);
            double bonusPerOrb = talent.getValue(0);
            bonusDamage += baseDamage * (bonusPerOrb * orbs);
        }

        // Frozen Heart - bonus damage to frozen
        if (hasTalentEffect(player, Talent.TalentEffectType.FROZEN_HEART)) {
            if (isFrozen(target)) {
                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROZEN_HEART);
                bonusDamage += baseDamage * talent.getValue(0);
            }
        }

        // Eternal Winter - bonus damage to slowed
        if (hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_WINTER)) {
            if (target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_WINTER);
                bonusDamage += baseDamage * talent.getValue(2);
            }
        }

        // Apply bonus damage
        if (bonusDamage > 0) {
            event.setDamage(baseDamage + bonusDamage);
        }

        // === PROC EFFECTS ===

        // Ignite
        if (hasTalentEffect(player, Talent.TalentEffectType.IGNITE)) {
            processIgnite(player, target, baseDamage);
        }

        // Frost Lord - guaranteed freeze
        if (hasTalentEffect(player, Talent.TalentEffectType.FROST_LORD)) {
            processFrostLord(player, target);
        }
        // Frost Bite - normal freeze chance
        else if (hasTalentEffect(player, Talent.TalentEffectType.FROST_BITE)) {
            processFrostBite(player, target);
        }

        // Chain Lightning
        if (hasTalentEffect(player, Talent.TalentEffectType.CHAIN_LIGHTNING)) {
            processChainLightning(player, target, baseDamage);
        }

        // Void Bolt
        if (hasTalentEffect(player, Talent.TalentEffectType.VOID_BOLT)) {
            processVoidBolt(player, target, baseDamage);
        }

        // Firestorm
        if (hasTalentEffect(player, Talent.TalentEffectType.FIRESTORM)) {
            processFirestorm(player, target, baseDamage);
        }

        // Soul Reservoir activation (crouch + attack)
        if (hasTalentEffect(player, Talent.TalentEffectType.SOUL_RESERVOIR)) {
            if (player.isSneaking() && getSoulOrbs(player) > 0) {
                processSoulReservoir(player, target, baseDamage);
            }
        }

        // Void Master - detonate rifts by shooting at them
        if (hasTalentEffect(player, Talent.TalentEffectType.VOID_MASTER)) {
            checkRiftDetonate(player, target.getLocation(), baseDamage);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isOccultiste(player)) return;

        // Corrupted Dimension - 25% dodge chance
        if (hasTalentEffect(player, Talent.TalentEffectType.CORRUPTED_DIMENSION)) {
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.CORRUPTED_DIMENSION);
            if (Math.random() < talent.getValue(0)) {
                event.setCancelled(true);
                // Visual
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
                return;
            }
        }

        // Soul Legion - DR per orb
        if (hasTalentEffect(player, Talent.TalentEffectType.SOUL_LEGION)) {
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_LEGION);
            int orbs = getSoulOrbs(player);
            double drPerOrb = talent.getValue(1);
            double totalDR = Math.min(0.50, drPerOrb * orbs);
            if (totalDR > 0) {
                event.setDamage(event.getDamage() * (1 - totalDR));
            }
        }

        // Fire Avatar - fire immunity
        if (hasTalentEffect(player, Talent.TalentEffectType.FIRE_AVATAR)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    // ==================== KILL EVENTS ====================

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!isOccultiste(killer)) return;

        LivingEntity victim = event.getEntity();

        // Soul Siphon - heal + orb
        if (hasTalentEffect(killer, Talent.TalentEffectType.SOUL_SIPHON)) {
            processSoulSiphon(killer);
        }

        // Phoenix - fire kill explosion
        if (hasTalentEffect(killer, Talent.TalentEffectType.PHOENIX_FLAME)) {
            if (isBurning(victim)) {
                processPhoenixExplosion(killer, victim);
            }
        }

        // Frozen Heart - shatter on frozen kill
        if (hasTalentEffect(killer, Talent.TalentEffectType.FROZEN_HEART)) {
            if (isFrozen(victim)) {
                processShatter(killer, victim);
            }
        }

        // Ice Age - frost zone on frozen kill
        if (hasTalentEffect(killer, Talent.TalentEffectType.ICE_AGE)) {
            if (isFrozen(victim)) {
                createIceZone(victim.getLocation());
            }
        }

        // Lord of the Dead - raise dead
        if (hasTalentEffect(killer, Talent.TalentEffectType.LORD_OF_THE_DEAD)) {
            processRaiseDead(killer, victim);
        }

        // Clean up tracking
        burningEnemies.remove(victim.getUniqueId());
        frozenEnemies.remove(victim.getUniqueId());
    }

    // ==================== SNEAK EVENTS ====================

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!isOccultiste(player)) return;

        if (event.isSneaking()) {
            // Time Stasis activation (crouch + jump)
            if (hasTalentEffect(player, Talent.TalentEffectType.TIME_STASIS)) {
                // Check if jumping (velocity Y > 0)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isSneaking() && player.getVelocity().getY() > 0.3) {
                        processTimeStasis(player);
                    }
                }, 5L);
            }

            // Erasure charging start (crouch + attack handled in damage event)
            if (hasTalentEffect(player, Talent.TalentEffectType.ERASURE)) {
                erasureCharging.put(player.getUniqueId(), System.currentTimeMillis());
            }

            // Necromancer summon (crouch to summon)
            if (hasTalentEffect(player, Talent.TalentEffectType.NECROMANCER)) {
                processNecromancerSummon(player);
            }
        } else {
            // Check if erasure was fully charged
            if (hasTalentEffect(player, Talent.TalentEffectType.ERASURE)) {
                Long chargeStart = erasureCharging.remove(player.getUniqueId());
                if (chargeStart != null) {
                    Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ERASURE);
                    long chargeTime = (long) talent.getValue(1);
                    if (System.currentTimeMillis() - chargeStart >= chargeTime) {
                        processErasure(player);
                    }
                }
            }
        }
    }

    // ==================== EFFECT PROCESSORS ====================

    private void processIgnite(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.IGNITE);
        if (!checkCooldown(player, "ignite", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            // Set on fire
            int duration = (int) (talent.getValue(2) * 20);
            target.setFireTicks(duration);
            burningEnemies.put(target.getUniqueId(), System.currentTimeMillis() + (long)(talent.getValue(2) * 1000));

            // Visual
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.2f);
        }
    }

    private void processFrostBite(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROST_BITE);
        if (!checkCooldown(player, "frost_bite", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            applyFreeze(target, (int)(talent.getValue(2) * 1000), talent.getValue(1));
        }
    }

    private void processFrostLord(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROST_LORD);
        applyFreeze(target, (int)(talent.getValue(1) * 1000), 0.99); // Max slow
    }

    private void applyFreeze(LivingEntity target, int durationMs, double slowStrength) {
        frozenEnemies.put(target.getUniqueId(), System.currentTimeMillis());

        int amplifier = (int)((slowStrength) * 4); // 0-4 amplifier
        int ticks = durationMs / 50;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier, false, true, true));

        // Visual
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);

        // Schedule remove from frozen tracking
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                frozenEnemies.remove(target.getUniqueId());
            }
        }, ticks + 5);
    }

    private void processChainLightning(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.CHAIN_LIGHTNING);
        if (!checkCooldown(player, "chain_lightning", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            int targets = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double range = talent.getValue(3);

            // Check for Thunder God (unlimited targets)
            if (hasTalentEffect(player, Talent.TalentEffectType.THUNDER_GOD)) {
                targets = 100; // Effectively unlimited
            }

            // Check for Mjolnir (triple strike)
            int strikes = 1;
            if (hasTalentEffect(player, Talent.TalentEffectType.MJOLNIR)) {
                Talent mjolnir = getTalentWithEffect(player, Talent.TalentEffectType.MJOLNIR);
                strikes = (int) mjolnir.getValue(0);
                damagePercent = mjolnir.getValue(1);
            }

            // Check for Overcharge (crit bonus targets)
            boolean canCrit = hasTalentEffect(player, Talent.TalentEffectType.OVERCHARGE);

            // Get nearby enemies
            List<LivingEntity> nearbyEnemies = new ArrayList<>();
            for (Entity entity : target.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player) && !entity.isDead()) {
                    nearbyEnemies.add(le);
                    if (nearbyEnemies.size() >= targets) break;
                }
            }

            // Chain to enemies
            Location lastLoc = target.getLocation().add(0, 1, 0);
            double lightningDamage = baseDamage * damagePercent;

            // First strike on primary target
            for (int s = 0; s < strikes; s++) {
                target.damage(lightningDamage, player);
            }
            spawnLightningVisual(player.getLocation().add(0, 1, 0), lastLoc);

            // Conductor - heal from lightning damage
            if (hasTalentEffect(player, Talent.TalentEffectType.CONDUCTOR)) {
                Talent conductor = getTalentWithEffect(player, Talent.TalentEffectType.CONDUCTOR);
                double heal = lightningDamage * strikes * conductor.getValue(0);
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            }

            for (LivingEntity enemy : nearbyEnemies) {
                // Check crit for overcharge
                int bonusTargets = 0;
                if (canCrit && Math.random() < 0.25) { // 25% base crit chance
                    Talent overcharge = getTalentWithEffect(player, Talent.TalentEffectType.OVERCHARGE);
                    bonusTargets = (int) overcharge.getValue(0);
                    targets += bonusTargets;
                }

                for (int s = 0; s < strikes; s++) {
                    enemy.damage(lightningDamage, player);
                }
                spawnLightningVisual(lastLoc, enemy.getLocation().add(0, 1, 0));
                lastLoc = enemy.getLocation().add(0, 1, 0);

                // Conductor heal
                if (hasTalentEffect(player, Talent.TalentEffectType.CONDUCTOR)) {
                    Talent conductor = getTalentWithEffect(player, Talent.TalentEffectType.CONDUCTOR);
                    double heal = lightningDamage * strikes * conductor.getValue(0);
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                }
            }

            // Sound
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);
        }
    }

    private void spawnLightningVisual(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            // Add some randomness for electric effect
            point.add((Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2);
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
        }
    }

    private void processVoidBolt(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VOID_BOLT);
        if (!checkCooldown(player, "void_bolt", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            double damage = baseDamage * talent.getValue(1);

            // Void Instability - explosion
            if (hasTalentEffect(player, Talent.TalentEffectType.VOID_INSTABILITY)) {
                Talent instability = getTalentWithEffect(player, Talent.TalentEffectType.VOID_INSTABILITY);
                double radius = instability.getValue(0);
                double aoeDamage = baseDamage * instability.getValue(1);

                for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        le.damage(aoeDamage, player);
                    }
                }

                // Explosion visual
                target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target.getLocation(), 50, radius/2, radius/2, radius/2, 0.05);
            }

            // Dimensional Rift - leave a rift
            if (hasTalentEffect(player, Talent.TalentEffectType.DIMENSIONAL_RIFT)) {
                createRift(player, target.getLocation());
            }

            // Deal void bolt damage
            target.damage(damage, player);

            // Visual
            target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.5);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }
    }

    private void createRift(Player player, Location location) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DIMENSIONAL_RIFT);
        long duration = (long) talent.getValue(0);
        long tickInterval = (long) talent.getValue(1);
        double damagePercent = talent.getValue(2);

        UUID riftId = UUID.randomUUID();
        RiftData rift = new RiftData(player.getUniqueId(), location, System.currentTimeMillis() + duration,
            tickInterval, damagePercent, System.currentTimeMillis());
        activeRifts.put(riftId, rift);

        // Visual spawn
        location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, location, 30, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
    }

    private void processFirestorm(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FIRESTORM);
        if (!checkCooldown(player, "firestorm", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            int meteors = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double zone = talent.getValue(3);

            Location center = target.getLocation();

            for (int i = 0; i < meteors; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Random position in zone
                    double offsetX = (Math.random() - 0.5) * zone;
                    double offsetZ = (Math.random() - 0.5) * zone;
                    Location impactLoc = center.clone().add(offsetX, 0, offsetZ);

                    // Damage nearby
                    double damage = baseDamage * damagePercent;
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                            le.damage(damage, player);
                            le.setFireTicks(40);
                        }
                    }

                    // Visual
                    impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 30, 1, 0.5, 1, 0.1);
                    impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);
                }, index * 5L);
            }
        }
    }

    private void processSoulSiphon(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_SIPHON);

        // Heal
        double healPercent = talent.getValue(0);
        double heal = player.getMaxHealth() * healPercent;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));

        // Add soul orb
        int maxOrbs = (int) talent.getValue(1);
        // Check Soul Legion for increased max
        if (hasTalentEffect(player, Talent.TalentEffectType.SOUL_LEGION)) {
            Talent legion = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_LEGION);
            maxOrbs = (int) legion.getValue(0);
        }

        int currentOrbs = getSoulOrbs(player);
        if (currentOrbs < maxOrbs) {
            soulOrbs.put(player.getUniqueId(), currentOrbs + 1);
        }

        // Visual
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 0.5f, 1.2f);
    }

    private void processSoulReservoir(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_RESERVOIR);
        if (!checkCooldown(player, "soul_reservoir", 1000)) return;

        int orbs = getSoulOrbs(player);
        if (orbs <= 0) return;

        double damagePerOrb = baseDamage * talent.getValue(0);
        double totalDamage = damagePerOrb * orbs;

        // AoE damage
        for (Entity entity : target.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.damage(totalDamage, player);
            }
        }
        target.damage(totalDamage, player);

        // Consume all orbs
        soulOrbs.put(player.getUniqueId(), 0);

        // Visual
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 50, 2, 1, 2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f);
    }

    private void processPhoenixExplosion(Player player, LivingEntity victim) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PHOENIX_FLAME);

        if (Math.random() < talent.getValue(0)) {
            double damage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue() * talent.getValue(1);
            double radius = talent.getValue(2);

            for (Entity entity : victim.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(60);
                }
            }

            // Visual
            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 2, 0, 0, 0, 0);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation(), 100, radius/2, radius/2, radius/2, 0.2);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.8f);
        }
    }

    private void processShatter(Player player, LivingEntity victim) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROZEN_HEART);
        double radius = talent.getValue(1);
        double damage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue() * 0.5;

        for (Entity entity : victim.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.damage(damage, player);
                applyFreeze(le, 1500, 0.5);
            }
        }

        // Visual
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation(), 50, radius/2, radius/2, radius/2, 0.1);
        victim.getWorld().spawnParticle(Particle.ITEM_CRACK, victim.getLocation(), 30, radius/2, radius/2, radius/2, 0.1,
            new org.bukkit.inventory.ItemStack(Material.ICE));
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
    }

    private void processRaiseDead(Player player, LivingEntity victim) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.LORD_OF_THE_DEAD);

        if (Math.random() < talent.getValue(0)) {
            List<UUID> minions = playerMinions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
            int maxMinions = (int) talent.getValue(3);

            if (minions.size() >= maxMinions) return;

            // Spawn zombie minion
            Zombie minion = victim.getWorld().spawn(victim.getLocation(), Zombie.class);
            minion.setCustomName("§8[Serviteur de " + player.getName() + "]");
            minion.setCustomNameVisible(true);
            minion.setBaby(false);
            minion.getScoreboardTags().add("player_minion");
            minion.getScoreboardTags().add("owner_" + player.getUniqueId());

            // Set stats
            double statPercent = talent.getValue(1);
            // Check Immortal Army buff
            if (hasTalentEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY)) {
                Talent immortal = getTalentWithEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY);
                statPercent += immortal.getValue(1);
            }
            minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20 * statPercent);
            minion.setHealth(minion.getMaxHealth());

            minions.add(minion.getUniqueId());

            // Schedule despawn
            long duration = (long) talent.getValue(2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (minion.isValid() && !minion.isDead()) {
                    minion.remove();
                    minions.remove(minion.getUniqueId());
                }
            }, duration / 50);

            // Visual
            victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation(), 20, 0.5, 1, 0.5, 0.05);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
        }
    }

    private void processTimeStasis(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.TIME_STASIS);
        if (!checkCooldown(player, "time_stasis", (long) talent.getValue(0))) return;

        long duration = (long) talent.getValue(1);
        timeStasisActive.put(player.getUniqueId(), System.currentTimeMillis() + duration);

        // Freeze all nearby enemies
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(duration / 50), 255, false, false, false));
                le.setAI(false);

                // Schedule AI restore
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (le.isValid() && !le.isDead()) {
                        le.setAI(true);
                    }
                }, duration / 50);
            }
        }

        // Visual & sound
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 15, 10, 15, 0.01);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);

        player.sendMessage("§b§l+ STASE TEMPORELLE +");
        player.sendMessage("§7Le temps est fige pendant §b5s§7!");
    }

    private void processNecromancerSummon(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.NECROMANCER);
        if (!checkCooldown(player, "necromancer", 2000)) return;

        int orbs = getSoulOrbs(player);
        if (orbs <= 0) return;

        List<UUID> minions = playerMinions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        int maxMinions = (int) talent.getValue(2);

        if (minions.size() >= maxMinions) return;

        // Consume one orb, summon one skeleton
        soulOrbs.put(player.getUniqueId(), orbs - 1);

        Skeleton minion = player.getWorld().spawn(player.getLocation().add(
            Math.random() * 2 - 1, 0, Math.random() * 2 - 1), Skeleton.class);
        minion.setCustomName("§8[Squelette de " + player.getName() + "]");
        minion.setCustomNameVisible(true);
        minion.getScoreboardTags().add("player_minion");
        minion.getScoreboardTags().add("owner_" + player.getUniqueId());

        double statPercent = talent.getValue(0);
        minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20 * statPercent);
        minion.setHealth(minion.getMaxHealth());

        minions.add(minion.getUniqueId());

        // Schedule despawn
        long duration = (long) talent.getValue(1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (minion.isValid() && !minion.isDead()) {
                minion.remove();
                minions.remove(minion.getUniqueId());
            }
        }, duration / 50);

        // Visual
        player.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 15, 0.3, 1, 0.3, 0.05);
        player.getWorld().playSound(minion.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 1.0f);
    }

    private void processErasure(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ERASURE);
        if (!checkCooldown(player, "erasure", (long) talent.getValue(0))) return;

        long duration = (long) talent.getValue(2);
        double radius = talent.getValue(3);
        double bossDamagePercent = talent.getValue(4);

        Location center = player.getLocation();

        player.sendMessage("§5§l+ EFFACEMENT +");
        player.sendMessage("§7Zone de neant activee!");

        // Create erasure zone effect
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int)(duration / 50);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Visual
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 0.5, z), 2, 0, 0, 0, 0);
                }
                center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 20, radius/2, 1, radius/2, 0.1);

                // Damage/kill enemies
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        // Check if boss
                        boolean isBoss = le.getScoreboardTags().contains("boss") ||
                            le.getMaxHealth() > 100;

                        if (isBoss) {
                            double damage = le.getMaxHealth() * bossDamagePercent / (maxTicks / 20.0);
                            le.damage(damage, player);
                        } else {
                            le.setHealth(0);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Sound
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
    }

    private void checkRiftDetonate(Player player, Location targetLoc, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VOID_MASTER);
        double detonateRadius = talent.getValue(1);
        double damageMultiplier = talent.getValue(0);

        Iterator<Map.Entry<UUID, RiftData>> iterator = activeRifts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RiftData> entry = iterator.next();
            RiftData rift = entry.getValue();

            if (!rift.ownerUuid.equals(player.getUniqueId())) continue;

            if (rift.location.distance(targetLoc) < 3) {
                // Detonate!
                double damage = baseDamage * damageMultiplier;

                for (Entity entity : rift.location.getWorld().getNearbyEntities(rift.location, detonateRadius, detonateRadius, detonateRadius)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        le.damage(damage, player);
                    }
                }

                // Visual
                rift.location.getWorld().spawnParticle(Particle.EXPLOSION, rift.location, 3, 0, 0, 0, 0);
                rift.location.getWorld().spawnParticle(Particle.DRAGON_BREATH, rift.location, 100, detonateRadius/2, detonateRadius/2, detonateRadius/2, 0.1);
                rift.location.getWorld().playSound(rift.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                iterator.remove();
                break;
            }
        }
    }

    // ==================== PERIODIC PROCESSORS ====================

    private void processFireSpread() {
        if (!hasTalentEffectForAnyPlayer(Talent.TalentEffectType.FIRE_SPREAD)) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = burningEnemies.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now > entry.getValue()) {
                iterator.remove();
                continue;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead()) {
                iterator.remove();
                continue;
            }

            // Spread fire to nearby enemies
            for (Entity nearby : entity.getNearbyEntities(2, 2, 2)) {
                if (nearby instanceof LivingEntity le && !(nearby instanceof Player)) {
                    if (!burningEnemies.containsKey(nearby.getUniqueId())) {
                        le.setFireTicks(60);
                        burningEnemies.put(nearby.getUniqueId(), now + 3000);
                    }
                }
            }
        }
    }

    private void processLightningStorm() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.LIGHTNING_STORM)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.LIGHTNING_STORM);
            int targets = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double range = talent.getValue(3);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            List<LivingEntity> nearbyEnemies = new ArrayList<>();
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player) && !entity.isDead()) {
                    nearbyEnemies.add(le);
                    if (nearbyEnemies.size() >= targets) break;
                }
            }

            for (LivingEntity enemy : nearbyEnemies) {
                enemy.damage(damage, player);
                spawnLightningVisual(player.getLocation().add(0, 1.5, 0), enemy.getLocation().add(0, 1, 0));
            }

            if (!nearbyEnemies.isEmpty()) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.5f);
            }
        }
    }

    private void processPerpetualStorm() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.PERPETUAL_STORM)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PERPETUAL_STORM);
            double radius = talent.getValue(1);
            int targets = (int) talent.getValue(2);
            double damagePercent = talent.getValue(3);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            List<LivingEntity> nearbyEnemies = new ArrayList<>();
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player) && !entity.isDead()) {
                    nearbyEnemies.add(le);
                }
            }

            // Random targets
            Collections.shuffle(nearbyEnemies);
            int count = 0;
            for (LivingEntity enemy : nearbyEnemies) {
                if (count >= targets) break;
                enemy.damage(damage, player);
                spawnLightningVisual(player.getLocation().add(0, 2, 0), enemy.getLocation().add(0, 1, 0));
                count++;
            }

            // Storm visual around player
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 2.5, 0), 5, radius/2, 0.5, radius/2, 0);
        }
    }

    private void processFireAvatar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.FIRE_AVATAR)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FIRE_AVATAR);
            double radius = talent.getValue(0);
            double damagePercent = talent.getValue(1);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(20);
                }
            }

            // Visual aura
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(x, 0.5, z), 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    private void processInferno() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.INFERNO)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.INFERNO);
            if (!checkCooldown(player, "inferno", (long) talent.getValue(0))) continue;

            double damagePercent = talent.getValue(1);
            double radius = talent.getValue(2);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(100);
                    burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + 5000);
                }
            }

            // Nova visual
            for (double r = 0; r <= radius; r += 1) {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(x, 0.3, z), 1, 0, 0, 0, 0.05);
                }
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
            player.sendMessage("§6§l+ INFERNO +");
        }
    }

    private void processBlackSun() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = blackSunActive.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now > entry.getValue()) {
                iterator.remove();
                continue;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Find talent for damage values
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_SUN);
            if (talent == null) continue;

            double damagePercent = talent.getValue(2);
            double radius = talent.getValue(3);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(20);
                }
            }

            // Sun visual above player
            Location sunLoc = player.getLocation().add(0, 5, 0);
            player.getWorld().spawnParticle(Particle.FLAME, sunLoc, 30, 1, 1, 1, 0.05);
            player.getWorld().spawnParticle(Particle.LAVA, sunLoc, 5, 1, 1, 1, 0);
        }

        // Check if players need to activate Black Sun
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.BLACK_SUN)) continue;
            if (blackSunActive.containsKey(player.getUniqueId())) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_SUN);
            if (!checkCooldown(player, "black_sun", (long) talent.getValue(0))) continue;

            // Activate black sun
            long duration = (long) talent.getValue(1);
            blackSunActive.put(player.getUniqueId(), System.currentTimeMillis() + duration);

            player.sendMessage("§c§l+ SOLEIL NOIR +");
            player.sendMessage("§7Un soleil ardent brule vos ennemis!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }
    }

    private void processEternalWinter() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_WINTER)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_WINTER);
            double radius = talent.getValue(0);
            double slowPercent = talent.getValue(1);

            int amplifier = (int)(slowPercent * 5); // 70% = amplifier 3-4

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, amplifier, false, true, true));
                }
            }

            // Winter visual
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 10, radius/2, 1, radius/2, 0.01);
        }
    }

    private void processDivineJudgment() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT);
            if (!checkCooldown(player, "divine_judgment", (long) talent.getValue(0))) continue;

            double damagePercent = talent.getValue(1);
            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            // Strike ALL nearby enemies
            int struck = 0;
            for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    // Lightning visual
                    le.getWorld().strikeLightningEffect(le.getLocation());
                    struck++;
                }
            }

            if (struck > 0) {
                player.sendMessage("§e§l+ JUGEMENT DIVIN +");
                player.sendMessage("§7" + struck + " ennemis frappes par la foudre divine!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
            }
        }
    }

    private void processMeteorRain() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.METEOR_RAIN)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.METEOR_RAIN);
            if (!checkCooldown(player, "meteor_rain", (long) talent.getValue(0))) continue;

            int meteors = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double zone = talent.getValue(3);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            Location center = player.getLocation();

            player.sendMessage("§4§l+ PLUIE DE METEORES +");
            player.sendMessage("§7L'apocalypse de feu s'abat!");

            for (int i = 0; i < meteors; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    double offsetX = (Math.random() - 0.5) * zone;
                    double offsetZ = (Math.random() - 0.5) * zone;
                    Location impactLoc = center.clone().add(offsetX, 0, offsetZ);

                    // Damage
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, 3, 3, 3)) {
                        if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                            le.damage(damage, player);
                            le.setFireTicks(100);
                        }
                    }

                    // Visual
                    impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 2, 0, 0, 0, 0);
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 50, 2, 1, 2, 0.2);
                    impactLoc.getWorld().spawnParticle(Particle.LAVA, impactLoc, 10, 1, 0.5, 1, 0);
                    impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                }, index * 3L);
            }
        }
    }

    private void processRifts() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, RiftData>> iterator = activeRifts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, RiftData> entry = iterator.next();
            RiftData rift = entry.getValue();

            if (now > rift.expiry) {
                iterator.remove();
                continue;
            }

            Player owner = Bukkit.getPlayer(rift.ownerUuid);
            if (owner == null) {
                iterator.remove();
                continue;
            }

            // Check tick timing
            if (now - rift.lastTick < rift.tickInterval) continue;
            rift.lastTick = now;

            double baseDamage = owner.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * rift.damagePercent;

            // Find closest enemy and damage it
            LivingEntity closest = null;
            double closestDist = Double.MAX_VALUE;
            for (Entity entity : rift.location.getWorld().getNearbyEntities(rift.location, 5, 5, 5)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    double dist = le.getLocation().distance(rift.location);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = le;
                    }
                }
            }

            if (closest != null) {
                closest.damage(damage, owner);
                spawnLightningVisual(rift.location.add(0, 0.5, 0), closest.getLocation().add(0, 1, 0));
                rift.location.add(0, -0.5, 0); // Reset location
            }

            // Void Anchor - pull enemies
            if (hasTalentEffect(owner, Talent.TalentEffectType.VOID_ANCHOR)) {
                Talent anchor = getTalentWithEffect(owner, Talent.TalentEffectType.VOID_ANCHOR);
                double pullStrength = anchor.getValue(0);

                for (Entity entity : rift.location.getWorld().getNearbyEntities(rift.location, 5, 5, 5)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        Vector direction = rift.location.toVector().subtract(entity.getLocation().toVector()).normalize();
                        entity.setVelocity(entity.getVelocity().add(direction.multiply(pullStrength * 0.3)));
                    }
                }
            }

            // Rift visual
            rift.location.getWorld().spawnParticle(Particle.PORTAL, rift.location, 10, 0.3, 0.3, 0.3, 0.1);
        }

        // Check for Black Hole formation
        checkBlackHoleFormation();
    }

    private void checkBlackHoleFormation() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.BLACK_HOLE)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_HOLE);
            int riftsNeeded = (int) talent.getValue(0);

            // Find rifts owned by this player
            List<UUID> playerRifts = new ArrayList<>();
            for (Map.Entry<UUID, RiftData> entry : activeRifts.entrySet()) {
                if (entry.getValue().ownerUuid.equals(player.getUniqueId())) {
                    playerRifts.add(entry.getKey());
                }
            }

            if (playerRifts.size() < riftsNeeded) continue;

            // Check if rifts are close together
            for (int i = 0; i < playerRifts.size() - riftsNeeded + 1; i++) {
                RiftData rift1 = activeRifts.get(playerRifts.get(i));
                List<UUID> closeRifts = new ArrayList<>();
                closeRifts.add(playerRifts.get(i));

                for (int j = i + 1; j < playerRifts.size(); j++) {
                    RiftData rift2 = activeRifts.get(playerRifts.get(j));
                    if (rift1.location.distance(rift2.location) < 5) {
                        closeRifts.add(playerRifts.get(j));
                    }
                }

                if (closeRifts.size() >= riftsNeeded) {
                    // Form black hole!
                    createBlackHole(player, rift1.location, closeRifts);
                    break;
                }
            }
        }
    }

    private void createBlackHole(Player player, Location center, List<UUID> riftsToConsume) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_HOLE);
        double damageMultiplier = talent.getValue(1);
        long duration = (long) talent.getValue(2);
        double radius = talent.getValue(3);

        // Remove consumed rifts
        for (UUID riftId : riftsToConsume) {
            activeRifts.remove(riftId);
        }

        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double totalDamage = baseDamage * damageMultiplier;
        double damagePerTick = totalDamage / (duration / 250.0);

        player.sendMessage("§0§l+ TROU NOIR +");
        player.sendMessage("§7La realite s'effondre!");

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int)(duration / 250);

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Pull and damage all enemies
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius * 2, radius * 2, radius * 2)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        // Strong pull
                        Vector direction = center.toVector().subtract(entity.getLocation().toVector()).normalize();
                        double distance = entity.getLocation().distance(center);
                        double pullForce = Math.min(1.0, radius / distance);
                        entity.setVelocity(direction.multiply(pullForce));

                        // Damage if in range
                        if (distance < radius) {
                            le.damage(damagePerTick, player);
                        }
                    }
                }

                // Visual
                center.getWorld().spawnParticle(Particle.PORTAL, center, 50, radius/2, radius/2, radius/2, 0.5);
                center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 30, 0.5, 0.5, 0.5, 0.1);

                if (ticks % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void processSoulRegen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_HARVEST)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_HARVEST);
            double regenPerOrb = talent.getValue(0);
            int orbs = getSoulOrbs(player);

            if (orbs > 0) {
                double totalRegen = player.getMaxHealth() * regenPerOrb * orbs;
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + totalRegen));
            }
        }
    }

    private void processBlizzardAura() {
        if (!hasTalentEffectForAnyPlayer(Talent.TalentEffectType.BLIZZARD)) return;

        for (Map.Entry<UUID, Long> entry : frozenEnemies.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead()) continue;
            if (!(entity instanceof LivingEntity frozen)) continue;

            // Check if any nearby player has Blizzard
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isOccultiste(player)) continue;
                if (!hasTalentEffect(player, Talent.TalentEffectType.BLIZZARD)) continue;

                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLIZZARD);
                double auraRadius = talent.getValue(0);

                // Create freeze aura around frozen enemies
                for (Entity nearby : frozen.getNearbyEntities(auraRadius, auraRadius, auraRadius)) {
                    if (nearby instanceof LivingEntity le && !(nearby instanceof Player)) {
                        if (!frozenEnemies.containsKey(nearby.getUniqueId())) {
                            applyFreeze(le, 1000, 0.5);
                        }
                    }
                }

                // Blizzard visual
                frozen.getWorld().spawnParticle(Particle.SNOWFLAKE, frozen.getLocation().add(0, 1, 0), 5, auraRadius/2, 0.5, auraRadius/2, 0.01);
            }
        }
    }

    private void processAbsoluteZero() {
        if (!hasTalentEffectForAnyPlayer(Talent.TalentEffectType.ABSOLUTE_ZERO)) return;

        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isOccultiste(player)) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO);
            long freezeTime = (long) talent.getValue(0);
            double bossDamageMultiplier = talent.getValue(1);

            for (Map.Entry<UUID, Long> entry : frozenEnemies.entrySet()) {
                if (now - entry.getValue() >= freezeTime) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity == null || entity.isDead()) continue;
                    if (!(entity instanceof LivingEntity le)) continue;

                    // Check if boss
                    boolean isBoss = le.getScoreboardTags().contains("boss") || le.getMaxHealth() > 100;

                    if (isBoss) {
                        double damage = le.getMaxHealth() * (bossDamageMultiplier / 10.0); // 500% over time
                        le.damage(damage, player);
                    } else {
                        le.setHealth(0);
                    }

                    // Visual
                    le.getWorld().spawnParticle(Particle.ITEM_CRACK, le.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1,
                        new org.bukkit.inventory.ItemStack(Material.ICE));
                    le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);

                    // Reset freeze time so it doesn't trigger again immediately
                    entry.setValue(now);
                }
            }
        }
    }

    private void processIceZones() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = iceZones.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now > entry.getValue()) {
                iterator.remove();
                continue;
            }

            // Parse location from key
            String[] parts = entry.getKey().split(",");
            if (parts.length != 4) continue;

            try {
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;

                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                Location loc = new Location(world, x, y, z);

                // Freeze enemies in zone
                for (Entity entity : world.getNearbyEntities(loc, 2, 2, 2)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        applyFreeze(le, 1000, 0.5);
                    }
                }

                // Visual
                world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 1, 0.2, 1, 0.01);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void createIceZone(Location location) {
        Talent talent = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTalentEffect(player, Talent.TalentEffectType.ICE_AGE)) {
                talent = getTalentWithEffect(player, Talent.TalentEffectType.ICE_AGE);
                break;
            }
        }
        if (talent == null) return;

        long duration = (long) talent.getValue(0);
        String key = location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
        iceZones.put(key, System.currentTimeMillis() + duration);

        // Visual spawn
        location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 30, 1, 0.5, 1, 0.05);
        location.getWorld().playSound(location, Sound.BLOCK_SNOW_PLACE, 1.0f, 0.5f);
    }

    private void cleanupExpiredData() {
        long now = System.currentTimeMillis();

        // Clean cooldowns
        internalCooldowns.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cd -> now > cd.getValue());
            return entry.getValue().isEmpty();
        });

        // Clean burning enemies
        burningEnemies.entrySet().removeIf(entry -> now > entry.getValue() || Bukkit.getEntity(entry.getKey()) == null);

        // Clean frozen enemies
        frozenEnemies.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null) return true;
            if (entity instanceof LivingEntity le) {
                return !le.hasPotionEffect(PotionEffectType.SLOWNESS);
            }
            return true;
        });

        // Clean rifts
        activeRifts.entrySet().removeIf(entry -> now > entry.getValue().expiry);

        // Clean minions
        for (Map.Entry<UUID, List<UUID>> entry : playerMinions.entrySet()) {
            entry.getValue().removeIf(minionId -> {
                Entity minion = Bukkit.getEntity(minionId);
                return minion == null || minion.isDead();
            });
        }

        // Clean time stasis
        timeStasisActive.entrySet().removeIf(entry -> now > entry.getValue());

        // Clean black sun
        blackSunActive.entrySet().removeIf(entry -> now > entry.getValue());

        // Clean erasure charging
        erasureCharging.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            return player == null || !player.isSneaking();
        });

        // Clean ice zones
        iceZones.entrySet().removeIf(entry -> now > entry.getValue());
    }

    // ==================== UTILITY METHODS ====================

    private boolean isOccultiste(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data.hasClass() && data.getSelectedClass() == ClassType.OCCULTISTE;
    }

    private boolean hasTalentEffect(Player player, Talent.TalentEffectType effectType) {
        return talentManager.hasTalentEffect(player, effectType);
    }

    private Talent getTalentWithEffect(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentWithEffect(player, effectType);
    }

    private boolean hasTalentEffectForAnyPlayer(Talent.TalentEffectType effectType) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOccultiste(player) && hasTalentEffect(player, effectType)) {
                return true;
            }
        }
        return false;
    }

    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    private LivingEntity getTarget(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity le && !(event.getEntity() instanceof Player)) {
            return le;
        }
        return null;
    }

    private boolean checkCooldown(Player player, String ability, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = internalCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        long now = System.currentTimeMillis();
        Long lastUse = playerCooldowns.get(ability);

        if (lastUse != null && now - lastUse < cooldownMs) {
            return false;
        }

        playerCooldowns.put(ability, now);
        return true;
    }

    private int getSoulOrbs(Player player) {
        return soulOrbs.getOrDefault(player.getUniqueId(), 0);
    }

    private boolean isBurning(LivingEntity entity) {
        return entity.getFireTicks() > 0 || burningEnemies.containsKey(entity.getUniqueId());
    }

    private boolean isFrozen(LivingEntity entity) {
        return frozenEnemies.containsKey(entity.getUniqueId()) ||
               entity.hasPotionEffect(PotionEffectType.SLOWNESS);
    }

    // ==================== DATA CLASSES ====================

    private static class RiftData {
        final UUID ownerUuid;
        final Location location;
        final long expiry;
        final long tickInterval;
        final double damagePercent;
        long lastTick;

        RiftData(UUID ownerUuid, Location location, long expiry, long tickInterval, double damagePercent, long lastTick) {
            this.ownerUuid = ownerUuid;
            this.location = location;
            this.expiry = expiry;
            this.tickInterval = tickInterval;
            this.damagePercent = damagePercent;
            this.lastTick = lastTick;
        }
    }
}
