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

    // ==================== HEAT INTENSITY SYSTEM ====================
    // Intensite de brulure par ennemi (entity UUID -> burn start timestamp)
    private final Map<UUID, Long> burnStartTime = new ConcurrentHashMap<>();
    // Zones de chaleur au sol (location key -> expiry timestamp)
    private final Map<String, Long> heatZones = new ConcurrentHashMap<>();
    // Cooldown Embrasement Critique par ennemi (entity UUID -> timestamp fin cooldown)
    private final Map<UUID, Long> criticalIgnitionCooldowns = new ConcurrentHashMap<>();
    // Intensite max avant explosion (en secondes de brulure)
    private static final double MAX_BURN_INTENSITY = 8.0;

    // Ennemis geles (entity UUID -> freeze start timestamp)
    private final Map<UUID, Long> frozenEnemies = new ConcurrentHashMap<>();

    // ==================== FROST STACKS SYSTEM ====================
    // Stacks de Givre par ennemi (entity UUID -> nombre de stacks)
    private final Map<UUID, Integer> frostStacks = new ConcurrentHashMap<>();
    // Dernier temps d'application de stack (pour decay)
    private final Map<UUID, Long> frostStacksLastApplied = new ConcurrentHashMap<>();
    // Cooldown Brisure Glaciale par ennemi (entity UUID -> timestamp fin cooldown)
    private final Map<UUID, Long> shatterCooldowns = new ConcurrentHashMap<>();
    // Max stacks de givre
    private static final int MAX_FROST_STACKS = 10;

    // Minions invoques (player UUID -> list of minion UUIDs)
    private final Map<UUID, List<UUID>> playerMinions = new ConcurrentHashMap<>();

    // Cooldowns internes
    private final Map<UUID, Map<String, Long>> internalCooldowns = new ConcurrentHashMap<>();

    // Black Sun actif (player UUID -> end timestamp)
    private final Map<UUID, Long> blackSunActive = new ConcurrentHashMap<>();

    // Time Stasis actif (player UUID -> end timestamp)
    private final Map<UUID, Long> timeStasisActive = new ConcurrentHashMap<>();

    // Double sneak detection for Time Stasis (player UUID -> last sneak timestamp)
    private final Map<UUID, Long> lastSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW_MS = 500; // 500ms pour double sneak

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

        // MINION AI TICK (40L = 2s) - Mise a jour des cibles des serviteurs
        new BukkitRunnable() {
            @Override
            public void run() {
                processMinionAI();
            }
        }.runTaskTimer(plugin, 40L, 40L);

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

    /**
     * Empeche les serviteurs d'attaquer leur proprietaire ou d'autres serviteurs allies
     * Priorite HIGHEST pour annuler l'evenement avant tout traitement
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinionAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        // Gerer les projectiles des minions (fleches de squelettes)
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        // Verifier si c'est une attaque de minion a bloquer
        if (shouldPreventMinionAttack(damager, target)) {
            event.setCancelled(true);
        }
    }

    /**
     * Empeche les joueurs d'attaquer leurs propres serviteurs
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAttackMinion(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (!target.getScoreboardTags().contains("player_minion")) return;

        Player attacker = getPlayerAttacker(event);
        if (attacker == null) return;

        // Si le joueur attaque son propre minion, annuler
        if (isPlayerMinion(target, attacker)) {
            event.setCancelled(true);
        }
    }

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

        // Frozen Heart - bonus damage to frozen + bonus per stack
        if (hasTalentEffect(player, Talent.TalentEffectType.FROZEN_HEART)) {
            if (isFrozen(target) || getFrostStacks(target) > 0) {
                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROZEN_HEART);
                double baseBonus = talent.getValue(0); // 20% base
                double bonusPerStack = talent.getValues().length > 2 ? talent.getValue(2) : 0.05; // 5% per stack
                int stacks = getFrostStacks(target);
                bonusDamage += baseDamage * (baseBonus + (bonusPerStack * stacks));
            }
        }

        // Eternal Winter - bonus damage based on stacks
        if (hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_WINTER)) {
            int stacks = getFrostStacks(target);
            if (stacks > 0) {
                Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_WINTER);
                double damagePerStack = talent.getValues().length > 2 ? talent.getValue(2) : 0.05; // 5% per stack
                double maxBonus = talent.getValues().length > 3 ? talent.getValue(3) : 0.40; // 40% max
                double bonus = Math.min(damagePerStack * stacks, maxBonus);
                bonusDamage += baseDamage * bonus;
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

        // Frozen Heart - shatter on frozen kill or with stacks
        if (hasTalentEffect(killer, Talent.TalentEffectType.FROZEN_HEART)) {
            int victimStacks = getFrostStacks(victim);
            if (isFrozen(victim) || victimStacks >= 3) {
                processShatter(killer, victim, victimStacks);
            }
        }

        // Ice Age - frost zone on kill with enough stacks
        if (hasTalentEffect(killer, Talent.TalentEffectType.ICE_AGE)) {
            int victimStacks = getFrostStacks(victim);
            createIceZone(victim.getLocation(), killer, victimStacks);
        }

        // Nettoyer les stacks du mort
        clearFrostStacks(victim);

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
            UUID uuid = player.getUniqueId();

            // Time Stasis activation (double sneak)
            if (hasTalentEffect(player, Talent.TalentEffectType.TIME_STASIS)) {
                long now = System.currentTimeMillis();
                Long lastSneak = lastSneakTime.get(uuid);

                if (lastSneak != null && (now - lastSneak) <= DOUBLE_SNEAK_WINDOW_MS) {
                    // Double sneak detected - activate Time Stasis
                    lastSneakTime.remove(uuid);
                    processTimeStasis(player);
                } else {
                    // Record this sneak for potential double sneak
                    lastSneakTime.put(uuid, now);
                }
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
            // values: chance, duration_s
            double durationSec = talent.getValue(1);
            int fireTicks = (int) (durationSec * 20);
            target.setFireTicks(fireTicks);
            burningEnemies.put(target.getUniqueId(), System.currentTimeMillis() + (long)(durationSec * 1000));

            // Systeme Surchauffe - demarrer/prolonger la brulure
            double intensity = startOrExtendBurn(target, fireTicks);

            // Verifier Ignition Critique si Phoenix est equipe
            if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                checkCriticalIgnition(player, target);
            }

            // Visual ameliore - intensite visuelle selon la Surchauffe
            int particleCount = 10 + (int)(intensity * 2);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), particleCount, 0.3, 0.5, 0.3, 0.03);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1.2, 0), 5, 0.2, 0.3, 0.2, 0.02);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.2f);
        }
    }

    private void processFrostBite(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROST_BITE);
        if (!checkCooldown(player, "frost_bite", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            int stacksToAdd = talent.getValues().length > 3 ? (int) talent.getValue(3) : 1;
            addFrostStacks(player, target, stacksToAdd);
            applyFreeze(target, (int)(talent.getValue(2) * 1000), talent.getValue(1));
        }
    }

    private void processFrostLord(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROST_LORD);
        if (Math.random() < talent.getValue(0)) { // 60% chance
            int stacksToAdd = talent.getValues().length > 2 ? (int) talent.getValue(2) : 2;
            addFrostStacks(player, target, stacksToAdd);
            applyFreeze(target, (int)(talent.getValue(1) * 1000), 0.80);

            // Reduction d'armure si talent le permet
            if (talent.getValues().length > 3) {
                // Visual d'armure brisee
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    /**
     * Ajoute des stacks de Givre a un ennemi
     */
    private void addFrostStacks(Player player, LivingEntity target, int amount) {
        UUID targetId = target.getUniqueId();
        int currentStacks = frostStacks.getOrDefault(targetId, 0);
        int newStacks = Math.min(currentStacks + amount, MAX_FROST_STACKS);
        frostStacks.put(targetId, newStacks);
        frostStacksLastApplied.put(targetId, System.currentTimeMillis());

        // Visual de stack (petits flocons)
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1.5, 0),
            3 + newStacks, 0.2, 0.2, 0.2, 0.01);

        // Verifier si on atteint le seuil pour Brisure Glaciale
        if (hasTalentEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO)) {
            checkAndTriggerShatter(player, target);
        }
    }

    /**
     * Recupere le nombre de stacks de Givre d'un ennemi
     */
    private int getFrostStacks(LivingEntity target) {
        return frostStacks.getOrDefault(target.getUniqueId(), 0);
    }

    /**
     * Retire tous les stacks de Givre d'un ennemi
     */
    private void clearFrostStacks(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        frostStacks.remove(targetId);
        frostStacksLastApplied.remove(targetId);
    }

    // ==================== HEAT INTENSITY METHODS ====================

    /**
     * Demarre ou prolonge la brulure d'un ennemi et retourne l'intensite actuelle
     */
    private double startOrExtendBurn(LivingEntity target, int fireTicks) {
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Si pas encore en train de bruler, demarrer le timer
        if (!burnStartTime.containsKey(targetId)) {
            burnStartTime.put(targetId, now);
        }

        // Appliquer le feu
        target.setFireTicks(Math.max(target.getFireTicks(), fireTicks));
        burningEnemies.put(targetId, now + (fireTicks * 50L));

        return getBurnIntensity(target);
    }

    /**
     * Recupere l'intensite de brulure d'un ennemi (en secondes de brulure continue)
     */
    private double getBurnIntensity(LivingEntity target) {
        Long startTime = burnStartTime.get(target.getUniqueId());
        if (startTime == null) return 0;

        double intensity = (System.currentTimeMillis() - startTime) / 1000.0;
        return Math.min(intensity, MAX_BURN_INTENSITY);
    }

    /**
     * Calcule le bonus de degats base sur l'intensite de brulure
     * Plus l'ennemi brule longtemps, plus il prend de degats
     */
    private double getBurnDamageMultiplier(LivingEntity target) {
        double intensity = getBurnIntensity(target);
        // +5% de degats par seconde de brulure, max +40%
        return 1.0 + (intensity * 0.05);
    }

    /**
     * Retire la brulure d'un ennemi
     */
    private void clearBurn(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        burnStartTime.remove(targetId);
        burningEnemies.remove(targetId);
    }

    /**
     * Verifie si l'ennemi a atteint l'intensite max pour Embrasement Critique
     */
    private void checkCriticalIgnition(Player player, LivingEntity target) {
        if (!hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) return;

        UUID targetId = target.getUniqueId();
        double intensity = getBurnIntensity(target);

        // Verifier si intensite max atteinte (8 secondes de feu continu)
        if (intensity < MAX_BURN_INTENSITY) return;

        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PHOENIX_FLAME);

        // Verifier cooldown par ennemi
        // values: [dmg%, boss_dmg%, radius, cooldown_ms, spread_burn_s]
        long cooldownMs = (long) talent.getValue(3);
        Long cooldownEnd = criticalIgnitionCooldowns.get(targetId);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) return;

        // Declencher Embrasement Critique!
        triggerCriticalIgnition(player, target, talent, intensity);
    }

    /**
     * Declenche l'Embrasement Critique - explosion massive
     * values: [dmg%, boss_dmg%, radius, cooldown_ms, spread_burn_s]
     */
    private void triggerCriticalIgnition(Player player, LivingEntity target, Talent talent, double intensity) {
        UUID targetId = target.getUniqueId();

        // Determiner si c'est un boss/elite
        boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                target.getScoreboardTags().contains("elite") ||
                                target.getMaxHealth() > 50;

        // Calculer les degats en % des PV max
        // values: [dmg%, boss_dmg%, radius, cooldown_ms, spread_burn_s]
        double baseDamagePercent = talent.getValue(0);  // 0.25 = 25%
        double bossDamagePercent = talent.getValue(1);  // 0.12 = 12%
        double damagePercent = isBossOrElite ? bossDamagePercent : baseDamagePercent;
        double totalDamage = target.getMaxHealth() * damagePercent;

        // Appliquer les degats SANS knockback
        damageNoKnockback(target, totalDamage, player);

        // Propager le feu aux ennemis proches
        double radius = talent.getValue(2);
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                startOrExtendBurn(le, 100); // 5 secondes de feu
                damageNoKnockback(le, totalDamage * 0.3, player); // 30% des degats aux proches
            }
        }

        // Creer une zone de chaleur au sol
        createHeatZone(target.getLocation(), 3000, radius);

        // Effets visuels spectaculaires
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.FLAME, loc, 80, 1.5, 1.5, 1.5, 0.2);
        target.getWorld().spawnParticle(Particle.LAVA, loc, 30, 1, 1, 1, 0.15);
        target.getWorld().spawnParticle(Particle.SMOKE, loc, 40, 1, 1, 1, 0.1);
        target.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2, 0.5, 0.5, 0.5, 0);
        target.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        target.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Mettre en cooldown
        long cooldownMs = (long) talent.getValue(3);
        criticalIgnitionCooldowns.put(targetId, System.currentTimeMillis() + cooldownMs);

        // Reset l'intensite de brulure
        burnStartTime.put(targetId, System.currentTimeMillis());

        // Message au joueur
        if (shouldSendTalentMessage(player)) {
            int damagePercentDisplay = (int) (damagePercent * 100);
            player.sendMessage("§c🔥 §6Embrasement Critique! §7" + damagePercentDisplay +
                "% PV max = §c" + String.format("%.1f", totalDamage) + " §7degats!");
        }
    }

    /**
     * Cree une zone de chaleur au sol qui inflige des degats
     */
    private void createHeatZone(Location location, long durationMs, double radius) {
        String key = location.getWorld().getName() + "," +
            Math.floor(location.getX()) + "," +
            Math.floor(location.getY()) + "," +
            Math.floor(location.getZ()) + "," + radius;
        heatZones.put(key, System.currentTimeMillis() + durationMs);

        // Visual de creation
        location.getWorld().spawnParticle(Particle.FLAME, location, 30, radius/2, 0.2, radius/2, 0.05);
        location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.8f);
    }

    // ==================== DAMAGE UTILITY METHODS ====================

    /**
     * Applique des degats a une entite SANS knockback
     */
    private void damageNoKnockback(LivingEntity target, double damage, Player source) {
        // Sauvegarder la velocite actuelle
        Vector velocity = target.getVelocity().clone();

        // Appliquer les degats
        target.damage(damage, source);

        // Restaurer la velocite immediatement pour annuler le knockback
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!target.isDead()) {
                target.setVelocity(velocity);
            }
        });
    }

    /**
     * Applique des degats AoE SANS knockback a tous les ennemis dans une zone
     */
    private void damageAreaNoKnockback(Location center, double radius, double damage, Player source) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                damageNoKnockback(le, damage, source);
            }
        }
    }

    private void applyFreeze(LivingEntity target, int durationMs, double slowStrength) {
        frozenEnemies.put(target.getUniqueId(), System.currentTimeMillis());

        // Calculer le slow base + bonus par stack si Permafrost actif
        double totalSlow = slowStrength;
        int stacks = getFrostStacks(target);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTalentEffect(player, Talent.TalentEffectType.PERMAFROST)) {
                Talent permafrost = getTalentWithEffect(player, Talent.TalentEffectType.PERMAFROST);
                double slowPerStack = permafrost.getValue(1);
                double maxSlow = permafrost.getValue(2);
                totalSlow = Math.min(totalSlow + (stacks * slowPerStack), maxSlow);
                break;
            }
        }

        int amplifier = (int)((totalSlow) * 4); // 0-4 amplifier
        amplifier = Math.min(amplifier, 4);
        int ticks = durationMs / 50;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier, false, true, true));

        // Visual ameliore selon les stacks
        int particleCount = 15 + (stacks * 3);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), particleCount, 0.4, 0.6, 0.4, 0.02);
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.5, 0),
            5 + stacks, 0.3, 0.3, 0.3, 0.1, Material.BLUE_ICE.createBlockData());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);

        // Schedule remove from frozen tracking
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                frozenEnemies.remove(target.getUniqueId());
            }
        }, ticks + 5);
    }

    /**
     * Verifie et declenche la Brisure Glaciale si les conditions sont remplies
     */
    private void checkAndTriggerShatter(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO);
        if (talent == null) return;

        UUID targetId = target.getUniqueId();
        int minStacks = (int) talent.getValue(0);
        int currentStacks = getFrostStacks(target);

        if (currentStacks < minStacks) return;

        // Verifier cooldown par ennemi
        long cooldownMs = (long) talent.getValue(3);
        Long cooldownEnd = shatterCooldowns.get(targetId);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) return;

        // Declencher la Brisure Glaciale!
        triggerGlacialShatter(player, target, talent, currentStacks);
    }

    /**
     * Declenche l'effet Brisure Glaciale
     */
    private void triggerGlacialShatter(Player player, LivingEntity target, Talent talent, int stacks) {
        UUID targetId = target.getUniqueId();

        // Determiner si c'est un boss/elite
        boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                target.getScoreboardTags().contains("elite") ||
                                target.getMaxHealth() > 50;

        // Calculer les degats
        double damagePerStack = isBossOrElite ? talent.getValue(2) : talent.getValue(1);
        double totalDamage = target.getMaxHealth() * damagePerStack * stacks;

        // Appliquer les degats
        target.damage(totalDamage, player);

        // Effets visuels spectaculaires
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.ITEM, loc, 50, 0.8, 1, 0.8, 0.15,
            new org.bukkit.inventory.ItemStack(Material.BLUE_ICE));
        target.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 40, 1, 1.5, 1, 0.1);
        target.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);

        // Mettre en cooldown
        long cooldownMs = (long) talent.getValue(3);
        shatterCooldowns.put(targetId, System.currentTimeMillis() + cooldownMs);

        // Retirer tous les stacks
        clearFrostStacks(target);

        // Message au joueur
        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§b❄ §3Brisure Glaciale! §7" + stacks + " stacks = §c" +
                String.format("%.1f", totalDamage) + " §7degats!");
        }
    }

    private void processChainLightning(Player player, LivingEntity target, double baseDamage) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.CHAIN_LIGHTNING);
        if (!checkCooldown(player, "chain_lightning", talent.getInternalCooldownMs())) return;

        if (Math.random() < talent.getValue(0)) {
            int targets = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double range = Math.min(talent.getValue(3), 10.0); // Cap a 10 blocs pour chain

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

            // First strike on primary target - sans knockback
            for (int s = 0; s < strikes; s++) {
                damageNoKnockback(target, lightningDamage, player);
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

                // Degats sans knockback
                for (int s = 0; s < strikes; s++) {
                    damageNoKnockback(enemy, lightningDamage, player);
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
     * Genere un eclair visuel entre deux points (particules jaunes uniquement)
     * Utilise DUST jaune pour le coeur lumineux et WAX_OFF pour l'effet electrique
     */
    private void spawnLightningVisual(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance < 0.5) return;

        direction.normalize();
        World world = from.getWorld();

        // Couleurs jaune/or pour l'eclair
        Particle.DustOptions yellowDust = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
        Particle.DustOptions brightYellowDust = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.5f);

        // Tracer l'eclair avec un leger zigzag
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        double zigzagOffset = 0;

        for (double d = 0; d < distance; d += 0.5) {
            // Zigzag aleatoire pour effet naturel d'eclair
            zigzagOffset = (Math.random() - 0.5) * 0.4;

            Location point = from.clone().add(direction.clone().multiply(d));
            point.add(perpendicular.clone().multiply(zigzagOffset));

            // Coeur lumineux jaune vif
            world.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0, brightYellowDust);
            // Halo jaune plus large
            world.spawnParticle(Particle.DUST, point, 1, 0.1, 0.1, 0.1, 0, yellowDust);
        }

        // Impact au point d'arrivee (burst jaune)
        world.spawnParticle(Particle.DUST, to, 8, 0.2, 0.2, 0.2, 0, brightYellowDust);
        world.spawnParticle(Particle.WAX_OFF, to, 5, 0.15, 0.15, 0.15, 0.02);
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

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§5§l+ VOIDFORM ACTIVE +");
            player.sendMessage("§7Degats d'ombre §c+30%§7, DOTs acceleres!");
        }
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

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§5§l+ ASCENSION SOMBRE +");
            player.sendMessage("§7Insanity maximale! DOTs etendus de §c50%§7!");
        }
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
            // values: chance, meteors, damage%, zone, burn_extension_s
            int meteors = (int) talent.getValue(1);
            double damagePercent = talent.getValue(2);
            double zone = Math.min(talent.getValue(3), 8.0); // Cap a 8 blocs
            double burnExtension = talent.getValues().length > 4 ? talent.getValue(4) : 2.0;
            int burnTicks = (int) (burnExtension * 20);

            Location center = target.getLocation();

            for (int i = 0; i < meteors; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Random position in zone
                    double offsetX = (Math.random() - 0.5) * zone;
                    double offsetZ = (Math.random() - 0.5) * zone;
                    Location impactLoc = center.clone().add(offsetX, 0, offsetZ);

                    // Damage nearby - sans knockback
                    double damage = baseDamage * damagePercent;
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                            damageNoKnockback(le, damage, player);
                            // Enflammer et prolonger Surchauffe
                            le.setFireTicks(burnTicks);
                            startOrExtendBurn(le, burnTicks);
                            burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + (long)(burnExtension * 1000));

                            // Verifier Ignition Critique si Phoenix est equipe
                            if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                                checkCriticalIgnition(player, le);
                            }
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

    private void processShatter(Player player, LivingEntity victim, int victimStacks) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.FROZEN_HEART);
        double radius = talent.getValue(1);
        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        // Degats scales avec les stacks de la victime
        double damage = baseDamage * (0.3 + (victimStacks * 0.1)); // 30% + 10% par stack

        for (Entity entity : victim.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.damage(damage, player);
                // Ajouter des stacks aux ennemis proches (propagation)
                addFrostStacks(player, le, Math.max(1, victimStacks / 2));
                applyFreeze(le, 1500, 0.4);
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

            // Spawn zombie servant based on killed entity type
            Zombie minion = victim.getWorld().spawn(victim.getLocation(), Zombie.class);
            minion.setCustomName("§5☠ §dServiteur §8[" + player.getName() + "]");
            minion.setCustomNameVisible(true);
            minion.setBaby(false);
            minion.getScoreboardTags().add("player_minion");
            minion.getScoreboardTags().add("owner_" + player.getUniqueId());
            minion.setPersistent(false); // Eviter la persistence au rechargement

            // Set stats based on victim and talent
            double statPercent = talent.getValue(1);
            // Check Immortal Army buff
            if (hasTalentEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY)) {
                Talent immortal = getTalentWithEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY);
                statPercent += immortal.getValue(1);
            }

            // Use victim's stats as base
            double victimMaxHealth = victim.getMaxHealth();
            double baseHealth = Math.max(victimMaxHealth * statPercent, 10); // Minimum 10 HP
            minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            minion.setHealth(minion.getMaxHealth());

            // Give zombie more speed and attack damage
            minion.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
            minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(5 * statPercent);

            minions.add(minion.getUniqueId());

            // Set initial target
            setMinionTarget(player, minion);

            // Schedule despawn
            long duration = (long) talent.getValue(2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (minion.isValid() && !minion.isDead()) {
                    // Visual de despawn
                    minion.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
                    minion.remove();
                    minions.remove(minion.getUniqueId());
                }
            }, duration / 50);

            // Visual
            victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation(), 20, 0.5, 1, 0.5, 0.05);
            victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
            sendActionBar(player, "§5☠ §dServiteur releve! §8(" + minions.size() + "/" + maxMinions + ")");
        }
    }

    private void processTimeStasis(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.TIME_STASIS);
        long cooldownMs = (long) talent.getValue(0);

        // Check cooldown with feedback
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = internalCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Long lastUse = playerCooldowns.get("time_stasis");
        long now = System.currentTimeMillis();

        if (lastUse != null && now - lastUse < cooldownMs) {
            // On cooldown - show remaining time
            long remainingMs = cooldownMs - (now - lastUse);
            int remainingSec = (int) Math.ceil(remainingMs / 1000.0);
            sendActionBar(player, "§b❄ Stase Temporelle §8- §cCooldown: §e" + remainingSec + "s");
            return;
        }

        // Set cooldown
        playerCooldowns.put("time_stasis", now);

        long duration = (long) talent.getValue(1);
        int stacksToApply = (int) talent.getValue(2);
        timeStasisActive.put(uuid, System.currentTimeMillis() + duration);

        // Collect all affected enemies for ice shatter at the end
        List<LivingEntity> frozenTargets = new ArrayList<>();

        // Freeze all nearby enemies and apply frost stacks
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                // Freeze
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(duration / 50), 255, false, false, false));
                le.setAI(false);
                frozenTargets.add(le);

                // Apply frost stacks (10 stacks as per talent description)
                int currentStacks = frostStacks.getOrDefault(le.getUniqueId(), 0);
                frostStacks.put(le.getUniqueId(), Math.min(currentStacks + stacksToApply, MAX_FROST_STACKS));
                frostStacksLastApplied.put(le.getUniqueId(), now);

                // Visual on each enemy
                le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            }
        }

        // Visual & sound
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 15, 10, 15, 0.01);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§b§l+ STASE TEMPORELLE +");
            player.sendMessage("§7Le temps est fige pendant §b" + (duration / 1000) + "s§7! §3" + stacksToApply + " stacks§7 appliques.");
        }

        // Schedule AI restore and ice shatter at the end
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity le : frozenTargets) {
                if (le.isValid() && !le.isDead()) {
                    le.setAI(true);

                    // Trigger Brisure Glaciale automatically if player has Absolute Zero talent
                    if (hasTalentEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO)) {
                        Talent shatterTalent = getTalentWithEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO);
                        int stacks = frostStacks.getOrDefault(le.getUniqueId(), 0);
                        if (stacks >= shatterTalent.getValue(0)) {
                            triggerGlacialShatter(player, le, shatterTalent, stacks);
                        }
                    }
                }
            }
        }, duration / 50);
    }

    private void processNecromancerSummon(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.NECROMANCER);
        if (!checkCooldown(player, "necromancer", 2000)) return;

        int orbs = getSoulOrbs(player);
        if (orbs <= 0) return;

        List<UUID> minions = playerMinions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        int maxMinions = (int) talent.getValue(2);

        if (minions.size() >= maxMinions) {
            sendActionBar(player, "§8Maximum de squelettes atteint!");
            return;
        }

        // Consume one orb, summon one skeleton
        soulOrbs.put(player.getUniqueId(), orbs - 1);

        Skeleton minion = player.getWorld().spawn(player.getLocation().add(
            Math.random() * 2 - 1, 0, Math.random() * 2 - 1), Skeleton.class);
        minion.setCustomName("§8☠ §7Squelette §8[" + player.getName() + "]");
        minion.setCustomNameVisible(true);
        minion.getScoreboardTags().add("player_minion");
        minion.getScoreboardTags().add("owner_" + player.getUniqueId());
        minion.setPersistent(false); // Eviter la persistence au rechargement

        // Check Immortal Army buff for stats
        double statPercent = talent.getValue(0);
        if (hasTalentEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY)) {
            Talent immortal = getTalentWithEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY);
            statPercent += immortal.getValue(1);
        }

        // Set stats based on player
        double playerMaxHealth = player.getMaxHealth();
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(playerMaxHealth * statPercent);
        minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(playerDamage * statPercent);
        minion.setHealth(minion.getMaxHealth());

        // Equip skeleton with bow for ranged attacks
        minion.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.BOW));

        minions.add(minion.getUniqueId());

        // Set initial target
        setMinionTarget(player, minion);

        // Schedule despawn
        long duration = (long) talent.getValue(1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (minion.isValid() && !minion.isDead()) {
                // Visual de despawn
                minion.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
                minion.remove();
                minions.remove(minion.getUniqueId());
            }
        }, duration / 50);

        // Visual
        player.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 15, 0.3, 1, 0.3, 0.05);
        player.getWorld().playSound(minion.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.8f);
        sendActionBar(player, "§8☠ §7Squelette invoque! §8(" + minions.size() + "/" + maxMinions + ")");
    }

    // ==================== PERIODIC PROCESSORS ====================

    private void processFireSpread() {
        if (!hasTalentEffectForAnyPlayer(Talent.TalentEffectType.FIRE_SPREAD)) return;

        // Trouver le talent pour les valeurs
        Talent talent = null;
        Player talentOwner = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTalentEffect(player, Talent.TalentEffectType.FIRE_SPREAD)) {
                talent = getTalentWithEffect(player, Talent.TalentEffectType.FIRE_SPREAD);
                talentOwner = player;
                break;
            }
        }
        if (talent == null) return;

        // values: range, propagation_duration_s
        double range = talent.getValue(0);
        double propagationDuration = talent.getValues().length > 1 ? talent.getValue(1) : 2.0;
        int propagationTicks = (int) (propagationDuration * 20);

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

            // Propager le feu aux ennemis proches (systeme Surchauffe)
            for (Entity nearby : entity.getNearbyEntities(range, range, range)) {
                if (nearby instanceof LivingEntity le && !(nearby instanceof Player)) {
                    // Enflammer et demarrer/prolonger Surchauffe
                    le.setFireTicks(propagationTicks);
                    startOrExtendBurn(le, propagationTicks);
                    burningEnemies.put(nearby.getUniqueId(), now + (long)(propagationDuration * 1000));

                    // Verifier Ignition Critique si Phoenix est equipe
                    if (talentOwner != null && hasTalentEffect(talentOwner, Talent.TalentEffectType.PHOENIX_FLAME)) {
                        checkCriticalIgnition(talentOwner, le);
                    }
                }
            }

            // Visual de propagation
            if (entity instanceof LivingEntity le) {
                le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 0.5, 0),
                    3, range/3, 0.2, range/3, 0.02);
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
            double range = Math.min(talent.getValue(3), 15.0); // Cap a 15 blocs

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
                // Degats sans knockback
                damageNoKnockback(enemy, damage, player);
                spawnLightningVisual(player.getLocation().add(0, 1.5, 0), enemy.getLocation().add(0, 1, 0));
            }

            if (!nearbyEnemies.isEmpty()) {
                // Son leger pour indiquer l'activation
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.2f, 1.8f);
                // Petit effet electrique jaune autour du joueur
                Particle.DustOptions yellowSpark = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 0.8f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 6, 0.3, 0.3, 0.3, 0, yellowSpark);
            }
        }
    }

    private void processPerpetualStorm() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.PERPETUAL_STORM)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.PERPETUAL_STORM);
            double radius = Math.min(talent.getValue(1), 12.0); // Cap a 12 blocs
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
                // Degats sans knockback
                damageNoKnockback(enemy, damage, player);
                spawnLightningVisual(player.getLocation().add(0, 2, 0), enemy.getLocation().add(0, 1, 0));
                count++;
            }

            // Storm visual around player - aura electrique jaune
            if (count > 0) {
                // Petit arc electrique jaune autour du joueur
                Particle.DustOptions yellowAura = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.2f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 2, 0), 5, 0.5, 0.3, 0.5, 0, yellowAura);
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
            // values: radius, damage_per_second%, burn_extension_per_s
            double radius = Math.min(talent.getValue(0), 8.0); // Cap a 8 blocs
            double damagePercent = talent.getValue(1);
            double burnExtension = talent.getValues().length > 2 ? talent.getValue(2) : 1.0;
            int burnTicks = (int) (burnExtension * 20);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Degats sans knockback (AoE)
                    damageNoKnockback(le, damage, player);
                    // Enflammer et prolonger Surchauffe
                    le.setFireTicks(burnTicks);
                    startOrExtendBurn(le, burnTicks);
                    burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + (long)(burnExtension * 1000));

                    // Verifier Ignition Critique si Phoenix est equipe
                    if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                        checkCriticalIgnition(player, le);
                    }
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

            // values: cooldown_ms, damage%, radius, burn_extension_s
            double damagePercent = talent.getValue(1);
            double radius = Math.min(talent.getValue(2), 12.0); // Cap a 12 blocs
            double burnExtension = talent.getValues().length > 3 ? talent.getValue(3) : 4.0;
            int burnTicks = (int) (burnExtension * 20);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Degats sans knockback (AoE nova)
                    damageNoKnockback(le, damage, player);
                    // Enflammer et prolonger massivement Surchauffe
                    le.setFireTicks(burnTicks);
                    startOrExtendBurn(le, burnTicks);
                    burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + (long)(burnExtension * 1000));

                    // Verifier Ignition Critique si Phoenix est equipe
                    if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                        checkCriticalIgnition(player, le);
                    }
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
            // values: cooldown, duration, damage%/s, radius, burn_extension_per_s
            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_SUN);
            if (talent == null) continue;

            double damagePercent = talent.getValue(2);
            double radius = Math.min(talent.getValue(3), 10.0); // Cap a 10 blocs
            double burnExtension = talent.getValues().length > 4 ? talent.getValue(4) : 2.0;
            int burnTicks = (int) (burnExtension * 20);

            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Degats sans knockback
                    damageNoKnockback(le, damage, player);
                    // Enflammer et prolonger Surchauffe
                    le.setFireTicks(burnTicks);
                    startOrExtendBurn(le, burnTicks);
                    burningEnemies.put(le.getUniqueId(), now + (long)(burnExtension * 1000));

                    // Verifier Ignition Critique si Phoenix est equipe
                    if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                        checkCriticalIgnition(player, le);
                    }
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

            if (shouldSendTalentMessage(player)) {
                player.sendMessage("§c§l+ SOLEIL NOIR +");
                player.sendMessage("§7Un soleil ardent brule vos ennemis!");
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }
    }

    private void processEternalWinter() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ETERNAL_WINTER)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ETERNAL_WINTER);
            double radius = Math.min(talent.getValue(0), 8.0); // Cap a 8 blocs
            int stacksPerTick = talent.getValues().length > 1 ? (int) talent.getValue(1) : 1;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Ajouter des stacks de Givre
                    addFrostStacks(player, le, stacksPerTick);

                    // Slow progressif base sur les stacks
                    int stacks = getFrostStacks(le);
                    int amplifier = Math.min(stacks / 2, 3); // 2 stacks = 1 amplifier, cap a 3
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, amplifier, false, false, true));
                }
            }

            // Winter visual ameliore - aura de froid
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0),
                10, radius/2, 1.5, radius/2, 0.02);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0),
                3, radius/3, 0.5, radius/3, 0.01);
        }
    }

    private void processDivineJudgment() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DIVINE_JUDGMENT);
            if (!checkCooldown(player, "divine_judgment", (long) talent.getValue(0))) continue;

            // values: cooldown_ms, damage%, range
            double damagePercent = talent.getValue(1);
            double range = talent.getValues().length > 2 ? Math.min(talent.getValue(2), 25.0) : 25.0; // Cap a 25 blocs
            double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            double damage = baseDamage * damagePercent;

            int struck = 0;
            int maxTargets = 30; // Limite le nombre de cibles
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (struck >= maxTargets) break;
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Degats sans knockback
                    damageNoKnockback(le, damage, player);
                    // Lightning visual (sans strikeLightningEffect qui est lourd)
                    spawnLightningVisual(player.getLocation().add(0, 10, 0), le.getLocation().add(0, 1, 0));
                    struck++;
                }
            }

            if (struck > 0) {
                sendActionBar(player, "§e§l+ JUGEMENT DIVIN + §7" + struck + " cibles");
                // Son reduit pour eviter le spam sonore avec plusieurs joueurs
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 0.8f);
                // Effet visuel de charge jaune au joueur
                Particle.DustOptions divineYellow = new Particle.DustOptions(Color.fromRGB(255, 230, 0), 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.5, 0.3, 0, divineYellow);
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

            // values: cooldown_ms, meteors, damage%, zone, burn_per_impact_s
            int meteors = Math.min((int) talent.getValue(1), 15); // Cap a 15 meteores
            double damagePercent = talent.getValue(2);
            double zone = Math.min(talent.getValue(3), 15.0); // Cap zone a 15 blocs (max spec)
            double burnPerImpact = talent.getValues().length > 4 ? talent.getValue(4) : 3.0;
            int burnTicks = (int) (burnPerImpact * 20);

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

                    // Damage - sans knockback
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, 3, 3, 3)) {
                        if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                            damageNoKnockback(le, damage, player);
                            // Enflammer massivement (T9 = +3s par meteore)
                            le.setFireTicks(burnTicks);
                            startOrExtendBurn(le, burnTicks);
                            burningEnemies.put(le.getUniqueId(), System.currentTimeMillis() + (long)(burnPerImpact * 1000));

                            // Ignition Critique automatique (T9 special)
                            if (hasTalentEffect(player, Talent.TalentEffectType.PHOENIX_FLAME)) {
                                // Force Ignition Critique apres 2 impacts (atteint rapidement Surchauffe max)
                                double intensity = getBurnIntensity(le);
                                if (intensity >= MAX_BURN_INTENSITY * 0.75) {
                                    checkCriticalIgnition(player, le);
                                }
                            }
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
                double slowPercent = talent.getValues().length > 1 ? talent.getValue(1) : 0.30;
                int stacksPerSec = talent.getValues().length > 2 ? (int) talent.getValue(2) : 1;

                // Aura qui ralentit et ajoute des stacks (ne gele plus directement)
                for (Entity nearby : frozen.getNearbyEntities(auraRadius, auraRadius, auraRadius)) {
                    if (nearby instanceof LivingEntity le && !(nearby instanceof Player)) {
                        // Ajouter des stacks de Givre
                        addFrostStacks(player, le, stacksPerSec);
                        // Appliquer un slow leger (pas de freeze)
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30,
                            (int)(slowPercent * 3), false, false, true));
                    }
                }

                // Blizzard visual ameliore
                frozen.getWorld().spawnParticle(Particle.SNOWFLAKE, frozen.getLocation().add(0, 1, 0),
                    8, auraRadius/2, 0.8, auraRadius/2, 0.02);
                frozen.getWorld().spawnParticle(Particle.CLOUD, frozen.getLocation().add(0, 0.5, 0),
                    3, auraRadius/3, 0.3, auraRadius/3, 0.01);
            }
        }
    }

    /**
     * Processe le systeme de Brisure Glaciale (Absolute Zero redesign)
     * Verifie periodiquement les stacks de Givre et declenche la Brisure si necessaire
     */
    private void processAbsoluteZero() {
        if (!hasTalentEffectForAnyPlayer(Talent.TalentEffectType.ABSOLUTE_ZERO)) return;

        // Nettoyage des cooldowns expires
        long now = System.currentTimeMillis();
        shatterCooldowns.entrySet().removeIf(e -> now > e.getValue());

        // Decay des stacks de Givre (si pas Permafrost)
        boolean hasPermafrost = hasTalentEffectForAnyPlayer(Talent.TalentEffectType.PERMAFROST);
        if (!hasPermafrost) {
            // Les stacks decayent apres 5 secondes sans application
            Iterator<Map.Entry<UUID, Long>> it = frostStacksLastApplied.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (now - entry.getValue() > 5000) {
                    UUID targetId = entry.getKey();
                    int currentStacks = frostStacks.getOrDefault(targetId, 0);
                    if (currentStacks > 0) {
                        frostStacks.put(targetId, currentStacks - 1);
                        if (currentStacks - 1 <= 0) {
                            frostStacks.remove(targetId);
                            it.remove();
                        }
                    }
                }
            }
        }

        // Verification periodique des stacks pour Brisure Glaciale
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!hasTalentEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO)) continue;

            Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.ABSOLUTE_ZERO);
            int minStacks = (int) talent.getValue(0);

            // Verifier chaque ennemi avec des stacks
            for (Map.Entry<UUID, Integer> stackEntry : new HashMap<>(frostStacks).entrySet()) {
                if (stackEntry.getValue() < minStacks) continue;

                Entity entity = Bukkit.getEntity(stackEntry.getKey());
                if (entity == null || entity.isDead()) {
                    frostStacks.remove(stackEntry.getKey());
                    continue;
                }
                if (!(entity instanceof LivingEntity le)) continue;

                // Verifier la distance (10 blocs max)
                if (le.getLocation().distance(player.getLocation()) > 10) continue;

                checkAndTriggerShatter(player, le);
            }
        }
    }

    private void processIceZones() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = iceZones.entrySet().iterator();

        // Trouver le talent Ice Age pour les valeurs
        Talent iceAgeTalent = null;
        Player iceAgeOwner = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTalentEffect(player, Talent.TalentEffectType.ICE_AGE)) {
                iceAgeTalent = getTalentWithEffect(player, Talent.TalentEffectType.ICE_AGE);
                iceAgeOwner = player;
                break;
            }
        }

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

                // Rayon et stacks/s selon le talent
                double radius = iceAgeTalent != null ? iceAgeTalent.getValue(1) : 2.5;
                int stacksPerSec = iceAgeTalent != null && iceAgeTalent.getValues().length > 2 ?
                    (int) iceAgeTalent.getValue(2) : 2;
                double slowPercent = iceAgeTalent != null && iceAgeTalent.getValues().length > 3 ?
                    iceAgeTalent.getValue(3) : 0.35;

                // Ajouter des stacks aux ennemis dans la zone
                for (Entity entity : world.getNearbyEntities(loc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                        if (iceAgeOwner != null) {
                            addFrostStacks(iceAgeOwner, le, stacksPerSec);
                        }
                        // Slow de zone
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30,
                            (int)(slowPercent * 4), false, false, true));
                    }
                }

                // Visual ameliore - zone de givre au sol
                world.spawnParticle(Particle.SNOWFLAKE, loc, 8, radius/2, 0.3, radius/2, 0.02);
                world.spawnParticle(Particle.BLOCK, loc, 5, radius/2, 0.1, radius/2, 0.01,
                    Material.BLUE_ICE.createBlockData());
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Cree une zone de givre a une location (appele sur la mort d'un ennemi gele avec Ice Age)
     */
    private void createIceZone(Location location, Player owner, int victimStacks) {
        Talent talent = getTalentWithEffect(owner, Talent.TalentEffectType.ICE_AGE);
        if (talent == null) return;

        // Verifier si la victime avait assez de stacks
        int minStacks = talent.getValues().length > 4 ? (int) talent.getValue(4) : 5;
        if (victimStacks < minStacks) return;

        long duration = (long) talent.getValue(0);
        String key = location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
        iceZones.put(key, System.currentTimeMillis() + duration);

        // Visual spawn spectaculaire
        location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 50, 1.5, 1, 1.5, 0.08);
        location.getWorld().spawnParticle(Particle.ITEM, location.add(0, 0.5, 0), 30, 1, 0.5, 1, 0.1,
            new org.bukkit.inventory.ItemStack(Material.BLUE_ICE));
        location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.8f);
    }

    /**
     * Version legacy pour compatibilite
     */
    private void createIceZone(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTalentEffect(player, Talent.TalentEffectType.ICE_AGE)) {
                createIceZone(location, player, 10); // Assume max stacks
                return;
            }
        }
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
        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§8§l- VOIDFORM TERMINEE -");
        }
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

    // ==================== MINION UTILITY METHODS ====================

    /**
     * Definit une cible pour un serviteur (squelette ou zombie)
     * Le serviteur attaquera l'ennemi le plus proche du joueur
     */
    private void setMinionTarget(Player owner, Mob minion) {
        if (minion == null || minion.isDead()) return;

        // Chercher l'ennemi le plus proche du joueur (pas du minion)
        LivingEntity closestEnemy = null;
        double closestDistance = 20.0; // Rayon de recherche

        for (Entity entity : owner.getNearbyEntities(closestDistance, closestDistance, closestDistance)) {
            // Ignorer les joueurs et les autres minions
            if (entity instanceof Player) continue;
            if (entity.getScoreboardTags().contains("player_minion")) continue;
            if (!(entity instanceof LivingEntity le)) continue;
            if (le.isDead()) continue;

            double distance = entity.getLocation().distance(owner.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEnemy = le;
            }
        }

        if (closestEnemy != null) {
            minion.setTarget(closestEnemy);
        }
    }

    /**
     * Met a jour periodiquement les cibles de tous les serviteurs
     * Appele toutes les 2 secondes
     */
    private void processMinionAI() {
        for (Map.Entry<UUID, List<UUID>> entry : playerMinions.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline()) continue;

            for (UUID minionId : entry.getValue()) {
                Entity entity = Bukkit.getEntity(minionId);
                if (entity == null || entity.isDead()) continue;
                if (!(entity instanceof Mob minion)) continue;

                // Verifier si le minion a deja une cible valide
                LivingEntity currentTarget = minion.getTarget();
                if (currentTarget != null && !currentTarget.isDead() &&
                    currentTarget.getLocation().distance(owner.getLocation()) < 25) {
                    continue; // Garder la cible actuelle
                }

                // Chercher une nouvelle cible
                setMinionTarget(owner, minion);

                // Si aucune cible, faire suivre le joueur
                if (minion.getTarget() == null) {
                    // Teleporter le minion s'il est trop loin
                    if (minion.getLocation().distance(owner.getLocation()) > 30) {
                        minion.teleport(owner.getLocation().add(
                            Math.random() * 3 - 1.5, 0, Math.random() * 3 - 1.5));
                    }
                }
            }
        }
    }

    /**
     * Verifie si une entite est un serviteur d'un joueur specifique
     */
    private boolean isPlayerMinion(Entity entity, Player player) {
        return entity.getScoreboardTags().contains("player_minion") &&
               entity.getScoreboardTags().contains("owner_" + player.getUniqueId());
    }

    /**
     * Empeche les serviteurs d'attaquer leur proprietaire ou d'autres serviteurs allies
     */
    private boolean shouldPreventMinionAttack(Entity attacker, Entity target) {
        // Si l'attaquant n'est pas un minion, ne pas bloquer
        if (!attacker.getScoreboardTags().contains("player_minion")) return false;

        // Trouver le proprietaire du minion
        String ownerTag = attacker.getScoreboardTags().stream()
            .filter(tag -> tag.startsWith("owner_"))
            .findFirst()
            .orElse(null);

        if (ownerTag == null) return false;

        String ownerUUID = ownerTag.substring(6);

        // Bloquer si la cible est le proprietaire
        if (target instanceof Player player && player.getUniqueId().toString().equals(ownerUUID)) {
            return true;
        }

        // Bloquer si la cible est un autre minion du meme proprietaire
        if (target.getScoreboardTags().contains("player_minion") &&
            target.getScoreboardTags().contains(ownerTag)) {
            return true;
        }

        return false;
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

    private boolean shouldSendTalentMessage(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data != null && data.isTalentMessagesEnabled();
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
