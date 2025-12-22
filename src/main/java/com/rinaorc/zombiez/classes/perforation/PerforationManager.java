package com.rinaorc.zombiez.classes.perforation;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de la Voie du GIVRE (TIR DE GIVRE) du Chasseur.
 * Système inspiré de l'Ice Shot Amazon de PoE2.
 *
 * Mécaniques principales:
 * - GIVRE (0-100%): Accumulation sur les ennemis
 * - 50% = RALENTI (-30% vitesse)
 * - 100% = GELÉ (immobilisé 2s, +50% dégâts reçus)
 * - ÉCLAT: Mort d'un gelé = explosion AoE + propagation givre
 * - ZÉRO ABSOLU: Ultimate de gel massif
 */
public class PerforationManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // ============ CONFIGURATION GIVRE ============
    private static final double FROST_SLOW_THRESHOLD = 50.0;      // 50% = ralenti
    private static final double FROST_FROZEN_THRESHOLD = 100.0;   // 100% = gelé
    private static final double FROST_SLOW_AMOUNT = 0.30;         // -30% vitesse
    private static final double FROZEN_DAMAGE_BONUS = 0.50;       // +50% dégâts sur gelés
    private static final long FROZEN_DURATION_MS = 2000;          // 2s de gel
    private static final double SHATTER_BASE_RADIUS = 4.0;        // Rayon d'éclat
    private static final double SHATTER_HP_DAMAGE_PERCENT = 0.15; // 15% HP max en dégâts
    private static final int SHATTER_MAX_CHAIN = 3;               // Max chaînes d'éclat
    private static final double FROST_DECAY_PER_SECOND = 5.0;     // -5% givre/s si pas touché

    // === GIVRE SUR ENNEMIS ===
    private final Map<UUID, Double> entityFrost = new ConcurrentHashMap<>();
    private final Map<UUID, Long> entityFrostLastUpdate = new ConcurrentHashMap<>();
    private final Map<UUID, Long> entityFrozenUntil = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> entityFrostSource = new ConcurrentHashMap<>(); // Qui a appliqué le givre

    // === CHARGE GLACIALE (remplace Calibre) ===
    private final Map<UUID, Integer> playerFrostCharge = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFrostChargeTime = new ConcurrentHashMap<>();

    // === HYPOTHERMIE (remplace Surchauffe) ===
    private final Map<UUID, Double> hypothermiaLevel = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHypothermiaTime = new ConcurrentHashMap<>();

    // === TEMPÊTE DE NEIGE (remplace Momentum) ===
    private final Map<UUID, Integer> shatterKillStreak = new ConcurrentHashMap<>();
    private final Map<UUID, Long> blizzardModeEnd = new ConcurrentHashMap<>();

    // === HIVER ÉTERNEL (remplace Dévastation) - PASSIF après X gels ===
    private final Map<UUID, Long> eternalWinterEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Long> eternalWinterCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> freezeCounter = new ConcurrentHashMap<>(); // Compteur de gels

    // === ZÉRO ABSOLU (remplace Jugement) - Activation directe ===
    private final Map<UUID, Long> absoluteZeroCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSneakTime = new ConcurrentHashMap<>();

    // === LIGNES DE GLACE (remplace Trajectoire Fatale) ===
    private final Map<UUID, List<IceLine>> iceLines = new ConcurrentHashMap<>();

    // === JOUEURS ACTIFS ===
    private final Set<UUID> activeFrostPlayers = ConcurrentHashMap.newKeySet();

    public PerforationManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        startTickTask();
        startFrostDecayTask();
    }

    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup hypothermie expirée
                hypothermiaLevel.entrySet().removeIf(entry -> {
                    Long lastTime = lastHypothermiaTime.get(entry.getKey());
                    return lastTime == null || now - lastTime > 3000; // Reset après 3s
                });

                // Cleanup blizzard expiré
                blizzardModeEnd.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            player.sendMessage("§b§l[GIVRE] §7Tempête de Neige §cterminée§7.");
                            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                        }
                        return true;
                    }
                    return false;
                });

                // Cleanup hiver éternel expiré
                eternalWinterEnd.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            player.sendMessage("§b§l[GIVRE] §7Hiver Éternel §cterminé§7.");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
                        }
                        return true;
                    }
                    return false;
                });

                // Cleanup entités gelées expirées
                entityFrozenUntil.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        // Réduire le givre de moitié après dégel
                        entityFrost.computeIfPresent(entry.getKey(), (k, v) -> v * 0.5);
                        return true;
                    }
                    return false;
                });

                // Cleanup lignes de glace expirées
                for (Map.Entry<UUID, List<IceLine>> entry : iceLines.entrySet()) {
                    entry.getValue().removeIf(line -> now > line.expiryTime);
                }

                // Enregistrer les providers ActionBar
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

    /**
     * Task de décroissance passive du givre
     */
    private void startFrostDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                entityFrost.entrySet().removeIf(entry -> {
                    UUID entityId = entry.getKey();
                    Long lastUpdate = entityFrostLastUpdate.get(entityId);

                    // Si pas touché depuis 2s, le givre décroît
                    if (lastUpdate != null && now - lastUpdate > 2000) {
                        double newFrost = entry.getValue() - FROST_DECAY_PER_SECOND;
                        if (newFrost <= 0) {
                            entityFrostLastUpdate.remove(entityId);
                            entityFrostSource.remove(entityId);
                            return true;
                        }
                        entry.setValue(newFrost);
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes
    }

    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isPerforationPlayer(player)) {
                if (!activeFrostPlayers.contains(uuid)) {
                    activeFrostPlayers.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                if (activeFrostPlayers.contains(uuid)) {
                    activeFrostPlayers.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    // ==================== SYSTÈME DE GIVRE ====================

    /**
     * Obtient le niveau de givre d'une entité (0-100%)
     */
    public double getEntityFrost(UUID entityId) {
        return entityFrost.getOrDefault(entityId, 0.0);
    }

    /**
     * Ajoute du givre à une entité
     * @return true si l'entité vient d'être gelée
     */
    public boolean addFrost(Player source, LivingEntity target, double amount) {
        UUID targetId = target.getUniqueId();
        UUID sourceId = source.getUniqueId();

        // En mode Hiver Éternel, +50% givre appliqué
        if (isEternalWinterActive(sourceId)) {
            amount *= 1.5;
        }

        // En mode Tempête de Neige, +30% givre
        if (isBlizzardActive(sourceId)) {
            amount *= 1.3;
        }

        double currentFrost = getEntityFrost(targetId);
        boolean wasNotFrozen = currentFrost < FROST_FROZEN_THRESHOLD;

        double newFrost = Math.min(currentFrost + amount, FROST_FROZEN_THRESHOLD);
        entityFrost.put(targetId, newFrost);
        entityFrostLastUpdate.put(targetId, System.currentTimeMillis());
        entityFrostSource.put(targetId, sourceId);

        // Effets visuels de givre
        spawnFrostParticles(target, newFrost);

        // Seuil 50% - RALENTI
        if (currentFrost < FROST_SLOW_THRESHOLD && newFrost >= FROST_SLOW_THRESHOLD) {
            applyFrostSlow(target);
            source.playSound(source.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, 1.2f);
        }

        // Seuil 100% - GELÉ
        if (wasNotFrozen && newFrost >= FROST_FROZEN_THRESHOLD) {
            freezeEntity(source, target);
            return true;
        }

        return false;
    }

    /**
     * Applique le ralentissement de givre
     */
    private void applyFrostSlow(LivingEntity target) {
        int slowLevel = 1; // Slowness II = -30%
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, 40, slowLevel, false, false, true
        ));
    }

    /**
     * Gèle complètement une entité
     */
    private void freezeEntity(Player source, LivingEntity target) {
        UUID targetId = target.getUniqueId();
        UUID sourceId = source.getUniqueId();
        long now = System.currentTimeMillis();

        entityFrozenUntil.put(targetId, now + FROZEN_DURATION_MS);

        // Immobilisation totale
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, (int)(FROZEN_DURATION_MS / 50), 255, false, false, true
        ));
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.JUMP_BOOST, (int)(FROZEN_DURATION_MS / 50), 128, false, false, true
        )); // Empêche les sauts

        // Marqueur visuel
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.GLOWING, (int)(FROZEN_DURATION_MS / 50), 0, false, false, true
        ));

        // Effets visuels de gel
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.BLOCK, loc, 30, 0.5, 0.8, 0.5, 0.1,
            Material.BLUE_ICE.createBlockData());
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.4, 0.6, 0.4, 0.05);
        target.getWorld().playSound(loc, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.5f);

        source.sendMessage("§b§l[GELÉ] §7Cible §bimmobilisée §72s! §a+50%§7 dégâts!");
        source.playSound(source.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.5f);

        // === HIVER ÉTERNEL - Compteur de gels ===
        Talent winterTalent = talentManager.getActiveTalentByEffect(source, Talent.TalentEffectType.DEVASTATION);
        if (winterTalent != null && !isEternalWinterActive(sourceId) && canActivateEternalWinter(sourceId)) {
            int freezesNeeded = (int) winterTalent.getValue(5); // 5 gels
            int currentFreezes = freezeCounter.getOrDefault(sourceId, 0) + 1;
            freezeCounter.put(sourceId, currentFreezes);

            if (currentFreezes >= freezesNeeded) {
                // Activer Hiver Éternel automatiquement!
                freezeCounter.put(sourceId, 0);
                activateEternalWinter(source);
            } else {
                // Feedback du compteur
                source.sendMessage("§b§l[HIVER] §7Gels: §b" + currentFreezes + "§7/§e" + freezesNeeded);
            }
        }
    }

    /**
     * Vérifie si une entité est gelée
     */
    public boolean isFrozen(UUID entityId) {
        Long frozenUntil = entityFrozenUntil.get(entityId);
        return frozenUntil != null && System.currentTimeMillis() < frozenUntil;
    }

    /**
     * Obtient le bonus de dégâts sur cible gelée
     */
    public double getFrozenDamageBonus(LivingEntity target) {
        if (isFrozen(target.getUniqueId())) {
            return FROZEN_DAMAGE_BONUS;
        }
        return 0;
    }

    /**
     * Spawn des particules de givre selon le niveau
     */
    private void spawnFrostParticles(LivingEntity target, double frostLevel) {
        Location loc = target.getLocation().add(0, 1, 0);
        int particleCount = (int)(frostLevel / 10);

        if (frostLevel >= FROST_FROZEN_THRESHOLD) {
            // Gelé = particules de glace denses
            target.getWorld().spawnParticle(Particle.BLOCK, loc, 5, 0.3, 0.5, 0.3, 0,
                Material.BLUE_ICE.createBlockData());
        } else if (frostLevel >= FROST_SLOW_THRESHOLD) {
            // Ralenti = flocons modérés
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, particleCount, 0.3, 0.4, 0.3, 0.02);
        } else if (frostLevel > 0) {
            // Début de givre = légers flocons
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, particleCount / 2, 0.2, 0.3, 0.2, 0.01);
        }
    }

    // ==================== ÉCLAT (SHATTER) ====================

    /**
     * Déclenche l'éclat quand une entité gelée meurt
     */
    public void triggerShatter(Player source, LivingEntity deadEntity, int chainDepth) {
        if (chainDepth > SHATTER_MAX_CHAIN) return;

        UUID deadId = deadEntity.getUniqueId();
        double frostLevel = getEntityFrost(deadId);

        // Nettoyer les données de l'entité morte
        entityFrost.remove(deadId);
        entityFrostLastUpdate.remove(deadId);
        entityFrozenUntil.remove(deadId);
        entityFrostSource.remove(deadId);

        // L'entité doit être gelée ou avoir au moins 70% de givre pour éclater
        if (frostLevel < 70) return;

        Location center = deadEntity.getLocation().add(0, 1, 0);
        double radius = SHATTER_BASE_RADIUS;

        // Talent Écho Glacial = rayon augmenté
        Talent chainTalent = talentManager.getActiveTalentByEffect(source, Talent.TalentEffectType.CHAIN_PERFORATION);
        if (chainTalent != null) {
            radius += chainTalent.getValue(1); // +2 blocs de rayon
        }

        // Calculer les dégâts (basés sur HP max de l'entité morte)
        double maxHealth = deadEntity.getAttribute(Attribute.MAX_HEALTH) != null ?
            deadEntity.getAttribute(Attribute.MAX_HEALTH).getValue() : 20;
        double shatterDamage = maxHealth * SHATTER_HP_DAMAGE_PERCENT;

        // Bonus en mode Hiver Éternel
        if (isEternalWinterActive(source.getUniqueId())) {
            shatterDamage *= 1.5;
        }

        // Cap à 500 dégâts pour éviter l'infini
        shatterDamage = Math.min(shatterDamage, 500);

        // Effets visuels d'éclat
        center.getWorld().spawnParticle(Particle.BLOCK, center, 50, radius / 2, radius / 2, radius / 2, 0.1,
            Material.BLUE_ICE.createBlockData());
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 40, radius / 2, radius / 2, radius / 2, 0.1);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.2f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.8f);

        // Propager givre et dégâts aux ennemis proches
        double frostToPropagate = frostLevel * 0.5; // 50% du givre se propage
        int targetsHit = 0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Monster target && !target.isDead() && target != deadEntity) {
                // Infliger dégâts d'éclat
                target.setMetadata("zombiez_talent_damage", new FixedMetadataValue(plugin, true));
                target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
                target.damage(shatterDamage, source);

                // Propager le givre
                boolean nowFrozen = addFrost(source, target, frostToPropagate);

                // Si la cible est maintenant gelée et meurt, déclencher éclat en chaîne
                if (nowFrozen && target.isDead()) {
                    // Délai pour éviter la récursion infinie
                    final LivingEntity finalTarget = target;
                    final int nextChain = chainDepth + 1;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            triggerShatter(source, finalTarget, nextChain);
                        }
                    }.runTaskLater(plugin, 2L);
                }

                targetsHit++;
            }
        }

        // Compteur de kills pour Tempête de Neige
        if (targetsHit > 0) {
            onShatterKill(source);
        }

        // Message
        if (chainDepth == 0) {
            source.sendMessage("§b§l[ÉCLAT] §7Explosion de glace! §c" +
                String.format("%.0f", shatterDamage) + " §7dégâts, §b" +
                String.format("%.0f", frostToPropagate) + "%§7 givre propagé!");
        }
    }

    // ==================== CHARGE GLACIALE (remplace CALIBRE) ====================

    public int getFrostCharge(UUID uuid) {
        return playerFrostCharge.getOrDefault(uuid, 0);
    }

    public void addFrostCharge(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = getFrostCharge(uuid);
        int maxCharge = 5;

        Talent chargeTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CALIBER);
        if (chargeTalent != null) {
            maxCharge = (int) chargeTalent.getValue(0);
        }

        // En mode Hiver Éternel, charges 2x plus vite
        if (isEternalWinterActive(uuid)) {
            amount *= 2;
        }

        int newCharge = Math.min(current + amount, maxCharge);
        playerFrostCharge.put(uuid, newCharge);
        lastFrostChargeTime.put(uuid, System.currentTimeMillis());

        // Feedback
        if (newCharge > current) {
            player.playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.8f + (newCharge * 0.15f));
        }

        // Charge max = TIR GLACIAL
        if (newCharge >= maxCharge && current < maxCharge) {
            player.sendMessage("§b§l[GIVRE] §eCharge MAX! §7Prochain tir = §b§lTIR GLACIAL!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.5f);
        }
    }

    public boolean isGlacialShot(UUID uuid) {
        return getFrostCharge(uuid) >= 5;
    }

    public void consumeFrostCharge(UUID uuid) {
        playerFrostCharge.put(uuid, 0);
    }

    /**
     * Obtient le bonus de givre basé sur la charge
     */
    public double getFrostChargeBonus(Player player) {
        Talent chargeTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CALIBER);
        if (chargeTalent == null) return 0;

        int charge = getFrostCharge(player.getUniqueId());
        double bonusPerLevel = chargeTalent.getValue(1); // +5% givre par niveau

        if (isGlacialShot(player.getUniqueId())) {
            // Tir Glacial = gel instantané (+100% givre)
            return 1.0;
        }

        return charge * bonusPerLevel;
    }

    // ==================== HYPOTHERMIE (remplace SURCHAUFFE) ====================

    public double getHypothermiaLevel(UUID uuid) {
        return hypothermiaLevel.getOrDefault(uuid, 0.0);
    }

    public void addHypothermia(Player player) {
        UUID uuid = player.getUniqueId();
        Talent hypoTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.OVERHEAT);
        if (hypoTalent == null) return;

        double stackPercent = hypoTalent.getValue(0); // 0.10 = 10%
        double maxPercent = hypoTalent.getValue(1); // 1.0 = 100%

        double current = getHypothermiaLevel(uuid);
        double newLevel = Math.min(current + stackPercent, maxPercent);
        hypothermiaLevel.put(uuid, newLevel);
        lastHypothermiaTime.put(uuid, System.currentTimeMillis());

        // Feedback
        if (newLevel > current) {
            float pitch = 0.8f + (float) (newLevel * 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.4f, pitch);
        }

        // Max = VAGUE DE FROID
        if (newLevel >= maxPercent && current < maxPercent) {
            player.sendMessage("§b§l[HYPOTHERMIE] §9100%! §7Prochain tir = §b§lVAGUE DE FROID!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
        }
    }

    public boolean isHypothermiaMax(UUID uuid) {
        return getHypothermiaLevel(uuid) >= 1.0;
    }

    public void resetHypothermia(UUID uuid) {
        hypothermiaLevel.put(uuid, 0.0);
    }

    /**
     * Déclenche une vague de froid AoE
     */
    public void triggerColdWave(Player player, Location center) {
        Talent hypoTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.OVERHEAT);
        if (hypoTalent == null) return;

        double radius = hypoTalent.getValue(3); // 4.0 blocs
        double frostAmount = 30 + (getHypothermiaLevel(player.getUniqueId()) * 40); // 30-70% givre

        // Effets visuels
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 60, radius / 2, radius / 2, radius / 2, 0.15);
        center.getWorld().spawnParticle(Particle.BLOCK, center, 30, radius / 2, 0.5, radius / 2, 0.05,
            Material.BLUE_ICE.createBlockData());
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);

        int frozen = 0;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Monster target) {
                if (addFrost(player, target, frostAmount)) {
                    frozen++;
                }
            }
        }

        player.sendMessage("§b§l[VAGUE DE FROID] §7" + frozen + " ennemis §bgelés§7! " +
            String.format("%.0f", frostAmount) + "% givre appliqué!");
        resetHypothermia(player.getUniqueId());
    }

    // ==================== TEMPÊTE DE NEIGE (remplace MOMENTUM) ====================

    public void onShatterKill(Player player) {
        Talent blizzardTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.HUNTER_MOMENTUM);
        if (blizzardTalent == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        int killsNeeded = (int) blizzardTalent.getValue(3); // 3 kills
        long duration = (long) blizzardTalent.getValue(6); // 4000ms

        int kills = shatterKillStreak.getOrDefault(uuid, 0) + 1;
        shatterKillStreak.put(uuid, kills);

        // Activer Tempête de Neige à X kills
        if (kills >= killsNeeded && !isBlizzardActive(uuid)) {
            activateBlizzard(player, duration);
            shatterKillStreak.put(uuid, 0);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 0.6f, 1.2f);
    }

    public boolean isBlizzardActive(UUID uuid) {
        return blizzardModeEnd.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    private void activateBlizzard(Player player, long duration) {
        UUID uuid = player.getUniqueId();

        blizzardModeEnd.put(uuid, System.currentTimeMillis() + duration);

        int durationTicks = (int) (duration / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, false, false));

        player.sendMessage("§b§l[TEMPÊTE DE NEIGE] §a+30%§7 givre appliqué! §9+Vitesse§7!");
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2.0f);

        // Effet visuel périodique
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isBlizzardActive(uuid)) {
                    cancel();
                    return;
                }
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    // ==================== HIVER ÉTERNEL (remplace DÉVASTATION) ====================

    public boolean isEternalWinterActive(UUID uuid) {
        return eternalWinterEnd.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public boolean canActivateEternalWinter(UUID uuid) {
        return eternalWinterCooldown.getOrDefault(uuid, 0L) < System.currentTimeMillis();
    }

    public void activateEternalWinter(Player player) {
        Talent winterTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.DEVASTATION);
        if (winterTalent == null) return;

        UUID uuid = player.getUniqueId();
        if (!canActivateEternalWinter(uuid)) {
            long remaining = (eternalWinterCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l[COOLDOWN] §7Hiver Éternel disponible dans §e" + remaining + "s§7.");
            return;
        }

        long duration = (long) winterTalent.getValue(0); // 8000ms
        long cooldown = (long) winterTalent.getValue(4); // 30000ms

        eternalWinterEnd.put(uuid, System.currentTimeMillis() + duration);
        eternalWinterCooldown.put(uuid, System.currentTimeMillis() + cooldown);

        // Effets
        int durationTicks = (int) (duration / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, false, false));

        player.sendMessage("§b§l[HIVER ÉTERNEL] §9Givre AMPLIFIÉ! §a+50%§7 givre! §c+50%§7 dégâts d'éclat!");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);

        // Aura de givre autour du joueur
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEternalWinterActive(uuid)) {
                    cancel();
                    return;
                }
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = p.getLocation();
                // Particules autour du joueur
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * 2;
                    double z = Math.sin(rad) * 2;
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(x, 0.5, z), 2, 0, 0.2, 0, 0.01);
                }

                // Givre passif aux ennemis proches
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 3, 5)) {
                    if (entity instanceof Monster target) {
                        addFrost(p, target, 5); // +5% givre/tick
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 10L);
    }

    // ==================== ZÉRO ABSOLU (remplace JUGEMENT) - ACTIVATION DIRECTE ====================

    public void handleSneakForAbsoluteZero(Player player) {
        Talent zeroTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.JUDGMENT);
        if (zeroTalent == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Vérifier cooldown
        long cooldown = (long) zeroTalent.getValue(3); // 45000ms
        if (absoluteZeroCooldown.getOrDefault(uuid, 0L) > now) {
            long remaining = (absoluteZeroCooldown.get(uuid) - now) / 1000;
            player.sendMessage("§c§l[COOLDOWN] §7Zéro Absolu disponible dans §e" + remaining + "s§7.");
            return;
        }

        // Double sneak detection
        long lastSneak = lastSneakTime.getOrDefault(uuid, 0L);
        lastSneakTime.put(uuid, now);

        if (now - lastSneak < 500) {
            // Activation directe - pas de charge ni d'immobilité!
            fireAbsoluteZero(player);
        }
    }

    private void fireAbsoluteZero(Player player) {
        Talent zeroTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.JUDGMENT);
        if (zeroTalent == null) return;

        UUID uuid = player.getUniqueId();
        double radius = zeroTalent.getValue(0); // 12.0 blocs
        double damageMult = zeroTalent.getValue(1); // 5.0 = 500%
        long freezeDuration = (long) zeroTalent.getValue(2); // 2000ms
        long cooldown = (long) zeroTalent.getValue(3); // 45000ms

        Location center = player.getLocation();

        player.sendMessage("§b§l★ ZÉRO ABSOLU ★");
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 2.0f);

        // Effet visuel massif - vague de glace qui s'étend
        for (double r = 0; r < radius; r += 1.5) {
            final double currentRadius = r;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int angle = 0; angle < 360; angle += 20) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;
                        Location point = center.clone().add(x, 0.3, z);
                        point.getWorld().spawnParticle(Particle.BLOCK, point, 2, 0.15, 0.1, 0.15, 0,
                            Material.BLUE_ICE.createBlockData());
                        point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 3, 0.1, 0.2, 0.1, 0.02);
                    }
                }
            }.runTaskLater(plugin, (long)(r / 3));
        }

        // Calculer dégâts
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double damage = baseDamage * damageMult;

        // Geler TOUS les ennemis dans le rayon
        int frozen = 0;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Monster target && !target.isDead()) {
                // Dégâts
                target.setMetadata("zombiez_talent_damage", new FixedMetadataValue(plugin, true));
                target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
                target.damage(Math.min(damage, 500), player); // Cap 500 dégâts

                // Gel instantané à 100%
                entityFrost.put(target.getUniqueId(), FROST_FROZEN_THRESHOLD);
                entityFrozenUntil.put(target.getUniqueId(), System.currentTimeMillis() + freezeDuration);

                // Effets de gel
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(freezeDuration / 50), 255, false, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int)(freezeDuration / 50), 0, false, false, true));

                frozen++;
            }
        }

        // Son final épique
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 2.0f);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.5f);

        player.sendMessage("§b§l[ZÉRO ABSOLU] §7" + frozen + " ennemis §b§lGELÉS§7!");

        // Cooldown
        absoluteZeroCooldown.put(uuid, System.currentTimeMillis() + cooldown);
    }

    // ==================== LIGNES DE GLACE (remplace TRAJECTOIRE FATALE) ====================

    public void createIceLine(Player player, Location start, Location end) {
        Talent iceTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.FATAL_TRAJECTORY);
        if (iceTalent == null) return;

        long duration = (long) iceTalent.getValue(2); // 3000ms
        double frostBonus = iceTalent.getValue(3); // 0.30 = +30% givre

        UUID uuid = player.getUniqueId();
        List<IceLine> lines = iceLines.computeIfAbsent(uuid, k -> new ArrayList<>());

        IceLine line = new IceLine(start.clone(), end.clone(), System.currentTimeMillis() + duration, frostBonus);
        lines.add(line);

        player.sendMessage("§b§l[LIGNE DE GLACE] §7Zone créée! §a+30%§7 givre dans la zone!");
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7f, 2.0f);

        // Effet visuel
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > duration / 50 || !lines.contains(line)) {
                    cancel();
                    return;
                }

                Vector dir = end.toVector().subtract(start.toVector()).normalize();
                double distance = start.distance(end);
                for (double d = 0; d < distance; d += 2) {
                    Location point = start.clone().add(dir.clone().multiply(d));
                    point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 2, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public double getIceLineFrostBonus(Player player, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        List<IceLine> lines = iceLines.get(uuid);
        if (lines == null || lines.isEmpty()) return 0;

        Location targetLoc = target.getLocation();

        for (IceLine line : lines) {
            if (isInLine(targetLoc, line.start, line.end, 2.0)) {
                return line.frostBonus;
            }
        }
        return 0;
    }

    private boolean isInLine(Location point, Location lineStart, Location lineEnd, double tolerance) {
        Vector line = lineEnd.toVector().subtract(lineStart.toVector());
        Vector toPoint = point.toVector().subtract(lineStart.toVector());

        double lineLength = line.length();
        if (lineLength == 0) return false;

        double projection = toPoint.dot(line.normalize());

        if (projection < 0 || projection > lineLength) return false;

        Location closestPoint = lineStart.clone().add(line.normalize().multiply(projection));
        return closestPoint.distance(point) <= tolerance;
    }

    // ==================== ÉCHO GLACIAL (remplace CHAIN PERFORATION) ====================

    /**
     * Propagation de givre en chaîne après un tir perçant
     */
    public void triggerFrostEcho(Player player, LivingEntity lastTarget, double baseFrost, int echoIndex) {
        Talent echoTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CHAIN_PERFORATION);
        if (echoTalent == null) return;

        int maxEchoes = (int) echoTalent.getValue(0); // 3
        double range = echoTalent.getValue(1); // 10.0 blocs

        if (echoIndex >= maxEchoes) return;

        // Trouver prochaine cible
        LivingEntity nextTarget = null;
        double closestDistance = range;

        for (Entity entity : lastTarget.getNearbyEntities(range, range, range)) {
            if (entity instanceof Monster target && target != lastTarget && !target.isDead()) {
                double distance = target.getLocation().distance(lastTarget.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nextTarget = target;
                }
            }
        }

        if (nextTarget == null) return;

        // Calculer givre de l'écho (décroissant)
        double frostPercent = echoTalent.getValue(2 + echoIndex); // 0.75, 0.50, 0.25
        double echoFrost = baseFrost * frostPercent;

        // Effet visuel
        Location from = lastTarget.getLocation().add(0, 1, 0);
        Location to = nextTarget.getLocation().add(0, 1, 0);
        Vector dir = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 1, 0, 0, 0, 0);
        }

        // Appliquer givre
        LivingEntity finalNextTarget = nextTarget;
        new BukkitRunnable() {
            @Override
            public void run() {
                addFrost(player, finalNextTarget, echoFrost);
                finalNextTarget.getWorld().playSound(finalNextTarget.getLocation(), Sound.BLOCK_POWDER_SNOW_HIT, 0.8f, 1.2f);

                // Écho suivant
                triggerFrostEcho(player, finalNextTarget, baseFrost, echoIndex + 1);
            }
        }.runTaskLater(plugin, (long) (distance / 2));

        if (echoIndex == 0) {
            player.sendMessage("§b§l[ÉCHO] §7Le givre se propage!");
        }
    }

    // ==================== ACTIONBAR ====================

    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        StringBuilder sb = new StringBuilder();

        // Charge Glaciale
        int charge = getFrostCharge(uuid);
        if (charge > 0 || hasTalent(player, Talent.TalentEffectType.CALIBER)) {
            sb.append("§bCharge: ");
            for (int i = 0; i < 5; i++) {
                sb.append(i < charge ? "§b❄" : "§8○");
            }
            sb.append(" ");
        }

        // Hypothermie
        double hypo = getHypothermiaLevel(uuid);
        if (hypo > 0 || hasTalent(player, Talent.TalentEffectType.OVERHEAT)) {
            int bars = (int) (hypo * 10);
            sb.append("§9Hypo: §b");
            for (int i = 0; i < 10; i++) {
                sb.append(i < bars ? "█" : "§8░");
            }
            sb.append("§9 ").append(String.format("%.0f", hypo * 100)).append("%");
            sb.append(" ");
        }

        // Hiver Éternel - Mode actif ou compteur
        if (isEternalWinterActive(uuid)) {
            long remaining = (eternalWinterEnd.get(uuid) - System.currentTimeMillis()) / 1000;
            sb.append("§b§lHIVER §e").append(remaining).append("s ");
        } else if (hasTalent(player, Talent.TalentEffectType.DEVASTATION)) {
            int freezes = freezeCounter.getOrDefault(uuid, 0);
            if (freezes > 0) {
                sb.append("§bGels: §e").append(freezes).append("§7/5 ");
            }
        }

        // Tempête de Neige
        if (isBlizzardActive(uuid)) {
            long remaining = (blizzardModeEnd.get(uuid) - System.currentTimeMillis()) / 1000;
            sb.append("§9§lTEMPÊTE §e").append(remaining).append("s ");
        }

        return sb.toString().trim();
    }

    // ==================== UTILITAIRES ====================

    public boolean isPerforationPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return false;

        String branchId = data.getSelectedBranchId();
        // Accepte "perforation" ou "givre" pour compatibilité
        return branchId != null && (branchId.toLowerCase().contains("perforation") || branchId.toLowerCase().contains("givre"));
    }

    public boolean hasTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType) != null;
    }

    public void registerPlayer(UUID uuid) {
        activeFrostPlayers.add(uuid);
    }

    public void cleanupPlayer(UUID uuid) {
        playerFrostCharge.remove(uuid);
        lastFrostChargeTime.remove(uuid);
        hypothermiaLevel.remove(uuid);
        lastHypothermiaTime.remove(uuid);
        shatterKillStreak.remove(uuid);
        blizzardModeEnd.remove(uuid);
        eternalWinterEnd.remove(uuid);
        eternalWinterCooldown.remove(uuid);
        freezeCounter.remove(uuid);
        absoluteZeroCooldown.remove(uuid);
        lastSneakTime.remove(uuid);
        iceLines.remove(uuid);
        activeFrostPlayers.remove(uuid);

        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(uuid);
        }
    }

    /**
     * Nettoie les données d'une entité morte
     */
    public void cleanupEntity(UUID entityId) {
        entityFrost.remove(entityId);
        entityFrostLastUpdate.remove(entityId);
        entityFrozenUntil.remove(entityId);
        entityFrostSource.remove(entityId);
    }

    /**
     * Obtient le joueur source du givre sur une entité
     */
    public Player getFrostSource(UUID entityId) {
        UUID sourceId = entityFrostSource.get(entityId);
        return sourceId != null ? Bukkit.getPlayer(sourceId) : null;
    }

    // ==================== CLASSES INTERNES ====================

    private static class IceLine {
        final Location start;
        final Location end;
        final long expiryTime;
        final double frostBonus;

        IceLine(Location start, Location end, long expiryTime, double frostBonus) {
            this.start = start;
            this.end = end;
            this.expiryTime = expiryTime;
            this.frostBonus = frostBonus;
        }
    }
}
