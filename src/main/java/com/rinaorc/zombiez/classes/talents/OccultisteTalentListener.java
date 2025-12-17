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

    // Minions invoques (player UUID -> list of minion UUIDs)
    private final Map<UUID, List<UUID>> playerMinions = new ConcurrentHashMap<>();

    // Cooldowns internes
    private final Map<UUID, Map<String, Long>> internalCooldowns = new ConcurrentHashMap<>();

    // Black Sun actif (player UUID -> end timestamp)
    private final Map<UUID, Long> blackSunActive = new ConcurrentHashMap<>();

    // Time Stasis actif (player UUID -> end timestamp)
    private final Map<UUID, Long> timeStasisActive = new ConcurrentHashMap<>();

    // Ice zones (location hash -> expiry timestamp)
    private final Map<String, Long> iceZones = new ConcurrentHashMap<>();

    // ==================== SHADOW PRIEST VOID TRACKING ====================

    // Insanity par joueur (0-100)
    private final Map<UUID, Double> playerInsanity = new ConcurrentHashMap<>();

    // Voidform actif (player UUID -> end timestamp, 0 = indefinite until insanity depletes)
    private final Map<UUID, Long> voidformActive = new ConcurrentHashMap<>();

    // Shadow DOTs actifs (entity UUID -> ShadowDotData)
    private final Map<UUID, ShadowDotData> shadowDots = new ConcurrentHashMap<>();

    // Vampiric Touch DOTs (entity UUID -> VampiricTouchData)
    private final Map<UUID, VampiricTouchData> vampiricTouchDots = new ConcurrentHashMap<>();

    // Psychic Horror debuff (entity UUID -> expiry timestamp)
    private final Map<UUID, Long> psychicHorrorDebuff = new ConcurrentHashMap<>();

    // Voidling actif (player UUID -> voidling entity UUID)
    private final Map<UUID, UUID> activeVoidlings = new ConcurrentHashMap<>();

    // Dark Ascension buff actif (player UUID -> expiry timestamp)
    private final Map<UUID, Long> darkAscensionBuff = new ConcurrentHashMap<>();

    // Dark Ascension charging (player UUID -> charge start timestamp)
    private final Map<UUID, Long> darkAscensionCharging = new ConcurrentHashMap<>();

    // Derniere attaque du joueur (pour decay d'insanity hors combat)
    private final Map<UUID, Long> lastCombatTime = new ConcurrentHashMap<>();

    public OccultisteTalentListener(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;

        // Demarrer les taches periodiques
        startPeriodicTasks();
    }

    // ==================== TACHES PERIODIQUES OPTIMISEES ====================

    // Cache des joueurs Occultistes actifs pour eviter des iterations inutiles
    private final Set<UUID> activeOccultistes = ConcurrentHashMap.newKeySet();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 2000; // 2 secondes

    private void startPeriodicTasks() {
        // FAST TICK (10L = 0.5s) - Auras haute frequence
        new BukkitRunnable() {
            @Override
            public void run() {
                updateOccultistesCache();
                if (activeOccultistes.isEmpty()) return;

                processPerpetualStorm();
                processEternalWinter();
                processBlizzardAura();
                processIceZones();
                processVoidformTick();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // NORMAL TICK (20L = 1s) - Effets standards
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeOccultistes.isEmpty()) return;

                processFireSpread();
                processFireAvatar();
                processInferno();
                processBlackSun();
                processDivineJudgment();
                processMeteorRain();
                processSoulRegen();
                processAbsoluteZero();
                processShadowDots();
                processVampiricTouchDots();
                processShadowyApparitions();
                processInsanityDecay();
                processPsychicHorror();
                processVoidlingAttacks();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // SLOW TICK (30L = 1.5s) - Effets moins frequents
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeOccultistes.isEmpty()) return;
                processLightningStorm();
            }
        }.runTaskTimer(plugin, 30L, 30L);

        // CLEANUP (200L = 10s) - Nettoyage des donnees expirees
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredData();
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private void updateOccultistesCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_TTL) return;
        lastCacheUpdate = now;

        activeOccultistes.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOccultiste(player)) {
                activeOccultistes.add(player.getUniqueId());
            }
        }
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

        // Shadow Word: Pain (DOT d'ombre)
        if (hasTalentEffect(player, Talent.TalentEffectType.SHADOW_WORD)) {
            processShadowWord(player, target, baseDamage);
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

        // Void Eruption activation (crouch + attack with 50+ insanity)
        if (hasTalentEffect(player, Talent.TalentEffectType.VOID_ERUPTION)) {
            if (player.isSneaking() && getInsanity(player) >= 50) {
                processVoidEruption(player, target, baseDamage);
            }
        }

        // Marquer le temps de combat pour le decay d'insanity
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isOccultiste(player)) return;

        // Soul Legion - DR per orb (capped a 40%)
        if (hasTalentEffect(player, Talent.TalentEffectType.SOUL_LEGION)) {
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SOUL_LEGION);
            int orbs = getSoulOrbs(player);
            double drPerOrb = talent.getValue(1);
            double totalDR = Math.min(0.50, drPerOrb * orbs); // Cap standardise a 50% pour toutes les classes
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

            // Voidling summon (crouch + jump)
            if (hasTalentEffect(player, Talent.TalentEffectType.VOIDLING)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isSneaking() && player.getVelocity().getY() > 0.3) {
                        processVoidlingSummon(player);
                    }
                }, 5L);
            }

            // Dark Ascension charging start (crouch + jump maintained)
            if (hasTalentEffect(player, Talent.TalentEffectType.DARK_ASCENSION)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isSneaking() && player.getVelocity().getY() > 0.3) {
                        darkAscensionCharging.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                }, 5L);
            }

            // Necromancer summon (crouch to summon)
            if (hasTalentEffect(player, Talent.TalentEffectType.NECROMANCER)) {
                processNecromancerSummon(player);
            }
        } else {
            // Check if Dark Ascension was fully charged
            if (hasTalentEffect(player, Talent.TalentEffectType.DARK_ASCENSION)) {
                Long chargeStart = darkAscensionCharging.remove(player.getUniqueId());
                if (chargeStart != null) {
                    Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DARK_ASCENSION);
                    long chargeTime = (long) talent.getValue(1);
                    if (System.currentTimeMillis() - chargeStart >= chargeTime) {
                        processDarkAscension(player);
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

            // Sound (volume reduit pour eviter le spam sonore)
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.8f);
        }
    }

    /**
     * Genere un eclair visuel entre deux points (particules uniquement, pas de vrai eclair)
     * Utilise END_ROD pour le coeur lumineux et ELECTRIC_SPARK pour l'effet electrique
     */
    private void spawnLightningVisual(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance < 0.5) return;

        direction.normalize();
        World world = from.getWorld();

        // Tracer l'eclair avec un leger zigzag
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        double zigzagOffset = 0;

        for (double d = 0; d < distance; d += 0.8) {
            // Zigzag aleatoire pour effet naturel
            zigzagOffset = (Math.random() - 0.5) * 0.3;

            Location point = from.clone().add(direction.clone().multiply(d));
            point.add(perpendicular.clone().multiply(zigzagOffset));

            // Coeur lumineux (moins de particules, plus visible)
            world.spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0);
        }

        // Impact au point d'arrivee (petit burst)
        world.spawnParticle(Particle.ELECTRIC_SPARK, to, 5, 0.15, 0.15, 0.15, 0.02);
    }

    // ==================== SHADOW PRIEST VOID PROCESSORS ====================

    /**
     * Shadow Word: Pain - Applique un DOT d'ombre qui génère de l'Insanity
     */
    private void processShadowWord(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SHADOW_WORD);
        if (!checkCooldown(player, "shadow_word", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            double damagePerSecond = baseDamage * talent.getValue(1);
            double duration = talent.getValue(2) * 1000; // Convert to ms
            double insanityPerTick = talent.getValue(3);

            // Check Dark Ascension buff for extended duration
            if (darkAscensionBuff.containsKey(player.getUniqueId())) {
                Talent darkAsc = getTalentWithEffect(player, Talent.TalentEffectType.DARK_ASCENSION);
                if (darkAsc != null) {
                    duration *= (1 + darkAsc.getValue(4)); // +50% duration
                }
            }

            // Appliquer ou rafraîchir le DOT
            ShadowDotData dotData = new ShadowDotData(
                player.getUniqueId(),
                damagePerSecond,
                System.currentTimeMillis() + (long) duration,
                insanityPerTick,
                System.currentTimeMillis()
            );
            shadowDots.put(target.getUniqueId(), dotData);

            // Si le joueur a Vampiric Touch, appliquer le 2ème DOT
            if (hasTalentEffect(player, Talent.TalentEffectType.VAMPIRIC_TOUCH)) {
                applyVampiricTouch(player, target, baseDamage);
            }

            // Visual
            target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        }
    }

    /**
     * Vampiric Touch - 2ème DOT qui soigne le joueur
     */
    private void applyVampiricTouch(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VAMPIRIC_TOUCH);
        double damagePerSecond = baseDamage * talent.getValue(0);
        double duration = talent.getValue(1) * 1000;
        double healPercent = talent.getValue(2);

        // Check Dark Ascension buff for extended duration
        if (darkAscensionBuff.containsKey(player.getUniqueId())) {
            Talent darkAsc = getTalentWithEffect(player, Talent.TalentEffectType.DARK_ASCENSION);
            if (darkAsc != null) {
                duration *= (1 + darkAsc.getValue(4));
            }
        }

        VampiricTouchData vtData = new VampiricTouchData(
            player.getUniqueId(),
            damagePerSecond,
            System.currentTimeMillis() + (long) duration,
            healPercent,
            System.currentTimeMillis()
        );
        vampiricTouchDots.put(target.getUniqueId(), vtData);

        // Visual
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.5, 0), 5, 0.2, 0.3, 0.2, 0.01);
    }

    /**
     * Void Eruption - Explosion massive et entrée en Voidform
     */
    private void processVoidEruption(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VOID_ERUPTION);
        if (!checkCooldown(player, "void_eruption", 2000)) return;

        double minInsanity = talent.getValue(0);
        double currentInsanity = getInsanity(player);

        if (currentInsanity < minInsanity) return;

        double baseDamagePercent = talent.getValue(1);
        double damagePerInsanity = talent.getValue(2);
        double radius = talent.getValue(3);
        long voidformDuration = (long) talent.getValue(4);

        // Calculer les dégâts totaux
        double totalDamage = baseDamage * (baseDamagePercent + (damagePerInsanity * currentInsanity));

        // Appliquer le bonus d'Insanity si le joueur a le talent
        if (hasTalentEffect(player, Talent.TalentEffectType.INSANITY)) {
            Talent insanityTalent = getTalentWithEffect(player, Talent.TalentEffectType.INSANITY);
            double damageBonus = insanityTalent.getValue(0) * currentInsanity;
            totalDamage *= (1 + damageBonus);
        }

        // Explosion AoE
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.damage(totalDamage, player);
            }
        }
        target.damage(totalDamage, player);

        // Consommer l'Insanity et entrer en Voidform
        playerInsanity.put(player.getUniqueId(), currentInsanity); // Garder l'Insanity pour Voidform
        enterVoidform(player, voidformDuration);

        // Visual spectaculaire
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 1, 1, 1, 0);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 100, radius/2, 2, radius/2, 0.2);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 50, radius/2, 1, radius/2, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);

        sendActionBar(player, "§5§l+ VOID ERUPTION + §7Entree en Voidform!");
    }

    /**
     * Entre le joueur en Voidform
     */
    private void enterVoidform(Player player, long duration) {
        long endTime = duration > 0 ? System.currentTimeMillis() + duration : Long.MAX_VALUE;
        voidformActive.put(player.getUniqueId(), endTime);

        // Visual effect sur le joueur
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.3);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int)(duration / 50), 0, false, false, true));

        player.sendMessage("§5§l+ VOIDFORM ACTIVE +");
        player.sendMessage("§7Degats d'ombre §c+30%§7, DOTs acceleres!");
    }

    /**
     * Dark Ascension - Ultimate void ability
     */
    private void processDarkAscension(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DARK_ASCENSION);
        if (!checkCooldown(player, "dark_ascension", (long) talent.getValue(0))) return;

        double insanitySet = talent.getValue(2);
        long buffDuration = (long) talent.getValue(3);

        // Reset Insanity to max
        playerInsanity.put(player.getUniqueId(), insanitySet);

        // Activer le buff Dark Ascension (DOTs +50% duration)
        darkAscensionBuff.put(player.getUniqueId(), System.currentTimeMillis() + buffDuration);

        // Entrer en Voidform immédiatement (indéfini - dépend de l'Insanity)
        enterVoidform(player, 0);

        // Reset tous les DOTs actifs (rafraîchir leur durée)
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, ShadowDotData> entry : shadowDots.entrySet()) {
            if (entry.getValue().ownerUuid.equals(player.getUniqueId())) {
                ShadowDotData dot = entry.getValue();
                long remainingDuration = dot.expiry - now;
                if (remainingDuration > 0) {
                    // Extend by 50%
                    dot.expiry = now + (long)(remainingDuration * 1.5);
                }
            }
        }
        for (Map.Entry<UUID, VampiricTouchData> entry : vampiricTouchDots.entrySet()) {
            if (entry.getValue().ownerUuid.equals(player.getUniqueId())) {
                VampiricTouchData dot = entry.getValue();
                long remainingDuration = dot.expiry - now;
                if (remainingDuration > 0) {
                    dot.expiry = now + (long)(remainingDuration * 1.5);
                }
            }
        }

        // Visual spectaculaire
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 200, 3, 3, 3, 0.5);
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 100, 2, 2, 2, 0.2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);

        player.sendMessage("§5§l+ ASCENSION SOMBRE +");
        player.sendMessage("§7Insanity maximale! DOTs etendus de §c50%§7!");
    }

    /**
     * Invoque un Voidling (Shadowfiend)
     */
    private void processVoidlingSummon(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VOIDLING);
        if (!checkCooldown(player, "voidling", (long) talent.getValue(0))) return;

        // Vérifier si un voidling est déjà actif
        UUID existingVoidling = activeVoidlings.get(player.getUniqueId());
        if (existingVoidling != null) {
            Entity existing = Bukkit.getEntity(existingVoidling);
            if (existing != null && existing.isValid()) {
                return; // Déjà un voidling actif
            }
        }

        long duration = (long) talent.getValue(1);

        // Spawn le Voidling (utiliser un Endermite pour le look void)
        Location spawnLoc = player.getLocation().add(Math.random() * 2 - 1, 0, Math.random() * 2 - 1);
        Endermite voidling = player.getWorld().spawn(spawnLoc, Endermite.class);
        voidling.setCustomName("§5[Devoreur du Vide de " + player.getName() + "]");
        voidling.setCustomNameVisible(true);
        voidling.getScoreboardTags().add("voidling");
        voidling.getScoreboardTags().add("owner_" + player.getUniqueId());

        // Booster ses stats
        voidling.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(50);
        voidling.setHealth(50);

        activeVoidlings.put(player.getUniqueId(), voidling.getUniqueId());

        // Schedule despawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (voidling.isValid() && !voidling.isDead()) {
                voidling.getWorld().spawnParticle(Particle.PORTAL, voidling.getLocation(), 30, 0.3, 0.5, 0.3, 0.1);
                voidling.remove();
            }
            activeVoidlings.remove(player.getUniqueId());
        }, duration / 50);

        // Visual
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        sendActionBar(player, "§5+ Devoreur du Vide invoque! +");
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
        victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation(), 30, radius/2, radius/2, radius/2, 0.1,
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
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.LIGHTNING_STORM)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.LIGHTNING_STORM);
            int targets = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double range = Math.min(talent.getValue(3), 20.0); // Cap a 20 blocs

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
                // Son leger pour indiquer l'activation
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.2f, 1.8f);
                // Petit effet electrique autour du joueur
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.5, 0), 4, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    private void processPerpetualStorm() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.PERPETUAL_STORM)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PERPETUAL_STORM);
            double radius = Math.min(talent.getValue(1), 15.0); // Cap a 15 blocs
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

            // Storm visual around player - aura electrique
            if (count > 0) {
                // Petit arc electrique autour du joueur
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 3, 0.5, 0.3, 0.5, 0.01);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.15f, 2.0f);
            }
        }
    }

    private void processFireAvatar() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.FIRE_AVATAR)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FIRE_AVATAR);
            double radius = Math.min(talent.getValue(0), 8.0); // Cap a 8 blocs
            double damagePercent = talent.getValue(1);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(20);
                }
            }

            // Visual aura (reduit de 16 a 8 particules)
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(x, 0.5, z), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    private void processInferno() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.INFERNO)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.INFERNO);
            if (!checkCooldown(player, "inferno", (long) talent.getValue(0))) continue;

            double damagePercent = talent.getValue(1);
            double radius = Math.min(talent.getValue(2), 12.0); // Cap a 12 blocs

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    le.setFireTicks(100);
                    burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + 5000);
                }
            }

            // Nova visual optimise (moins de particules)
            for (double r = 1; r <= radius; r += 2) {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(x, 0.3, z), 1, 0, 0, 0, 0.05);
                }
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
            sendActionBar(player, "§6§l+ INFERNO +");
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
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_WINTER)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_WINTER);
            double radius = Math.min(talent.getValue(0), 10.0); // Cap a 10 blocs
            double slowPercent = talent.getValue(1);

            int amplifier = Math.min((int)(slowPercent * 5), 3); // Cap amplifier a 3

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, amplifier, false, true, true));
                }
            }

            // Winter visual (reduit)
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 5, radius/2, 1, radius/2, 0.01);
        }
    }

    private void processDivineJudgment() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT);
            if (!checkCooldown(player, "divine_judgment", (long) talent.getValue(0))) continue;

            double damagePercent = talent.getValue(1);
            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            // Strike enemies - RANGE REDUIT de 50 a 25 blocs pour eviter l'abus
            double range = 25.0;
            int struck = 0;
            int maxTargets = 30; // Limite le nombre de cibles
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (struck >= maxTargets) break;
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    le.damage(damage, player);
                    // Lightning visual (sans strikeLightningEffect qui est lourd)
                    spawnLightningVisual(player.getLocation().add(0, 10, 0), le.getLocation().add(0, 1, 0));
                    struck++;
                }
            }

            if (struck > 0) {
                sendActionBar(player, "§e§l+ JUGEMENT DIVIN + §7" + struck + " cibles");
                // Son reduit pour eviter le spam sonore avec plusieurs joueurs
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 0.8f);
                // Effet visuel de charge au joueur
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.5, 0), 8, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    private void processMeteorRain() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.METEOR_RAIN)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.METEOR_RAIN);
            if (!checkCooldown(player, "meteor_rain", (long) talent.getValue(0))) continue;

            int meteors = Math.min((int) talent.getValue(1), 15); // Cap a 15 meteores
            double damagePercent = talent.getValue(2);
            double zone = Math.min(talent.getValue(3), 20.0); // Cap zone a 20 blocs

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            Location center = player.getLocation();

            sendActionBar(player, "§4§l+ PLUIE DE METEORES +");

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

                    // Visual (reduit)
                    impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 25, 2, 1, 2, 0.2);
                    impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.8f);
                }, index * 3L);
            }
        }
    }

    private void processSoulRegen() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_HARVEST)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_HARVEST);
            double regenPerOrb = talent.getValue(0);
            int orbs = getSoulOrbs(player);

            if (orbs > 0) {
                double totalRegen = player.getMaxHealth() * regenPerOrb * orbs;
                // Cap la regen a 5% HP max par tick
                totalRegen = Math.min(totalRegen, player.getMaxHealth() * 0.05);
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

        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO);
            long freezeTime = (long) talent.getValue(0);
            double bossDamageMultiplier = Math.min(talent.getValue(1), 3.0); // Cap a 300% pour les boss

            for (Map.Entry<UUID, Long> entry : frozenEnemies.entrySet()) {
                if (now - entry.getValue() >= freezeTime) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity == null || entity.isDead()) continue;
                    if (!(entity instanceof LivingEntity le)) continue;

                    // Check if boss ou elite (> 50 HP)
                    boolean isBossOrElite = le.getScoreboardTags().contains("boss") ||
                                            le.getScoreboardTags().contains("elite") ||
                                            le.getMaxHealth() > 50;

                    if (isBossOrElite) {
                        // Boss/Elite: degats % capped
                        double damage = le.getMaxHealth() * (bossDamageMultiplier / 10.0);
                        le.damage(damage, player);
                    } else {
                        // Mobs normaux: instakill
                        le.setHealth(0);
                    }

                    // Visual (reduit)
                    le.getWorld().spawnParticle(Particle.ITEM, le.getLocation().add(0, 1, 0), 25, 0.5, 1, 0.5, 0.1,
                        new org.bukkit.inventory.ItemStack(Material.ICE));
                    le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.3f);

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

    // ==================== SHADOW PRIEST PERIODIC PROCESSORS ====================

    /**
     * Processe les DOTs Shadow Word: Pain
     */
    private void processShadowDots() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, ShadowDotData>> iterator = shadowDots.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ShadowDotData> entry = iterator.next();
            ShadowDotData dot = entry.getValue();

            if (now > dot.expiry) {
                iterator.remove();
                continue;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead()) {
                iterator.remove();
                continue;
            }

            if (!(entity instanceof LivingEntity target)) continue;

            Player owner = Bukkit.getPlayer(dot.ownerUuid);
            if (owner == null || !owner.isOnline()) {
                iterator.remove();
                continue;
            }

            // Calculer l'intervalle de tick (1s normal, 0.5s en Voidform avec le talent)
            long tickInterval = 1000;
            if (isInVoidform(owner) && hasTalentEffect(owner, Talent.TalentEffectType.VOIDFORM)) {
                Talent voidformTalent = getTalentWithEffect(owner, Talent.TalentEffectType.VOIDFORM);
                double speedBonus = voidformTalent.getValue(1); // 50% faster
                tickInterval = (long)(tickInterval / (1 + speedBonus));
            }

            if (now - dot.lastTick < tickInterval) continue;
            dot.lastTick = now;

            // Appliquer les dégâts
            double damage = dot.damagePerSecond;

            // Bonus de Voidform (+30% dégâts)
            if (isInVoidform(owner) && hasTalentEffect(owner, Talent.TalentEffectType.VOIDFORM)) {
                Talent voidformTalent = getTalentWithEffect(owner, Talent.TalentEffectType.VOIDFORM);
                damage *= (1 + voidformTalent.getValue(0));
            }

            // Bonus d'Insanity
            if (hasTalentEffect(owner, Talent.TalentEffectType.INSANITY)) {
                Talent insanityTalent = getTalentWithEffect(owner, Talent.TalentEffectType.INSANITY);
                double insanityBonus = insanityTalent.getValue(0) * getInsanity(owner);
                damage *= (1 + insanityBonus);
            }

            target.damage(damage, owner);

            // Générer de l'Insanity
            addInsanity(owner, dot.insanityPerTick);

            // Visual léger
            target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.05);
        }
    }

    /**
     * Processe les DOTs Vampiric Touch
     */
    private void processVampiricTouchDots() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, VampiricTouchData>> iterator = vampiricTouchDots.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, VampiricTouchData> entry = iterator.next();
            VampiricTouchData dot = entry.getValue();

            if (now > dot.expiry) {
                iterator.remove();
                continue;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead()) {
                iterator.remove();
                continue;
            }

            if (!(entity instanceof LivingEntity target)) continue;

            Player owner = Bukkit.getPlayer(dot.ownerUuid);
            if (owner == null || !owner.isOnline()) {
                iterator.remove();
                continue;
            }

            // Calculer l'intervalle de tick
            long tickInterval = 1000;
            if (isInVoidform(owner) && hasTalentEffect(owner, Talent.TalentEffectType.VOIDFORM)) {
                Talent voidformTalent = getTalentWithEffect(owner, Talent.TalentEffectType.VOIDFORM);
                double speedBonus = voidformTalent.getValue(1);
                tickInterval = (long)(tickInterval / (1 + speedBonus));
            }

            if (now - dot.lastTick < tickInterval) continue;
            dot.lastTick = now;

            // Appliquer les dégâts
            double damage = dot.damagePerSecond;

            // Bonus de Voidform
            if (isInVoidform(owner) && hasTalentEffect(owner, Talent.TalentEffectType.VOIDFORM)) {
                Talent voidformTalent = getTalentWithEffect(owner, Talent.TalentEffectType.VOIDFORM);
                damage *= (1 + voidformTalent.getValue(0));
            }

            // Bonus d'Insanity
            if (hasTalentEffect(owner, Talent.TalentEffectType.INSANITY)) {
                Talent insanityTalent = getTalentWithEffect(owner, Talent.TalentEffectType.INSANITY);
                double insanityBonus = insanityTalent.getValue(0) * getInsanity(owner);
                damage *= (1 + insanityBonus);
            }

            target.damage(damage, owner);

            // Soigner le joueur
            double heal = damage * dot.healPercent;
            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));

            // Visual
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.5, 0), 2, 0.1, 0.2, 0.1, 0);
        }
    }

    /**
     * Processe les Shadowy Apparitions
     */
    private void processShadowyApparitions() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.SHADOWY_APPARITIONS)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SHADOWY_APPARITIONS);
            long spawnInterval = (long) talent.getValue(0);
            double damagePercent = talent.getValue(1);
            double insanityGain = talent.getValue(2);

            if (!checkCooldown(player, "shadowy_apparitions", spawnInterval)) continue;

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();

            // Trouver les ennemis avec nos DOTs
            for (Map.Entry<UUID, ShadowDotData> entry : shadowDots.entrySet()) {
                if (!entry.getValue().ownerUuid.equals(player.getUniqueId())) continue;

                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity == null || entity.isDead()) continue;
                if (!(entity instanceof LivingEntity target)) continue;

                // Créer une apparition (effet visuel + dégâts)
                Location playerLoc = player.getLocation().add(0, 1, 0);
                Location targetLoc = target.getLocation().add(0, 1, 0);

                // Animation visuelle de l'apparition
                spawnApparitionEffect(playerLoc, targetLoc);

                // Dégâts après un délai (simuler le vol)
                double distance = playerLoc.distance(targetLoc);
                long travelTime = (long)(distance * 50); // 50ms par bloc

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (target.isValid() && !target.isDead()) {
                        double damage = baseDamage * damagePercent;

                        // Bonus Insanity
                        if (hasTalentEffect(player, Talent.TalentEffectType.INSANITY)) {
                            Talent insanityTalent = getTalentWithEffect(player, Talent.TalentEffectType.INSANITY);
                            damage *= (1 + insanityTalent.getValue(0) * getInsanity(player));
                        }

                        target.damage(damage, player);
                        addInsanity(player, insanityGain);

                        // Impact visual
                        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.05);
                    }
                }, travelTime / 50);

                break; // Une apparition par tick
            }
        }
    }

    /**
     * Effet visuel d'une apparition d'ombre
     */
    private void spawnApparitionEffect(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        World world = from.getWorld();

        // Tracer le chemin de l'apparition
        for (double d = 0; d < distance; d += 1.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.SOUL, point, 2, 0.1, 0.1, 0.1, 0.01);
        }

        world.playSound(from, Sound.PARTICLE_SOUL_ESCAPE, 0.3f, 1.5f);
    }

    /**
     * Decay de l'Insanity hors combat
     */
    private void processInsanityDecay() {
        long now = System.currentTimeMillis();

        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.INSANITY)) continue;

            // Vérifier si hors combat (5 secondes sans attaque)
            Long lastCombat = lastCombatTime.get(uuid);
            if (lastCombat != null && now - lastCombat < 5000) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.INSANITY);
            double decayPerSecond = talent.getValue(2);

            double currentInsanity = getInsanity(player);
            if (currentInsanity > 0) {
                double newInsanity = Math.max(0, currentInsanity - decayPerSecond);
                playerInsanity.put(uuid, newInsanity);
            }
        }
    }

    /**
     * Tick du Voidform - drain d'Insanity et bonus
     */
    private void processVoidformTick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = voidformActive.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID uuid = entry.getKey();

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Vérifier expiration (si durée fixe)
            if (entry.getValue() != Long.MAX_VALUE && now > entry.getValue()) {
                exitVoidform(player);
                iterator.remove();
                continue;
            }

            // Traiter le drain/gain d'Insanity si le joueur a le talent Voidform
            if (hasTalentEffect(player, Talent.TalentEffectType.VOIDFORM)) {
                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.VOIDFORM);
                double insanityGainPerSecond = talent.getValue(2);
                double insanityDrainPerSecond = talent.getValue(3);

                double currentInsanity = getInsanity(player);

                // Net change (gain - drain) / 2 car tick = 0.5s
                double netChange = (insanityGainPerSecond - insanityDrainPerSecond) / 2.0;
                double newInsanity = currentInsanity + netChange;

                if (newInsanity <= 0) {
                    // Sortir du Voidform
                    exitVoidform(player);
                    iterator.remove();
                    playerInsanity.put(uuid, 0.0);
                } else {
                    playerInsanity.put(uuid, Math.min(100, newInsanity));
                }
            }

            // Visual Voidform
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
        }
    }

    /**
     * Sortir du Voidform
     */
    private void exitVoidform(Player player) {
        voidformActive.remove(player.getUniqueId());
        player.sendMessage("§8§l- VOIDFORM TERMINEE -");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.5f);
    }

    /**
     * Psychic Horror - Fear périodique
     */
    private void processPsychicHorror() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.PSYCHIC_HORROR)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PSYCHIC_HORROR);
            long cooldown = (long) talent.getValue(0);

            if (!checkCooldown(player, "psychic_horror", cooldown)) continue;

            double radius = talent.getValue(1);
            long stunDuration = (long) talent.getValue(2);
            double damageReduction = talent.getValue(3);
            long debuffDuration = (long) talent.getValue(4);

            boolean hitAny = false;
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Stun
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(stunDuration / 50), 255, false, false, false));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int)(debuffDuration / 50), (int)(damageReduction * 5), false, true, true));

                    // Tracker le debuff
                    psychicHorrorDebuff.put(le.getUniqueId(), System.currentTimeMillis() + debuffDuration);

                    hitAny = true;
                }
            }

            if (hitAny) {
                // Visual
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation(), 30, radius/2, 1, radius/2, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);

                sendActionBar(player, "§5+ TOURMENT PSYCHIQUE +");
            }
        }
    }

    /**
     * Voidling attacks
     */
    private void processVoidlingAttacks() {
        for (Map.Entry<UUID, UUID> entry : activeVoidlings.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline()) continue;

            Entity voidlingEntity = Bukkit.getEntity(entry.getValue());
            if (voidlingEntity == null || voidlingEntity.isDead()) continue;
            if (!(voidlingEntity instanceof LivingEntity voidling)) continue;

            Talent talent = getTalentWithEffect(owner, Talent.TalentEffectType.VOIDLING);
            if (talent == null) continue;

            double damagePercent = talent.getValue(2);
            double insanityPerHit = talent.getValue(3);

            double baseDamage = owner.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            // Trouver l'ennemi le plus proche
            LivingEntity closest = null;
            double closestDist = Double.MAX_VALUE;
            for (Entity entity : voidling.getNearbyEntities(8, 8, 8)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player) && !entity.getScoreboardTags().contains("voidling")) {
                    double dist = le.getLocation().distance(voidling.getLocation());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = le;
                    }
                }
            }

            if (closest != null && closestDist < 3) {
                closest.damage(damage, owner);
                addInsanity(owner, insanityPerHit);

                // Visual
                closest.getWorld().spawnParticle(Particle.PORTAL, closest.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.1);
            }
        }
    }

    // ==================== INSANITY HELPERS ====================

    /**
     * Obtenir l'Insanity d'un joueur
     */
    private double getInsanity(Player player) {
        return playerInsanity.getOrDefault(player.getUniqueId(), 0.0);
    }

    /**
     * Ajouter de l'Insanity à un joueur
     */
    private void addInsanity(Player player, double amount) {
        if (!hasTalentEffect(player, Talent.TalentEffectType.INSANITY) &&
            !hasTalentEffect(player, Talent.TalentEffectType.SHADOW_WORD)) return;

        double maxInsanity = 100;
        if (hasTalentEffect(player, Talent.TalentEffectType.INSANITY)) {
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.INSANITY);
            maxInsanity = talent.getValue(1);
        }

        double current = getInsanity(player);
        double newValue = Math.min(maxInsanity, current + amount);
        playerInsanity.put(player.getUniqueId(), newValue);

        // Mettre à jour le temps de combat
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Vérifier si un joueur est en Voidform
     */
    private boolean isInVoidform(Player player) {
        return voidformActive.containsKey(player.getUniqueId());
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

        // Clean ice zones
        iceZones.entrySet().removeIf(entry -> now > entry.getValue());

        // Clean shadow DOTs
        shadowDots.entrySet().removeIf(entry -> now > entry.getValue().expiry || Bukkit.getEntity(entry.getKey()) == null);

        // Clean vampiric touch DOTs
        vampiricTouchDots.entrySet().removeIf(entry -> now > entry.getValue().expiry || Bukkit.getEntity(entry.getKey()) == null);

        // Clean psychic horror debuff
        psychicHorrorDebuff.entrySet().removeIf(entry -> now > entry.getValue());

        // Clean dark ascension buff
        darkAscensionBuff.entrySet().removeIf(entry -> now > entry.getValue());

        // Clean dark ascension charging
        darkAscensionCharging.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            return player == null || !player.isSneaking();
        });

        // Clean voidlings
        activeVoidlings.entrySet().removeIf(entry -> {
            Entity voidling = Bukkit.getEntity(entry.getValue());
            return voidling == null || voidling.isDead();
        });
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

    /**
     * Envoie un message dans l'actionbar au lieu du chat pour eviter le spam
     */
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    // ==================== DATA CLASSES ====================

    /**
     * Données pour le DOT Shadow Word: Pain
     */
    private static class ShadowDotData {
        final UUID ownerUuid;
        final double damagePerSecond;
        long expiry;
        final double insanityPerTick;
        long lastTick;

        ShadowDotData(UUID ownerUuid, double damagePerSecond, long expiry, double insanityPerTick, long lastTick) {
            this.ownerUuid = ownerUuid;
            this.damagePerSecond = damagePerSecond;
            this.expiry = expiry;
            this.insanityPerTick = insanityPerTick;
            this.lastTick = lastTick;
        }
    }

    /**
     * Données pour le DOT Vampiric Touch
     */
    private static class VampiricTouchData {
        final UUID ownerUuid;
        final double damagePerSecond;
        long expiry;
        final double healPercent;
        long lastTick;

        VampiricTouchData(UUID ownerUuid, double damagePerSecond, long expiry, double healPercent, long lastTick) {
            this.ownerUuid = ownerUuid;
            this.damagePerSecond = damagePerSecond;
            this.expiry = expiry;
            this.healPercent = healPercent;
            this.lastTick = lastTick;
        }
    }
}
