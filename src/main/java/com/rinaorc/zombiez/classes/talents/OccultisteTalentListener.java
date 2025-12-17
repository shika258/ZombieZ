package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;
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
    // Max stacks de givre (cap à 20 pour éviter les one-shots)
    private static final int MAX_FROST_STACKS = 20;

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

    // ==================== GRAVITY/VOID TRACKING ====================

    // Shadow DOTs actifs (entity UUID -> ShadowDotData)
    private final Map<UUID, ShadowDotData> shadowDots = new ConcurrentHashMap<>();

    // Vampiric Touch DOTs (entity UUID -> VampiricTouchData)
    private final Map<UUID, VampiricTouchData> vampiricTouchDots = new ConcurrentHashMap<>();

    // Dark Gravity - ennemis ralentis (entity UUID -> expiry timestamp)
    private final Map<UUID, Long> gravitySlowed = new ConcurrentHashMap<>();

    // Gravity Wells actifs (location key -> GravityWellData)
    private final Map<String, GravityWellData> activeGravityWells = new ConcurrentHashMap<>();

    // Singularities actives (location key -> SingularityData)
    private final Map<String, SingularityData> activeSingularities = new ConcurrentHashMap<>();

    // Black Holes actifs (location key -> BlackHoleData)
    private final Map<String, BlackHoleData> activeBlackHoles = new ConcurrentHashMap<>();

    // Dimensional Rift - ennemis bannis (entity UUID -> BanishedData)
    private final Map<UUID, BanishedData> banishedEntities = new ConcurrentHashMap<>();

    // Multi-kill tracking for Singularity (player UUID -> list of kill timestamps)
    private final Map<UUID, List<Long>> recentKills = new ConcurrentHashMap<>();

    // Periodic gravity effects cooldown tracking
    private final Map<UUID, Long> lastGravityWellSpawn = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBlackHoleSpawn = new ConcurrentHashMap<>();

    // Dimensional Rift per-target cooldowns (entity UUID -> expiry timestamp)
    private final Map<UUID, Long> dimensionalRiftCooldowns = new ConcurrentHashMap<>();

    // Psychic Horror debuff tracking (entity UUID -> expiry timestamp)
    private final Map<UUID, Long> psychicHorrorDebuff = new ConcurrentHashMap<>();

    // Dark Ascension buff tracking (player UUID -> expiry timestamp)
    private final Map<UUID, Long> darkAscensionBuff = new ConcurrentHashMap<>();

    // Dark Ascension charging tracking (player UUID -> charge start timestamp)
    private final Map<UUID, Long> darkAscensionCharging = new ConcurrentHashMap<>();

    // Active Voidlings tracking (player UUID -> voidling entity UUID)
    private final Map<UUID, UUID> activeVoidlings = new ConcurrentHashMap<>();

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
                processGravityEffects(); // Gravity Wells, Singularities, Black Holes
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
                processBanishedEntities(); // Dimensional Rift
                processPeriodicGravitySpawns(); // Gravity Well + Black Hole auto-spawn
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

        // MINION AI TICK (20L = 1s) - Mise a jour des cibles et attaques des serviteurs
        new BukkitRunnable() {
            @Override
            public void run() {
                processMinionAI();
            }
        }.runTaskTimer(plugin, 20L, 20L);

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

    /**
     * Applique les degats du joueur proprietaire aux attaques des minions
     * Les minions font maintenant des degats bases sur les stats du joueur
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMinionDealDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        // Gerer les projectiles des minions (fleches de squelettes)
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        // Verifier si c'est un minion du joueur
        if (!damager.getScoreboardTags().contains("player_minion")) return;

        // Ne pas modifier les degats aux joueurs (ne devrait pas arriver mais securite)
        if (target instanceof Player) return;

        // Trouver le proprietaire du minion
        String ownerUUID = damager.getScoreboardTags().stream()
            .filter(tag -> tag.startsWith("owner_"))
            .map(tag -> tag.substring(6))
            .findFirst()
            .orElse(null);

        if (ownerUUID == null) return;

        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
        if (owner == null || !owner.isOnline()) return;

        // Calculer les degats bases sur les stats du joueur
        double playerDamage = calculateMinionDamage(owner);

        // Appliquer les degats
        event.setDamage(playerDamage);
    }

    /**
     * Calcule les degats qu'un minion devrait faire base sur les stats du joueur
     */
    private double calculateMinionDamage(Player player) {
        // Degats de base du joueur (attribut Minecraft)
        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();

        // Bonus de degats % de l'equipement ZombieZ
        double damagePercent = plugin.getItemManager().getPlayerStat(player, StatType.DAMAGE_PERCENT);
        baseDamage *= (1 + damagePercent / 100.0);

        // Degats elementaires
        baseDamage += plugin.getItemManager().getPlayerStat(player, StatType.FIRE_DAMAGE);
        baseDamage += plugin.getItemManager().getPlayerStat(player, StatType.ICE_DAMAGE);
        baseDamage += plugin.getItemManager().getPlayerStat(player, StatType.LIGHTNING_DAMAGE);
        baseDamage += plugin.getItemManager().getPlayerStat(player, StatType.POISON_DAMAGE);

        // Critique (les minions peuvent critiquer!)
        double critChance = plugin.getItemManager().getPlayerStat(player, StatType.CRIT_CHANCE) / 100.0;
        if (Math.random() < critChance) {
            double critDamage = 1.5 + plugin.getItemManager().getPlayerStat(player, StatType.CRIT_DAMAGE) / 100.0;
            baseDamage *= critDamage;
        }

        // Multiplicateur de degats des minions (pour equilibrage, 80% des degats du joueur)
        baseDamage *= 0.8;

        return Math.max(1.0, baseDamage);
    }

    /**
     * Empeche les serviteurs de cibler leur proprietaire ou d'autres serviteurs allies
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinionTarget(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("player_minion")) return;

        LivingEntity target = event.getTarget();
        if (target == null) return;

        // Trouver le proprietaire du minion
        String ownerUUID = entity.getScoreboardTags().stream()
            .filter(tag -> tag.startsWith("owner_"))
            .map(tag -> tag.substring(6))
            .findFirst()
            .orElse(null);

        if (ownerUUID == null) return;

        // Empecher le ciblage du proprietaire
        if (target instanceof Player player && player.getUniqueId().toString().equals(ownerUUID)) {
            event.setCancelled(true);
            // Forcer a chercher une autre cible
            if (entity instanceof Mob mob) {
                Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (owner != null) {
                    setMinionTarget(owner, mob);
                }
            }
            return;
        }

        // Empecher le ciblage d'autres serviteurs du meme proprietaire
        if (target.getScoreboardTags().contains("player_minion") &&
            target.getScoreboardTags().contains("owner_" + ownerUUID)) {
            event.setCancelled(true);
            if (entity instanceof Mob mob) {
                Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (owner != null) {
                    setMinionTarget(owner, mob);
                }
            }
        }
    }

    /**
     * Activation des talents par clic droit
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isOccultiste(player)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Necromancer summon (sneak + right click)
        if (player.isSneaking() && hasTalentEffect(player, Talent.TalentEffectType.NECROMANCER)) {
            processNecromancerSummon(player);
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

        // Dark Gravity - slow enemies + bonus damage to slowed
        if (hasTalentEffect(player, Talent.TalentEffectType.DARK_GRAVITY)) {
            processDarkGravity(player, target, event);
        }

        // Dimensional Rift - passive banish at low HP threshold
        if (hasTalentEffect(player, Talent.TalentEffectType.DIMENSIONAL_RIFT)) {
            processDimensionalRiftPassive(player, target);
        }
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

        // Implosion - on kill, pull nearby enemies toward the corpse
        if (hasTalentEffect(killer, Talent.TalentEffectType.IMPLOSION)) {
            processImplosionOnKill(killer, victim.getLocation());
        }

        // Singularity - track kills for multi-kill trigger
        if (hasTalentEffect(killer, Talent.TalentEffectType.SINGULARITY)) {
            trackKillForSingularity(killer, victim.getLocation());
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
                    // Double sneak detected - check cooldown BEFORE activating
                    lastSneakTime.remove(uuid);

                    // Early cooldown check to prevent spam bypass
                    Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.TIME_STASIS);
                    long cooldownMs = (long) talent.getValue(0);
                    Map<String, Long> playerCooldowns = internalCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                    Long lastUse = playerCooldowns.get("time_stasis");

                    if (lastUse != null && now - lastUse < cooldownMs) {
                        // On cooldown - show remaining time and skip
                        long remainingMs = cooldownMs - (now - lastUse);
                        int remainingSec = (int) Math.ceil(remainingMs / 1000.0);
                        sendActionBar(player, "§b❄ Stase Temporelle §8- §cCooldown: §e" + remainingSec + "s");
                        return;
                    }

                    // Set cooldown IMMEDIATELY before processing to prevent race conditions
                    playerCooldowns.put("time_stasis", now);
                    processTimeStasis(player);
                } else {
                    // Record this sneak for potential double sneak
                    lastSneakTime.put(uuid, now);
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

            // Visual ameliore - intensite visuelle selon la Surchauffe (reduit)
            int particleCount = 5 + (int)(intensity);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), particleCount, 0.3, 0.5, 0.3, 0.03);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1.2, 0), 2, 0.2, 0.3, 0.2, 0.02);
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

        // Effets visuels (reduits)
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.FLAME, loc, 30, 1, 1, 1, 0.15);
        target.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.8, 0.8, 0.8, 0.1);
        target.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 0.8, 0.8, 0.8, 0.08);
        target.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0.3, 0.3, 0.3, 0);
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

        // Visual de creation (reduit)
        location.getWorld().spawnParticle(Particle.FLAME, location, 12, radius/2, 0.2, radius/2, 0.05);
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

        // Visual ameliore selon les stacks (reduit)
        int particleCount = 8 + stacks;
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), particleCount, 0.4, 0.6, 0.4, 0.02);
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.5, 0),
            3 + (stacks / 2), 0.3, 0.3, 0.3, 0.1, Material.BLUE_ICE.createBlockData());
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

        // IMPORTANT: Mettre en cooldown AVANT d'appliquer les degats pour eviter la reentrance
        // (target.damage() declenche EntityDamageByEntityEvent qui pourrait re-appeler cette methode)
        long cooldownMs = (long) talent.getValue(3);
        shatterCooldowns.put(targetId, System.currentTimeMillis() + cooldownMs);

        // Retirer tous les stacks AVANT les degats pour eviter les triggers en cascade
        clearFrostStacks(target);

        // Determiner si c'est un boss/elite
        boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                target.getScoreboardTags().contains("elite") ||
                                target.getMaxHealth() > 50;

        // Calculer les degats
        double damagePerStack = isBossOrElite ? talent.getValue(2) : talent.getValue(1);
        double totalDamage = target.getMaxHealth() * damagePerStack * stacks;

        // Appliquer les degats (maintenant que le cooldown est set, pas de reentrance possible)
        target.damage(totalDamage, player);

        // Effets visuels spectaculaires
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.ITEM, loc, 20, 0.6, 0.8, 0.6, 0.1,
            new org.bukkit.inventory.ItemStack(Material.BLUE_ICE));
        target.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0.3, 0.3, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.8, 1, 0.8, 0.08);
        target.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);

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

            // Appliquer ou rafraîchir le DOT
            ShadowDotData dotData = new ShadowDotData(
                player.getUniqueId(),
                damagePerSecond,
                System.currentTimeMillis() + (long) duration,
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

    // ==================== GRAVITY/VOID TALENT PROCESSORS ====================

    /**
     * Dark Gravity - Ralentit les ennemis et augmente les degats contre les ralentis
     */
    private void processDarkGravity(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DARK_GRAVITY);

        double slowPercent = talent.getValue(0);
        long durationMs = (long) talent.getValue(1);
        double damageBonus = talent.getValue(2);

        // Appliquer le ralentissement
        int slowLevel = (int) Math.ceil(slowPercent * 4) - 1; // Slowness I-IV
        int durationTicks = (int) (durationMs / 50);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, slowLevel, false, true));

        // Tracker l'ennemi ralenti
        gravitySlowed.put(target.getUniqueId(), System.currentTimeMillis() + durationMs);

        // Bonus de degats si l'ennemi etait deja ralenti
        if (target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            event.setDamage(event.getDamage() * (1 + damageBonus));
        }

        // Visual subtle
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
    }

    /**
     * Implosion - On kill, pull nearby enemies toward the corpse
     */
    private void processImplosionOnKill(Player player, Location corpseLocation) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.IMPLOSION);

        double radius = talent.getValue(0);
        double damagePercent = talent.getValue(1);
        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double damage = baseDamage * damagePercent;

        int pulled = 0;
        // Attirer les ennemis vers le cadavre
        for (Entity entity : corpseLocation.getWorld().getNearbyEntities(corpseLocation, radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player) && !isPlayerMinion(entity, player)) {
                // Calculer la direction vers le cadavre
                Vector direction = corpseLocation.toVector().subtract(le.getLocation().toVector()).normalize();
                double distance = le.getLocation().distance(corpseLocation);

                // Force d'attraction moyenne
                double pullStrength = Math.min(1.5, (radius - distance) / radius * 2.0);
                le.setVelocity(direction.multiply(pullStrength).setY(0.2));

                // Infliger des degats
                le.damage(damage, player);

                // Visual sur l'entite
                le.getWorld().spawnParticle(Particle.PORTAL, le.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                pulled++;
            }
        }

        if (pulled > 0) {
            // Visual au centre (cadavre)
            corpseLocation.getWorld().spawnParticle(Particle.REVERSE_PORTAL, corpseLocation.add(0, 1, 0), 50, 1, 1, 1, 0.2);
            corpseLocation.getWorld().playSound(corpseLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        }
    }

    /**
     * Gravity Well - Periodic automatic spawn at player location
     */
    private void spawnGravityWellPeriodic(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.GRAVITY_WELL);
        long intervalMs = (long) talent.getValue(4);

        // Check if enough time has passed since last spawn
        Long lastSpawn = lastGravityWellSpawn.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastSpawn != null && (now - lastSpawn) < intervalMs) {
            return;
        }
        lastGravityWellSpawn.put(player.getUniqueId(), now);

        long durationMs = (long) talent.getValue(0);
        double radius = talent.getValue(1);
        double slowPercent = talent.getValue(2);
        double damagePerSecondPercent = talent.getValue(3);

        // Position: at player location
        Location target = player.getLocation().clone();
        String locKey = getLocationKey(target);

        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double dps = baseDamage * damagePerSecondPercent;
        int slowLevel = (int) Math.ceil(slowPercent * 4) - 1;

        GravityWellData data = new GravityWellData(
            player.getUniqueId(), target, now + durationMs,
            radius, dps, slowLevel
        );
        activeGravityWells.put(locKey, data);

        // Visual initial
        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.clone().add(0, 1, 0), 60, 1, 0.5, 1, 0.2);
        target.getWorld().playSound(target, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);

        sendActionBar(player, "§5+ Puits de Gravite! +");
    }

    /**
     * Singularity - Track kills for multi-kill trigger
     */
    private void trackKillForSingularity(Player player, Location killLocation) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SINGULARITY);
        long cooldownMs = (long) talent.getValue(4);
        if (!checkCooldown(player, "singularity", cooldownMs)) return;

        int requiredKills = (int) talent.getValue(5);
        long killWindowMs = (long) talent.getValue(6);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Get or create kill list
        List<Long> kills = recentKills.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());

        // Remove expired kills
        kills.removeIf(t -> (now - t) > killWindowMs);

        // Add this kill
        kills.add(now);

        // Check if we have enough kills for a singularity
        if (kills.size() >= requiredKills) {
            kills.clear(); // Reset kill counter
            spawnSingularityAtLocation(player, killLocation);
        }
    }

    /**
     * Singularity - Spawn at a specific location
     */
    private void spawnSingularityAtLocation(Player player, Location target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.SINGULARITY);

        double radius = talent.getValue(0);
        long durationMs = (long) talent.getValue(1);
        double initialDamagePercent = talent.getValue(2);
        double dpsPercent = talent.getValue(3);

        String locKey = getLocationKey(target);

        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double initialDamage = baseDamage * initialDamagePercent;
        double dps = baseDamage * dpsPercent;

        SingularityData data = new SingularityData(
            player.getUniqueId(), target, System.currentTimeMillis() + durationMs,
            radius, initialDamage, dps
        );
        activeSingularities.put(locKey, data);

        // Degats initiaux
        for (Entity entity : target.getWorld().getNearbyEntities(target, radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player) && !isPlayerMinion(entity, player)) {
                le.damage(initialDamage, player);
            }
        }

        // Visual spectaculaire
        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.clone().add(0, 1, 0), 100, 2, 2, 2, 0.5);
        target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target, 50, 1, 1, 1, 0.1);
        target.getWorld().playSound(target, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.8f);
        target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.3f);

        sendActionBar(player, "§5§l* SINGULARITE! *");
    }

    /**
     * Dimensional Rift - Passive: Banish enemies below HP threshold
     */
    private void processDimensionalRiftPassive(Player player, LivingEntity target) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.DIMENSIONAL_RIFT);

        // Check per-target cooldown
        UUID targetUuid = target.getUniqueId();
        Long cooldownExpiry = dimensionalRiftCooldowns.get(targetUuid);
        long now = System.currentTimeMillis();
        if (cooldownExpiry != null && now < cooldownExpiry) {
            return;
        }

        // Check if already banished
        if (banishedEntities.containsKey(targetUuid)) {
            return;
        }

        double hpThreshold = talent.getValue(0);
        long banishDurationMs = (long) talent.getValue(1);
        double exitDamagePercent = talent.getValue(2);
        long cooldownPerTargetMs = (long) talent.getValue(3);

        // Check if target is below HP threshold
        double hpPercent = target.getHealth() / target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (hpPercent > hpThreshold) {
            return;
        }

        // Set per-target cooldown
        dimensionalRiftCooldowns.put(targetUuid, now + cooldownPerTargetMs);

        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double exitDamage = baseDamage * exitDamagePercent;

        // Bannir l'entite
        Location originalLoc = target.getLocation().clone();
        BanishedData data = new BanishedData(
            player.getUniqueId(), originalLoc, now + banishDurationMs, exitDamage
        );
        banishedEntities.put(targetUuid, data);

        // Effets de bannissement
        target.setAI(false);
        target.setInvisible(true);
        target.setInvulnerable(true);
        target.teleport(originalLoc.clone().add(0, -100, 0));

        // Visual
        originalLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, originalLoc.add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.3);
        originalLoc.getWorld().spawnParticle(Particle.PORTAL, originalLoc, 30, 0.3, 0.5, 0.3, 0.5);
        originalLoc.getWorld().playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        sendActionBar(player, "§5* Banni dans le vide! *");
    }

    /**
     * Black Hole - Periodic automatic spawn
     */
    private void spawnBlackHolePeriodic(Player player) {
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.BLACK_HOLE);
        long intervalMs = (long) talent.getValue(4);

        // Check if enough time has passed since last spawn
        Long lastSpawn = lastBlackHoleSpawn.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastSpawn != null && (now - lastSpawn) < intervalMs) {
            return;
        }
        lastBlackHoleSpawn.put(player.getUniqueId(), now);

        double radius = talent.getValue(0);
        long durationMs = (long) talent.getValue(1);
        double initialDamagePercent = talent.getValue(2);
        double dpsPercent = talent.getValue(3);

        // Position: at player location
        Location target = player.getLocation().clone().add(0, 1, 0);
        String locKey = getLocationKey(target);

        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double initialDamage = baseDamage * initialDamagePercent;
        double dps = baseDamage * dpsPercent;

        BlackHoleData data = new BlackHoleData(
            player.getUniqueId(), target, now + durationMs,
            radius, initialDamage, dps
        );
        activeBlackHoles.put(locKey, data);

        // Degats initiaux massifs + Application des DOTs
        double baseDamagePlayer = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        for (Entity entity : target.getWorld().getNearbyEntities(target, radius, radius, radius)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player) && !isPlayerMinion(entity, player)) {
                le.damage(initialDamage, player);

                // Appliquer automatiquement les DOTs d'ombre aux ennemis dans le trou noir
                if (!shadowDots.containsKey(le.getUniqueId())) {
                    double shadowDps = baseDamagePlayer * 0.15; // 15% base damage/s comme Shadow Word
                    ShadowDotData dotData = new ShadowDotData(
                        player.getUniqueId(),
                        shadowDps,
                        now + 4000, // 4 secondes de DOT
                        now
                    );
                    shadowDots.put(le.getUniqueId(), dotData);

                    // Appliquer aussi Vampiric Touch si le joueur l'a
                    if (hasTalentEffect(player, Talent.TalentEffectType.VAMPIRIC_TOUCH)) {
                        applyVampiricTouch(player, le, baseDamagePlayer);
                    }
                }
            }
        }

        // Visual d'apparition ULTRA SPECTACULAIRE - Effet d'implosion
        // Phase 1: Expansion rapide
        target.getWorld().spawnParticle(Particle.END_ROD, target, 100, 0.1, 0.1, 0.1, 0.8);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target, 80, 0.5, 0.5, 0.5, 0.5);

        // Phase 2: Formation des anneaux
        for (int ring = 0; ring < 5; ring++) {
            double ringRadius = radius * (0.2 + ring * 0.2);
            for (int i = 0; i < 30; i++) {
                double angle = i * Math.PI * 2 / 30;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                Location ringLoc = target.clone().add(x, ring * 0.3, z);
                target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, ringLoc, 5, 0.1, 0.1, 0.1, 0.2);
            }
        }

        // Noyau sombre intense
        target.getWorld().spawnParticle(Particle.SQUID_INK, target, 60, 0.5, 0.5, 0.5, 0.02);
        target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target, 150, 2, 2, 2, 0.4);
        target.getWorld().spawnParticle(Particle.SMOKE, target, 100, 2.5, 2.5, 2.5, 0.3);
        target.getWorld().spawnParticle(Particle.ASH, target, 80, radius, 3, radius, 0.1);

        // Eclairs et distorsion
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target, 40, 1.5, 1.5, 1.5, 0.3);
        target.getWorld().spawnParticle(Particle.ENCHANT, target, 100, 2, 2, 2, 2.0);

        // Sons d'apparition dramatiques
        target.getWorld().playSound(target, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.3f);
        target.getWorld().playSound(target, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.2f);
        target.getWorld().playSound(target, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.4f);
        target.getWorld().playSound(target, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
        target.getWorld().playSound(target, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.3f);

        sendActionBar(player, "§0§l✦ TROU NOIR! ✦");
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

                    // Visual (reduit)
                    impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 12, 0.8, 0.4, 0.8, 0.08);
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

            // Visual (reduit)
            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 1, 0, 0, 0, 0);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation(), 35, radius/2, radius/2, radius/2, 0.15);
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

        // Visual (reduit)
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation(), 20, radius/2, radius/2, radius/2, 0.08);
        victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation(), 12, radius/2, radius/2, radius/2, 0.08,
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
            minion.setCustomName("§5☠ §cMort-Vivant §8[" + player.getName() + "]");
            minion.setCustomNameVisible(true);
            minion.setBaby(false);
            minion.getScoreboardTags().add("player_minion");
            minion.getScoreboardTags().add("owner_" + player.getUniqueId());
            minion.setPersistent(false);

            // Set stats based on victim and talent
            double statPercent = talent.getValue(1);
            // Check Immortal Army buff
            if (hasTalentEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY)) {
                Talent immortal = getTalentWithEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY);
                statPercent += immortal.getValue(1);
            }

            // Use victim's stats as base - buffed significantly
            double victimMaxHealth = victim.getMaxHealth();
            double baseHealth = Math.max(victimMaxHealth * statPercent, 50); // Minimum 50 HP
            minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            minion.setHealth(minion.getMaxHealth());

            // Give zombie more speed and attack damage - buffed
            minion.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.32);
            minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(Math.max(10 * statPercent, 8));
            minion.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(8);

            // Give iron armor and sword
            minion.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.IRON_HELMET));
            minion.getEquipment().setChestplate(new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE));
            minion.getEquipment().setLeggings(new org.bukkit.inventory.ItemStack(Material.IRON_LEGGINGS));
            minion.getEquipment().setBoots(new org.bukkit.inventory.ItemStack(Material.IRON_BOOTS));

            org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(Material.IRON_SWORD);
            sword.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 3);
            minion.getEquipment().setItemInMainHand(sword);

            // Prevent equipment drops
            minion.getEquipment().setHelmetDropChance(0f);
            minion.getEquipment().setChestplateDropChance(0f);
            minion.getEquipment().setLeggingsDropChance(0f);
            minion.getEquipment().setBootsDropChance(0f);
            minion.getEquipment().setItemInMainHandDropChance(0f);

            // Add glowing effect
            minion.setGlowing(true);

            // Add potion effects for power
            minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            minion.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            minion.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

            minions.add(minion.getUniqueId());

            // Set initial target
            setMinionTarget(player, minion);

            // Force immediate first attack after spawn to fix first attack bug
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (minion.isValid() && !minion.isDead() && minion.getTarget() != null) {
                    LivingEntity target = minion.getTarget();
                    if (target.isValid() && !target.isDead()) {
                        // Force attack by dealing direct damage
                        double damage = minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                        target.damage(damage, minion);
                    }
                }
            }, 5L); // 5 ticks = 0.25s delay for first attack

            // Schedule despawn - cap at 30 seconds max
            long duration = Math.min((long) talent.getValue(2), 30000L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (minion.isValid() && !minion.isDead()) {
                    // Visual de despawn
                    minion.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 15, 0.3, 0.5, 0.3, 0.05);
                    minion.getWorld().spawnParticle(Particle.SMOKE, minion.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                    minion.remove();
                    minions.remove(minion.getUniqueId());
                }
            }, duration / 50);

            // Enhanced visual
            victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation(), 30, 0.5, 1.5, 0.5, 0.08);
            victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation(), 15, 0.4, 0.5, 0.4, 0.03);
            victim.getWorld().spawnParticle(Particle.ENCHANT, victim.getLocation(), 20, 0.5, 1, 0.5, 0.5);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.4f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.2f, 1.5f);
            sendActionBar(player, "§5☠ §cMort-Vivant releve! §8(" + minions.size() + "/" + maxMinions + ")");
        }
    }

    private void processTimeStasis(Player player) {
        // Note: Cooldown is checked and set in onPlayerSneak() before calling this method
        Talent talent = getTalentWithEffect(player, Talent.TalentEffectType.TIME_STASIS);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

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

                // Visual on each enemy (reduit)
                le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.03);
            }
        }

        // Visual & sound (reduit)
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 40, 10, 8, 10, 0.01);
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
        if (!checkCooldown(player, "necromancer", 1500)) return;

        int orbs = getSoulOrbs(player);
        if (orbs <= 0) {
            sendActionBar(player, "§8Pas d'orbes d'ame disponibles!");
            return;
        }

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
        minion.setShouldBurnInDay(false);
        minion.setCustomName("§5☠ §dSquelette Archer §8[" + player.getName() + "]");
        minion.setCustomNameVisible(true);
        minion.getScoreboardTags().add("player_minion");
        minion.getScoreboardTags().add("owner_" + player.getUniqueId());
        minion.setPersistent(false);

        // Check Immortal Army buff for stats
        double statPercent = talent.getValue(0);
        if (hasTalentEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY)) {
            Talent immortal = getTalentWithEffect(player, Talent.TalentEffectType.IMMORTAL_ARMY);
            statPercent += immortal.getValue(1);
        }

        // Set stats based on player - buffed significantly
        double playerMaxHealth = player.getMaxHealth();
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(Math.max(playerMaxHealth * statPercent, 40));
        minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(Math.max(playerDamage * statPercent, 8));
        minion.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.28);
        minion.setHealth(minion.getMaxHealth());

        // Equip skeleton with enchanted bow and armor
        org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(Material.BOW);
        bow.addEnchantment(org.bukkit.enchantments.Enchantment.POWER, 3);
        bow.addEnchantment(org.bukkit.enchantments.Enchantment.FLAME, 1);
        minion.getEquipment().setItemInMainHand(bow);

        // Purple-tinted leather armor
        org.bukkit.inventory.meta.LeatherArmorMeta armorMeta;
        org.bukkit.Color purpleColor = org.bukkit.Color.fromRGB(128, 0, 128);

        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(Material.LEATHER_HELMET);
        armorMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
        armorMeta.setColor(purpleColor);
        helmet.setItemMeta(armorMeta);
        minion.getEquipment().setHelmet(helmet);

        org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(Material.LEATHER_CHESTPLATE);
        armorMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
        armorMeta.setColor(purpleColor);
        chestplate.setItemMeta(armorMeta);
        minion.getEquipment().setChestplate(chestplate);

        // Prevent equipment drops
        minion.getEquipment().setHelmetDropChance(0f);
        minion.getEquipment().setChestplateDropChance(0f);
        minion.getEquipment().setItemInMainHandDropChance(0f);

        // Add glowing effect
        minion.setGlowing(true);

        // Add potion effects for power
        minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        minion.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        minions.add(minion.getUniqueId());

        // Set initial target
        setMinionTarget(player, minion);

        // Force immediate first attack after spawn to fix first attack bug
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (minion.isValid() && !minion.isDead() && minion.getTarget() != null) {
                LivingEntity target = minion.getTarget();
                if (target.isValid() && !target.isDead()) {
                    // Force attack by dealing direct damage
                    double damage = minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                    target.damage(damage, minion);
                }
            }
        }, 5L); // 5 ticks = 0.25s delay for first attack

        // Schedule despawn - cap at 30 seconds max
        long duration = Math.min((long) talent.getValue(1), 30000L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (minion.isValid() && !minion.isDead()) {
                // Visual de despawn
                minion.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 15, 0.3, 0.5, 0.3, 0.05);
                minion.getWorld().spawnParticle(Particle.SMOKE, minion.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                minion.remove();
                minions.remove(minion.getUniqueId());
            }
        }, duration / 50);

        // Enhanced visual
        player.getWorld().spawnParticle(Particle.SOUL, minion.getLocation(), 25, 0.5, 1.5, 0.5, 0.08);
        player.getWorld().spawnParticle(Particle.ENCHANT, minion.getLocation(), 30, 0.5, 1, 0.5, 0.5);
        player.getWorld().playSound(minion.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.7f);
        player.getWorld().playSound(minion.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.5f);
        sendActionBar(player, "§5☠ §dSquelette Archer invoque! §8(" + minions.size() + "/" + maxMinions + ")");
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

            // Nova visual optimise (reduit)
            for (double r = 1; r <= radius; r += 3) {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
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

            // Sun visual above player (reduit)
            Location sunLoc = player.getLocation().add(0, 5, 0);
            player.getWorld().spawnParticle(Particle.FLAME, sunLoc, 12, 0.8, 0.8, 0.8, 0.04);
            player.getWorld().spawnParticle(Particle.LAVA, sunLoc, 2, 0.8, 0.8, 0.8, 0);
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

            // Winter visual ameliore - aura de froid (reduit)
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0),
                5, radius/2, 1, radius/2, 0.02);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0),
                1, radius/3, 0.3, radius/3, 0.01);
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
                    impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, 10, 1.5, 0.8, 1.5, 0.15);
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

                // Blizzard visual ameliore (reduit)
                frozen.getWorld().spawnParticle(Particle.SNOWFLAKE, frozen.getLocation().add(0, 1, 0),
                    4, auraRadius/2, 0.6, auraRadius/2, 0.02);
                frozen.getWorld().spawnParticle(Particle.CLOUD, frozen.getLocation().add(0, 0.5, 0),
                    1, auraRadius/3, 0.2, auraRadius/3, 0.01);
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

                // Visual ameliore - zone de givre au sol (reduit)
                world.spawnParticle(Particle.SNOWFLAKE, loc, 4, radius/2, 0.2, radius/2, 0.02);
                world.spawnParticle(Particle.BLOCK, loc, 2, radius/2, 0.1, radius/2, 0.01,
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

        // Visual spawn (reduit)
        location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 20, 1.2, 0.8, 1.2, 0.06);
        location.getWorld().spawnParticle(Particle.ITEM, location.add(0, 0.5, 0), 12, 0.8, 0.4, 0.8, 0.08,
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

            // Tick every second
            long tickInterval = 1000;

            if (now - dot.lastTick < tickInterval) continue;
            dot.lastTick = now;

            // Appliquer les dégâts
            double damage = dot.damagePerSecond;
            target.damage(damage, owner);

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

            // Tick every second
            long tickInterval = 1000;

            if (now - dot.lastTick < tickInterval) continue;
            dot.lastTick = now;

            // Appliquer les dégâts
            double damage = dot.damagePerSecond;
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
                        target.damage(damage, player);

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

    // ==================== GRAVITY EFFECTS PERIODIC PROCESSORS ====================

    /**
     * Traite tous les effets de gravite actifs (Gravity Wells, Singularities, Black Holes)
     */
    private void processGravityEffects() {
        long now = System.currentTimeMillis();

        // Process Gravity Wells
        Iterator<Map.Entry<String, GravityWellData>> wellIterator = activeGravityWells.entrySet().iterator();
        while (wellIterator.hasNext()) {
            Map.Entry<String, GravityWellData> entry = wellIterator.next();
            GravityWellData data = entry.getValue();

            if (now > data.expiry) {
                wellIterator.remove();
                continue;
            }

            Player owner = Bukkit.getPlayer(data.ownerUuid);
            if (owner == null) {
                wellIterator.remove();
                continue;
            }

            Location center = data.location;

            // Effet sur les ennemis proches
            for (Entity entity : center.getWorld().getNearbyEntities(center, data.radius, data.radius, data.radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    // Ralentissement
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, data.slowLevel, false, false));

                    // Attraction lente vers le centre
                    Vector direction = center.toVector().subtract(le.getLocation().toVector()).normalize();
                    le.setVelocity(le.getVelocity().add(direction.multiply(0.15)));

                    // Degats legers (par demi-seconde)
                    le.damage(data.dps * 0.5, owner);
                }
            }

            // Visual
            center.getWorld().spawnParticle(Particle.PORTAL, center, 15, data.radius/3, 0.5, data.radius/3, 0.05);
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 8, 0.5, 0.3, 0.5, 0.1);
        }

        // Process Singularities
        Iterator<Map.Entry<String, SingularityData>> singIterator = activeSingularities.entrySet().iterator();
        while (singIterator.hasNext()) {
            Map.Entry<String, SingularityData> entry = singIterator.next();
            SingularityData data = entry.getValue();

            if (now > data.expiry) {
                singIterator.remove();
                continue;
            }

            Player owner = Bukkit.getPlayer(data.ownerUuid);
            if (owner == null) {
                singIterator.remove();
                continue;
            }

            Location center = data.location;

            // Aspiration violente
            for (Entity entity : center.getWorld().getNearbyEntities(center, data.radius, data.radius, data.radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                    Vector direction = center.toVector().subtract(le.getLocation().toVector()).normalize();
                    double distance = le.getLocation().distance(center);
                    double pullStrength = Math.min(1.5, (data.radius - distance) / data.radius * 2.0);

                    le.setVelocity(direction.multiply(pullStrength).setY(0.1));
                    le.damage(data.dps * 0.5, owner);
                }
            }

            // Visual intense
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 40, 1, 1, 1, 0.3);
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 20, 0.5, 0.5, 0.5, 0.05);
        }

        // Process Black Holes
        Iterator<Map.Entry<String, BlackHoleData>> bhIterator = activeBlackHoles.entrySet().iterator();
        while (bhIterator.hasNext()) {
            Map.Entry<String, BlackHoleData> entry = bhIterator.next();
            BlackHoleData data = entry.getValue();

            if (now > data.expiry) {
                // Explosion finale SPECTACULAIRE
                Location center = data.location;
                center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 0, 0, 0, 0);
                center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 200, 3, 3, 3, 2.0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 100, 2, 2, 2, 0.5);
                center.getWorld().spawnParticle(Particle.END_ROD, center, 80, 4, 4, 4, 0.3);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                bhIterator.remove();
                continue;
            }

            Player owner = Bukkit.getPlayer(data.ownerUuid);
            if (owner == null) {
                bhIterator.remove();
                continue;
            }

            Location center = data.location;
            double baseDamage = owner.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();

            // Aspiration MASSIVE + Application des DOTs
            for (Entity entity : center.getWorld().getNearbyEntities(center, data.radius, data.radius, data.radius)) {
                if (entity instanceof LivingEntity le && !(entity instanceof Player) && !isPlayerMinion(entity, owner)) {
                    Vector direction = center.toVector().subtract(le.getLocation().toVector()).normalize();
                    double distance = le.getLocation().distance(center);
                    double pullStrength = Math.min(2.5, (data.radius - distance) / data.radius * 3.0);

                    le.setVelocity(direction.multiply(pullStrength).setY(0.2));
                    le.damage(data.dps * 0.5, owner);

                    // Appliquer automatiquement les DOTs d'ombre aux ennemis dans le trou noir
                    if (!shadowDots.containsKey(le.getUniqueId())) {
                        double shadowDps = baseDamage * 0.15; // 15% base damage/s comme Shadow Word
                        ShadowDotData dotData = new ShadowDotData(
                            owner.getUniqueId(),
                            shadowDps,
                            now + 4000, // 4 secondes de DOT
                            now
                        );
                        shadowDots.put(le.getUniqueId(), dotData);

                        // Appliquer aussi Vampiric Touch si le joueur l'a
                        if (hasTalentEffect(owner, Talent.TalentEffectType.VAMPIRIC_TOUCH)) {
                            applyVampiricTouch(owner, le, baseDamage);
                        }
                    }

                    // Visual AMELIORE sur l'entite aspiree - effet de distorsion
                    le.getWorld().spawnParticle(Particle.PORTAL, le.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.3);
                    le.getWorld().spawnParticle(Particle.REVERSE_PORTAL, le.getLocation().add(0, 0.5, 0), 8, 0.2, 0.3, 0.2, 0.2);
                    le.getWorld().spawnParticle(Particle.SOUL, le.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Visual ULTRA SPECTACULAIRE du trou noir avec rotation
            double time = (now % 2000) / 2000.0 * Math.PI * 2; // Rotation toutes les 2 secondes

            // Noyau central dense et sombre
            center.getWorld().spawnParticle(Particle.SQUID_INK, center, 30, 0.3, 0.3, 0.3, 0.01);
            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 20, 0.5, 0.5, 0.5, 0.05);

            // Anneaux de particules en rotation (disque d'accrétion)
            for (int ring = 0; ring < 3; ring++) {
                double ringRadius = data.radius * (0.3 + ring * 0.25);
                int particlesPerRing = 20 + ring * 10;
                for (int i = 0; i < particlesPerRing; i++) {
                    double angle = time + (i * Math.PI * 2 / particlesPerRing) + (ring * 0.5);
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    Location particleLoc = center.clone().add(x, 0.2 * ring, z);
                    center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }

            // Spirales aspirantes
            for (int spiral = 0; spiral < 4; spiral++) {
                double spiralAngle = time * 2 + (spiral * Math.PI / 2);
                for (double dist = 0.5; dist < data.radius; dist += 0.8) {
                    double x = Math.cos(spiralAngle + dist * 0.5) * dist;
                    double z = Math.sin(spiralAngle + dist * 0.5) * dist;
                    double y = Math.sin(dist) * 0.5;
                    Location spiralLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, spiralLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Colonnes de particules montantes
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 15, 0.8, 1.5, 0.8, 0.05);
            center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(0, 1, 0), 30, 1.5, 0.5, 1.5, 1.0);

            // Halo extérieur
            center.getWorld().spawnParticle(Particle.SMOKE, center, 60, data.radius * 0.8, 0.5, data.radius * 0.8, 0.02);
            center.getWorld().spawnParticle(Particle.ASH, center, 40, data.radius, 2, data.radius, 0.1);

            // Son ambient plus fréquent et varié
            if (Math.random() < 0.5) {
                center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.2f);
            }
            if (Math.random() < 0.2) {
                center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.3f);
            }
        }
    }

    /**
     * Traite le retour des entites bannies par Dimensional Rift
     */
    private void processBanishedEntities() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, BanishedData>> iterator = banishedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BanishedData> entry = iterator.next();
            BanishedData data = entry.getValue();

            if (now > data.expiry) {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity instanceof LivingEntity le) {
                    Player owner = Bukkit.getPlayer(data.ownerUuid);

                    // Restaurer l'entite
                    le.teleport(data.originalLocation);
                    le.setAI(true);
                    le.setInvisible(false);
                    le.setInvulnerable(false);

                    // Degats de sortie
                    if (owner != null) {
                        le.damage(data.exitDamage, owner);

                        // EXPLOSION DU VIDE - Inflige des degats aux mobs autour
                        Talent talent = getTalentWithEffect(owner, Talent.TalentEffectType.DIMENSIONAL_RIFT);
                        if (talent != null && talent.getValues().length > 5) {
                            double aoeDamagePercent = talent.getValue(4); // 1.0 = 100%
                            double aoeRadius = talent.getValue(5); // 4.0 blocs
                            double baseDamage = owner.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                            double aoeDamage = baseDamage * aoeDamagePercent;

                            Location explosionLoc = data.originalLocation.clone();

                            // Infliger des degats aux mobs proches (pas au mob banni lui-meme)
                            for (Entity nearby : explosionLoc.getWorld().getNearbyEntities(explosionLoc, aoeRadius, aoeRadius, aoeRadius)) {
                                if (nearby instanceof LivingEntity nearbyLe && !(nearby instanceof Player)
                                    && !nearby.getUniqueId().equals(le.getUniqueId())
                                    && !isPlayerMinion(nearby, owner)) {
                                    damageNoKnockback(nearbyLe, aoeDamage, owner);

                                    // Visual sur les mobs touches
                                    nearbyLe.getWorld().spawnParticle(Particle.PORTAL, nearbyLe.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.5);
                                }
                            }

                            // Visual SPECTACULAIRE de l'explosion du vide
                            // Onde de choc en expansion
                            for (double radius = 0.5; radius <= aoeRadius; radius += 0.8) {
                                for (int i = 0; i < 16; i++) {
                                    double angle = i * Math.PI * 2 / 16;
                                    double x = Math.cos(angle) * radius;
                                    double z = Math.sin(angle) * radius;
                                    Location particleLoc = explosionLoc.clone().add(x, 0.5, z);
                                    explosionLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 3, 0.1, 0.2, 0.1, 0.1);
                                }
                            }

                            // Noyau central de l'explosion
                            explosionLoc.getWorld().spawnParticle(Particle.SQUID_INK, explosionLoc.clone().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.05);
                            explosionLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, explosionLoc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                            explosionLoc.getWorld().spawnParticle(Particle.END_ROD, explosionLoc.clone().add(0, 1.5, 0), 25, 0.8, 0.8, 0.8, 0.15);
                            explosionLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, explosionLoc.clone().add(0, 0.5, 0), 50, 1.5, 0.5, 1.5, 0.08);

                            // Sons d'explosion du vide
                            explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 0.5f);
                            explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.4f);
                            explosionLoc.getWorld().playSound(explosionLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.5f);
                        }
                    }

                    // Visual de reapparition sur l'entite
                    Location loc = data.originalLocation.clone();
                    loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 1, 0), 60, 0.5, 1, 0.5, 0.4);
                    loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 35, 0.4, 0.6, 0.4, 0.08);
                    loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 40, 0.5, 1, 0.5, 1.0);
                }

                iterator.remove();
            }
        }
    }

    /**
     * Process periodic gravity effect spawns (Gravity Well + Black Hole)
     * Called in combat to auto-spawn gravity effects
     */
    private void processPeriodicGravitySpawns() {
        for (UUID uuid : activeOccultistes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            // Check if player is in combat (has nearby enemies)
            boolean inCombat = !player.getNearbyEntities(15, 15, 15).stream()
                .filter(e -> e instanceof LivingEntity && !(e instanceof Player))
                .toList().isEmpty();

            if (!inCombat) continue;

            // Gravity Well - auto-spawn every 15s
            if (hasTalentEffect(player, Talent.TalentEffectType.GRAVITY_WELL)) {
                spawnGravityWellPeriodic(player);
            }

            // Black Hole - auto-spawn every 45s
            if (hasTalentEffect(player, Talent.TalentEffectType.BLACK_HOLE)) {
                spawnBlackHolePeriodic(player);
            }
        }
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
     * Met a jour periodiquement les cibles et force les attaques des serviteurs
     * Appele toutes les secondes
     *
     * IMPORTANT: Les mobs ne s'attaquent pas naturellement entre eux dans Minecraft.
     * Cette methode force les attaques en infligeant des degats directement.
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
                boolean hasValidTarget = currentTarget != null && !currentTarget.isDead() &&
                    currentTarget.getLocation().distance(owner.getLocation()) < 25;

                if (!hasValidTarget) {
                    // Chercher une nouvelle cible
                    setMinionTarget(owner, minion);
                    currentTarget = minion.getTarget();
                }

                // Si aucune cible, faire suivre le joueur
                if (currentTarget == null) {
                    // Teleporter le minion s'il est trop loin
                    if (minion.getLocation().distance(owner.getLocation()) > 30) {
                        minion.teleport(owner.getLocation().add(
                            Math.random() * 3 - 1.5, 0, Math.random() * 3 - 1.5));
                    }
                    continue;
                }

                // FORCE ATTACK: Les mobs ne s'attaquent pas naturellement entre eux
                // On force l'attaque si le minion est a portee de sa cible
                double distanceToTarget = minion.getLocation().distance(currentTarget.getLocation());
                if (!currentTarget.isValid() || currentTarget.isDead()) continue;

                // Determiner la portee selon le type de minion
                boolean isRanged = minion instanceof AbstractSkeleton;
                double attackRange = isRanged ? 15.0 : 2.5;

                if (distanceToTarget <= attackRange) {
                    // Faire regarder la cible
                    minion.lookAt(currentTarget);

                    if (isRanged && minion instanceof AbstractSkeleton skeleton) {
                        // Squelette archer - tirer une fleche
                        forceMinionRangedAttack(skeleton, currentTarget, owner);
                    } else {
                        // Attaque de melee
                        forceMinionMeleeAttack(minion, currentTarget);
                    }
                }
            }
        }
    }

    /**
     * Force une attaque de melee d'un minion
     */
    private void forceMinionMeleeAttack(Mob minion, LivingEntity target) {
        // Animation d'attaque (bras qui balance)
        if (minion instanceof Zombie zombie) {
            zombie.swingMainHand();
        }

        // Infliger les degats directement (ceci declenche onMinionDealDamage)
        double baseDamage = 8.0;
        var damageAttr = minion.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            baseDamage = damageAttr.getValue();
        }

        // Les degats seront modifies par onMinionDealDamage pour utiliser les stats du joueur
        target.damage(baseDamage, minion);
    }

    /**
     * Force une attaque a distance d'un squelette archer
     */
    private void forceMinionRangedAttack(AbstractSkeleton skeleton, LivingEntity target, Player owner) {
        // Creer et lancer une fleche
        Location eyeLocation = skeleton.getEyeLocation();
        org.bukkit.util.Vector direction = target.getEyeLocation().subtract(eyeLocation).toVector().normalize();

        Arrow arrow = skeleton.getWorld().spawn(eyeLocation.add(direction), Arrow.class);
        arrow.setVelocity(direction.multiply(2.5));
        arrow.setShooter(skeleton);
        arrow.setDamage(8.0); // Degats de base - sera modifie par onMinionDealDamage

        // Marquer la fleche comme venant d'un minion pour le calcul des degats
        arrow.addScoreboardTag("minion_arrow");
        arrow.addScoreboardTag("owner_" + owner.getUniqueId());

        // Son de tir
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f);

        // Effet visuel de flamme si le squelette a Flame
        var mainHand = skeleton.getEquipment().getItemInMainHand();
        if (mainHand.containsEnchantment(org.bukkit.enchantments.Enchantment.FLAME)) {
            arrow.setFireTicks(100);
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

    /**
     * Generates a unique string key for a Location
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

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
        long lastTick;

        ShadowDotData(UUID ownerUuid, double damagePerSecond, long expiry, long lastTick) {
            this.ownerUuid = ownerUuid;
            this.damagePerSecond = damagePerSecond;
            this.expiry = expiry;
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

    // ==================== GRAVITY EFFECT DATA CLASSES ====================

    /**
     * Données pour un Gravity Well
     */
    private static class GravityWellData {
        final UUID ownerUuid;
        final Location location;
        final long expiry;
        final double radius;
        final double dps;
        final int slowLevel;

        GravityWellData(UUID ownerUuid, Location location, long expiry, double radius, double dps, int slowLevel) {
            this.ownerUuid = ownerUuid;
            this.location = location;
            this.expiry = expiry;
            this.radius = radius;
            this.dps = dps;
            this.slowLevel = slowLevel;
        }
    }

    /**
     * Données pour une Singularité
     */
    private static class SingularityData {
        final UUID ownerUuid;
        final Location location;
        final long expiry;
        final double radius;
        final double initialDamage;
        final double dps;

        SingularityData(UUID ownerUuid, Location location, long expiry, double radius, double initialDamage, double dps) {
            this.ownerUuid = ownerUuid;
            this.location = location;
            this.expiry = expiry;
            this.radius = radius;
            this.initialDamage = initialDamage;
            this.dps = dps;
        }
    }

    /**
     * Données pour un Trou Noir
     */
    private static class BlackHoleData {
        final UUID ownerUuid;
        final Location location;
        final long expiry;
        final double radius;
        final double initialDamage;
        final double dps;

        BlackHoleData(UUID ownerUuid, Location location, long expiry, double radius, double initialDamage, double dps) {
            this.ownerUuid = ownerUuid;
            this.location = location;
            this.expiry = expiry;
            this.radius = radius;
            this.initialDamage = initialDamage;
            this.dps = dps;
        }
    }

    /**
     * Données pour une entité bannie par Dimensional Rift
     */
    private static class BanishedData {
        final UUID ownerUuid;
        final Location originalLocation;
        final long expiry;
        final double exitDamage;

        BanishedData(UUID ownerUuid, Location originalLocation, long expiry, double exitDamage) {
            this.ownerUuid = ownerUuid;
            this.originalLocation = originalLocation;
            this.expiry = expiry;
            this.exitDamage = exitDamage;
        }
    }
}
