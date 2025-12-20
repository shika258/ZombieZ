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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de la Voie de la Perforation du Chasseur.
 * Gère le système de Calibre, Surchauffe, Perforation et effets associés.
 */
public class PerforationManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // === CALIBRE ===
    private final Map<UUID, Integer> playerCaliber = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShotTime = new ConcurrentHashMap<>();

    // === SURCHAUFFE ===
    private final Map<UUID, Double> overheatLevel = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastOverheatTime = new ConcurrentHashMap<>();

    // === RÉDUCTION D'ARMURE ===
    private final Map<UUID, Map<UUID, ArmorReductionData>> armorReduction = new ConcurrentHashMap<>();

    // === MOMENTUM ===
    private final Map<UUID, Integer> consecutiveKills = new ConcurrentHashMap<>();
    private final Map<UUID, Long> momentumEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Long> frenzyEnd = new ConcurrentHashMap<>();

    // === DÉVASTATION ===
    private final Map<UUID, Long> devastationEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Long> devastationCooldown = new ConcurrentHashMap<>();

    // === JUGEMENT ===
    private final Map<UUID, Long> judgmentCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSneakTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> chargingJudgment = new ConcurrentHashMap<>();
    private final Map<UUID, Long> judgmentChargeStart = new ConcurrentHashMap<>();

    // === LIGNES DE MORT (Trajectoire Fatale) ===
    private final Map<UUID, List<FatalLine>> fatalLines = new ConcurrentHashMap<>();

    // === JOUEURS ACTIFS ===
    private final Set<UUID> activePerforationPlayers = ConcurrentHashMap.newKeySet();

    public PerforationManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        startTickTask();
    }

    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup surchauffe expirée
                overheatLevel.entrySet().removeIf(entry -> {
                    Long lastTime = lastOverheatTime.get(entry.getKey());
                    if (lastTime == null || now - lastTime > 2500) {
                        return true; // Reset après 2.5s sans tir
                    }
                    return false;
                });

                // Cleanup momentum expiré
                momentumEnd.entrySet().removeIf(entry -> now > entry.getValue());
                frenzyEnd.entrySet().removeIf(entry -> now > entry.getValue());

                // Cleanup kills consécutifs si pas de momentum
                consecutiveKills.entrySet().removeIf(entry ->
                    !momentumEnd.containsKey(entry.getKey()) && !frenzyEnd.containsKey(entry.getKey()));

                // Cleanup dévastation expirée
                devastationEnd.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            player.sendMessage("§a§l[PERFORATION] §7Mode Dévastation §cterminé§7.");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
                        }
                        return true;
                    }
                    return false;
                });

                // Cleanup réduction d'armure expirée
                for (Map.Entry<UUID, Map<UUID, ArmorReductionData>> entry : armorReduction.entrySet()) {
                    entry.getValue().entrySet().removeIf(e -> now > e.getValue().expiryTime);
                }

                // Cleanup lignes de mort expirées
                for (Map.Entry<UUID, List<FatalLine>> entry : fatalLines.entrySet()) {
                    entry.getValue().removeIf(line -> now > line.expiryTime);
                }

                // Enregistrer les providers ActionBar pour les joueurs Perforation
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

    /**
     * Enregistre les providers d'ActionBar pour les joueurs Perforation auprès du ActionBarManager
     */
    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isPerforationPlayer(player)) {
                // Enregistrer le provider si pas déjà fait
                if (!activePerforationPlayers.contains(uuid)) {
                    activePerforationPlayers.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                // Retirer le provider si le joueur n'est plus Perforation
                if (activePerforationPlayers.contains(uuid)) {
                    activePerforationPlayers.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    // ==================== CALIBRE ====================

    public int getCaliber(UUID uuid) {
        return playerCaliber.getOrDefault(uuid, 0);
    }

    public void addCaliber(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = getCaliber(uuid);
        int maxCaliber = 5;

        Talent caliberTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CALIBER);
        if (caliberTalent != null) {
            maxCaliber = (int) caliberTalent.getValue(0);
        }

        // En mode Dévastation, le calibre monte 2x plus vite
        if (isDevastationActive(uuid)) {
            amount *= 2;
        }

        int newCaliber = Math.min(current + amount, maxCaliber);
        playerCaliber.put(uuid, newCaliber);
        lastShotTime.put(uuid, System.currentTimeMillis());

        // Feedback visuel
        if (newCaliber > current) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 0.8f + (newCaliber * 0.15f));
        }

        // Calibre max atteint
        if (newCaliber >= maxCaliber && current < maxCaliber) {
            player.sendMessage("§a§l[PERFORATION] §eCalibre MAX! §7Prochain tir = §c§lTIR LOURD!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.5f);
        }
    }

    public boolean isHeavyShot(UUID uuid) {
        return getCaliber(uuid) >= 5;
    }

    public void consumeCaliber(UUID uuid) {
        playerCaliber.put(uuid, 0);
    }

    public double getCaliberDamageBonus(Player player) {
        Talent caliberTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CALIBER);
        if (caliberTalent == null) return 0;

        int caliber = getCaliber(player.getUniqueId());
        double bonusPerLevel = caliberTalent.getValue(1); // 0.05 = 5%

        if (isHeavyShot(player.getUniqueId())) {
            double heavyShotBonus = caliberTalent.getValue(2); // 1.0 = 100%

            // En mode Dévastation, Tirs Lourds = +150%
            if (isDevastationActive(player.getUniqueId())) {
                Talent devastation = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.DEVASTATION);
                if (devastation != null) {
                    heavyShotBonus = devastation.getValue(3); // 1.50 = 150%
                }
            }

            return heavyShotBonus;
        }

        return caliber * bonusPerLevel;
    }

    // ==================== SURCHAUFFE ====================

    public double getOverheatLevel(UUID uuid) {
        return overheatLevel.getOrDefault(uuid, 0.0);
    }

    public void addOverheat(Player player) {
        UUID uuid = player.getUniqueId();
        Talent overheatTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.OVERHEAT);
        if (overheatTalent == null) return;

        double stackPercent = overheatTalent.getValue(0); // 0.10 = 10%
        double maxPercent = overheatTalent.getValue(1); // 1.0 = 100%

        double current = getOverheatLevel(uuid);
        double newLevel = Math.min(current + stackPercent, maxPercent);
        overheatLevel.put(uuid, newLevel);
        lastOverheatTime.put(uuid, System.currentTimeMillis());

        // Feedback
        if (newLevel > current) {
            float pitch = 0.8f + (float) (newLevel * 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.4f, pitch);
        }

        // Max atteint
        if (newLevel >= maxPercent && current < maxPercent) {
            player.sendMessage("§6§l[SURCHAUFFE] §c100%! §7Prochain tir = §6§lEXPLOSION!");
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
        }
    }

    public boolean isOverheatMax(UUID uuid) {
        return getOverheatLevel(uuid) >= 1.0;
    }

    public void resetOverheat(UUID uuid) {
        overheatLevel.put(uuid, 0.0);
    }

    public void triggerOverheatExplosion(Player player, Location center) {
        Talent overheatTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.OVERHEAT);
        if (overheatTalent == null) return;

        double radius = overheatTalent.getValue(3); // 4.0 blocs
        double explosionBonus = overheatTalent.getValue(4); // 0.50 = 50%
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double explosionDamage = baseDamage * (1 + explosionBonus + getOverheatLevel(player.getUniqueId()));

        // Effets visuels
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().spawnParticle(Particle.FLAME, center, 30, radius / 2, radius / 2, radius / 2, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Dégâts AoE
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Monster target) {
                target.damage(explosionDamage, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); // 2s slow
            }
        }

        player.sendMessage("§6§l[SURCHAUFFE] §c§lEXPLOSION! §7" + String.format("%.0f", explosionDamage) + " dégâts!");
        resetOverheat(player.getUniqueId());
    }

    // ==================== RÉDUCTION D'ARMURE ====================

    public void addArmorReduction(Player player, LivingEntity target, int pierceCount) {
        Talent perfTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.ABSOLUTE_PERFORATION);
        if (perfTalent == null) return;

        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        double reductionPerPierce = perfTalent.getValue(0); // 0.20 = 20%
        double maxReduction = perfTalent.getValue(1); // 0.80 = 80%
        long duration = (long) perfTalent.getValue(2); // 5000ms

        Map<UUID, ArmorReductionData> playerReductions = armorReduction.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        ArmorReductionData current = playerReductions.get(targetUuid);

        double newReduction = (current != null ? current.reduction : 0) + (reductionPerPierce * pierceCount);
        newReduction = Math.min(newReduction, maxReduction);

        long expiryTime = System.currentTimeMillis() + duration;
        playerReductions.put(targetUuid, new ArmorReductionData(newReduction, expiryTime));

        // Exposé à -80%
        if (newReduction >= maxReduction) {
            double exposedBonus = perfTalent.getValue(3); // 0.35 = 35%
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration / 50), 0));

            player.sendMessage("§c§l[EXPOSÉ] §7Cible à §c-80%§7 armure! §a+35%§7 dégâts!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        }
    }

    public double getArmorReductionDamageBonus(Player player, LivingEntity target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Map<UUID, ArmorReductionData> playerReductions = armorReduction.get(playerUuid);
        if (playerReductions == null) return 0;

        ArmorReductionData data = playerReductions.get(targetUuid);
        if (data == null) return 0;

        // Bonus dégâts basé sur réduction d'armure
        // Simule la réduction d'armure via bonus dégâts
        double bonus = data.reduction * 0.5; // 80% armor reduction = +40% dégâts

        // Bonus EXPOSÉ
        if (data.reduction >= 0.80) {
            Talent perfTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.ABSOLUTE_PERFORATION);
            if (perfTalent != null) {
                bonus += perfTalent.getValue(3); // 0.35 = 35%
            }
        }

        return bonus;
    }

    // ==================== MOMENTUM ====================

    public void onKillDuringOverheat(Player player) {
        Talent momentumTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.HUNTER_MOMENTUM);
        if (momentumTalent == null) return;

        if (getOverheatLevel(player.getUniqueId()) <= 0) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        double speedBonus = momentumTalent.getValue(0); // 0.35 = 35%
        long baseDuration = (long) momentumTalent.getValue(1); // 2000ms
        long extension = (long) momentumTalent.getValue(2); // 1000ms
        int frenzyKills = (int) momentumTalent.getValue(3); // 3

        // Ajouter/étendre momentum
        long currentEnd = momentumEnd.getOrDefault(uuid, 0L);
        if (currentEnd > now) {
            momentumEnd.put(uuid, currentEnd + extension);
        } else {
            momentumEnd.put(uuid, now + baseDuration);
        }

        // Appliquer vitesse
        int durationTicks = (int) ((momentumEnd.get(uuid) - now) / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, false, false));

        // Compter kills consécutifs
        int kills = consecutiveKills.getOrDefault(uuid, 0) + 1;
        consecutiveKills.put(uuid, kills);

        // Frénésie à 3 kills
        if (kills >= frenzyKills && !isFrenzyActive(uuid)) {
            activateFrenzy(player);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    public boolean isFrenzyActive(UUID uuid) {
        return frenzyEnd.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    private void activateFrenzy(Player player) {
        Talent momentumTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.HUNTER_MOMENTUM);
        if (momentumTalent == null) return;

        UUID uuid = player.getUniqueId();
        double frenzyAS = momentumTalent.getValue(4); // 0.60 = 60%
        double frenzySpeed = momentumTalent.getValue(5); // 0.50 = 50%
        long frenzyDuration = (long) momentumTalent.getValue(6); // 4000ms

        frenzyEnd.put(uuid, System.currentTimeMillis() + frenzyDuration);
        consecutiveKills.put(uuid, 0);

        int durationTicks = (int) (frenzyDuration / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, 1, false, false));

        player.sendMessage("§a§l[FRÉNÉSIE] §c+60%§7 Attack Speed! §a+50%§7 Vitesse! §6Tirs enflammés!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);

        // Effet visuel
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    // ==================== DÉVASTATION ====================

    public boolean isDevastationActive(UUID uuid) {
        return devastationEnd.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public boolean canActivateDevastation(UUID uuid) {
        return devastationCooldown.getOrDefault(uuid, 0L) < System.currentTimeMillis();
    }

    public void activateDevastation(Player player) {
        Talent devTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.DEVASTATION);
        if (devTalent == null) return;

        UUID uuid = player.getUniqueId();
        if (!canActivateDevastation(uuid)) {
            long remaining = (devastationCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l[COOLDOWN] §7Dévastation disponible dans §e" + remaining + "s§7.");
            return;
        }

        long duration = (long) devTalent.getValue(0); // 8000ms
        long cooldown = (long) devTalent.getValue(4); // 30000ms

        devastationEnd.put(uuid, System.currentTimeMillis() + duration);
        devastationCooldown.put(uuid, System.currentTimeMillis() + cooldown);

        // Effets
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration / 50), 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), 1, false, false));

        player.sendMessage("§a§l[DÉVASTATION] §cPierce INFINI! §a+60%§7 dégâts! §9Slow§7 aux touchés!");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.5f);

        // Effet visuel périodique
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isDevastationActive(uuid)) {
                    cancel();
                    return;
                }
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    public double getDevastationDamageBonus(UUID uuid) {
        if (!isDevastationActive(uuid)) return 0;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 0;

        Talent devTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.DEVASTATION);
        if (devTalent == null) return 0;

        return devTalent.getValue(1); // 0.60 = 60%
    }

    public void applyDevastationSlow(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1)); // 3s slow II
    }

    // ==================== JUGEMENT ====================

    public void handleSneakForJudgment(Player player) {
        Talent judgmentTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.JUDGMENT);
        if (judgmentTalent == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Vérifier cooldown
        if (judgmentCooldown.getOrDefault(uuid, 0L) > now) {
            long remaining = (judgmentCooldown.get(uuid) - now) / 1000;
            player.sendMessage("§c§l[COOLDOWN] §7Jugement disponible dans §e" + remaining + "s§7.");
            return;
        }

        // Double sneak detection
        long lastSneak = lastSneakTime.getOrDefault(uuid, 0L);
        lastSneakTime.put(uuid, now);

        if (now - lastSneak < 500) {
            // Double sneak détecté - commencer la charge
            startJudgmentCharge(player);
        }
    }

    private void startJudgmentCharge(Player player) {
        UUID uuid = player.getUniqueId();

        if (chargingJudgment.getOrDefault(uuid, false)) return;

        Talent judgmentTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.JUDGMENT);
        if (judgmentTalent == null) return;

        long chargeTime = (long) judgmentTalent.getValue(0); // 1500ms

        chargingJudgment.put(uuid, true);
        judgmentChargeStart.put(uuid, System.currentTimeMillis());

        player.sendMessage("§a§l[JUGEMENT] §eChargement... §7Restez immobile!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);

        Location startLoc = player.getLocation();

        // Vérifier immobilité et charger
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (chargeTime / 50);

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    chargingJudgment.put(uuid, false);
                    cancel();
                    return;
                }

                // Vérifier immobilité
                if (p.getLocation().distance(startLoc) > 0.5) {
                    p.sendMessage("§c§l[JUGEMENT] §7Charge annulée - vous avez bougé!");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    chargingJudgment.put(uuid, false);
                    cancel();
                    return;
                }

                ticks++;

                // Effets de charge
                float progress = (float) ticks / maxTicks;
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0),
                    (int) (5 + progress * 15), 0.5 - progress * 0.3, 0.5, 0.5 - progress * 0.3, 0.02);

                if (ticks % 10 == 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f + progress);
                }

                // Charge complète
                if (ticks >= maxTicks) {
                    fireJudgment(p);
                    chargingJudgment.put(uuid, false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void fireJudgment(Player player) {
        Talent judgmentTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.JUDGMENT);
        if (judgmentTalent == null) return;

        UUID uuid = player.getUniqueId();
        double range = judgmentTalent.getValue(1); // 50.0 blocs
        double damageMult = judgmentTalent.getValue(2); // 10.0 = 1000%
        long armorDuration = (long) judgmentTalent.getValue(4); // 5000ms
        long fireDuration = (long) judgmentTalent.getValue(5); // 3000ms
        long cooldown = (long) judgmentTalent.getValue(6); // 45000ms

        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double judgmentDamage = baseDamage * damageMult;

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        player.sendMessage("§a§l★ JUGEMENT ★");
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);

        // Tracer le rayon
        List<LivingEntity> hit = new ArrayList<>();
        for (double d = 0; d < range; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));

            // Particules
            point.getWorld().spawnParticle(Particle.END_ROD, point, 2, 0.1, 0.1, 0.1, 0);
            point.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0.05, 0.05, 0.05, 0);

            // Traînée de feu
            if (d % 2 < 0.5) {
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks++ > 60) { // 3s
                            cancel();
                            return;
                        }
                        point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0.2, 0.1, 0.2, 0.01);

                        // Brûler les ennemis dans la traînée
                        for (Entity e : point.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                            if (e instanceof Monster target && !hit.contains(target)) {
                                target.setFireTicks(20);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }

            // Détecter les ennemis
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                if (entity instanceof Monster target && !hit.contains(target)) {
                    hit.add(target);

                    // Dégâts
                    target.damage(judgmentDamage, player);

                    // Effets
                    target.setFireTicks((int) (fireDuration / 50));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (fireDuration / 50), 2));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (armorDuration / 50), 0));

                    // Marquer comme -100% armure
                    Map<UUID, ArmorReductionData> reductions = armorReduction.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                    reductions.put(target.getUniqueId(), new ArmorReductionData(1.0, System.currentTimeMillis() + armorDuration));

                    // Effet visuel sur la cible
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
                }
            }
        }

        // Son final
        player.playSound(start.clone().add(direction.clone().multiply(range / 2)), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.5f);

        // Message résultat
        player.sendMessage("§a§l[JUGEMENT] §7" + hit.size() + " ennemis touchés! §c" + String.format("%.0f", judgmentDamage) + " dégâts!");

        // Cooldown
        judgmentCooldown.put(uuid, System.currentTimeMillis() + cooldown);
    }

    // ==================== TRAJECTOIRE FATALE ====================

    public void createFatalLine(Player player, Location start, Location end) {
        Talent fatalTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.FATAL_TRAJECTORY);
        if (fatalTalent == null) return;

        long duration = (long) fatalTalent.getValue(2); // 3000ms
        double damageBonus = fatalTalent.getValue(3); // 0.30 = 30%

        UUID uuid = player.getUniqueId();
        List<FatalLine> lines = fatalLines.computeIfAbsent(uuid, k -> new ArrayList<>());

        FatalLine line = new FatalLine(start.clone(), end.clone(), System.currentTimeMillis() + duration, damageBonus);
        lines.add(line);

        player.sendMessage("§a§l[TRAJECTOIRE] §7Ligne de Mort créée! §c+30%§7 dégâts zone!");
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7f, 1.5f);

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
                    point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public double getFatalLineDamageBonus(Player player, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        List<FatalLine> lines = fatalLines.get(uuid);
        if (lines == null || lines.isEmpty()) return 0;

        Location targetLoc = target.getLocation();

        for (FatalLine line : lines) {
            if (isInLine(targetLoc, line.start, line.end, 2.0)) {
                return line.damageBonus;
            }
        }
        return 0;
    }

    private boolean isInLine(Location point, Location lineStart, Location lineEnd, double tolerance) {
        Vector line = lineEnd.toVector().subtract(lineStart.toVector());
        Vector toPoint = point.toVector().subtract(lineStart.toVector());

        double lineLength = line.length();
        double projection = toPoint.dot(line.normalize());

        if (projection < 0 || projection > lineLength) return false;

        Location closestPoint = lineStart.clone().add(line.normalize().multiply(projection));
        return closestPoint.distance(point) <= tolerance;
    }

    // ==================== REBONDS (CHAIN PERFORATION) ====================

    public void triggerChainBounce(Player player, LivingEntity lastTarget, double baseDamage, int bounceIndex) {
        Talent chainTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.CHAIN_PERFORATION);
        if (chainTalent == null) return;

        int maxBounces = (int) chainTalent.getValue(0); // 3
        double range = chainTalent.getValue(1); // 10.0 blocs
        double caliberChance = chainTalent.getValue(5); // 0.30 = 30%

        if (bounceIndex >= maxBounces) return;

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

        // Calculer dégâts du rebond
        double damagePercent = chainTalent.getValue(2 + bounceIndex); // 0.75, 0.50, 0.25
        double bounceDamage = baseDamage * damagePercent;

        // Effet visuel
        Location from = lastTarget.getLocation().add(0, 1, 0);
        Location to = nextTarget.getLocation().add(0, 1, 0);
        Vector dir = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
        }

        // Infliger dégâts
        LivingEntity finalNextTarget = nextTarget;
        new BukkitRunnable() {
            @Override
            public void run() {
                finalNextTarget.damage(bounceDamage, player);
                finalNextTarget.getWorld().playSound(finalNextTarget.getLocation(), Sound.ENTITY_ARROW_HIT, 0.8f, 1.2f);

                // Chance de +1 Calibre
                if (Math.random() < caliberChance) {
                    addCaliber(player, 1);
                }

                // Rebond suivant
                triggerChainBounce(player, finalNextTarget, baseDamage, bounceIndex + 1);
            }
        }.runTaskLater(plugin, (long) (distance / 2));

        if (bounceIndex == 0) {
            player.sendMessage("§b§l[REBOND] §7Projectile rebondit!");
        }
    }

    // ==================== ACTIONBAR ====================

    /**
     * Construit le contenu de l'ActionBar pour un joueur Perforation
     * Appelé par ActionBarManager quand le joueur est en combat
     */
    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        StringBuilder sb = new StringBuilder();

        // Calibre
        int caliber = getCaliber(uuid);
        if (caliber > 0 || hasTalent(player, Talent.TalentEffectType.CALIBER)) {
            sb.append("§eCalibre: ");
            for (int i = 0; i < 5; i++) {
                sb.append(i < caliber ? "§e⬤" : "§8○");
            }
            sb.append(" ");
        }

        // Surchauffe
        double overheat = getOverheatLevel(uuid);
        if (overheat > 0 || hasTalent(player, Talent.TalentEffectType.OVERHEAT)) {
            int bars = (int) (overheat * 10);
            sb.append("§6Surchauffe: §c");
            for (int i = 0; i < 10; i++) {
                sb.append(i < bars ? "█" : "§8░");
            }
            sb.append("§6 ").append(String.format("%.0f", overheat * 100)).append("%");
            sb.append(" ");
        }

        // Dévastation
        if (isDevastationActive(uuid)) {
            long remaining = (devastationEnd.get(uuid) - System.currentTimeMillis()) / 1000;
            sb.append("§a§lDÉVASTATION §e").append(remaining).append("s ");
        }

        // Frénésie
        if (isFrenzyActive(uuid)) {
            long remaining = (frenzyEnd.get(uuid) - System.currentTimeMillis()) / 1000;
            sb.append("§c§lFRÉNÉSIE §e").append(remaining).append("s ");
        }

        return sb.toString().trim();
    }

    // ==================== UTILITAIRES ====================

    public boolean isPerforationPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return false;

        String branchId = data.getSelectedBranchId();
        return branchId != null && branchId.toLowerCase().contains("perforation");
    }

    public boolean hasTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType) != null;
    }

    public void registerPlayer(UUID uuid) {
        activePerforationPlayers.add(uuid);
    }

    public void cleanupPlayer(UUID uuid) {
        playerCaliber.remove(uuid);
        overheatLevel.remove(uuid);
        lastOverheatTime.remove(uuid);
        lastShotTime.remove(uuid);
        armorReduction.remove(uuid);
        consecutiveKills.remove(uuid);
        momentumEnd.remove(uuid);
        frenzyEnd.remove(uuid);
        devastationEnd.remove(uuid);
        devastationCooldown.remove(uuid);
        judgmentCooldown.remove(uuid);
        lastSneakTime.remove(uuid);
        chargingJudgment.remove(uuid);
        judgmentChargeStart.remove(uuid);
        fatalLines.remove(uuid);
        activePerforationPlayers.remove(uuid);

        // Désenregistrer de l'ActionBarManager
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(uuid);
        }
    }

    // ==================== CLASSES INTERNES ====================

    private static class ArmorReductionData {
        final double reduction;
        final long expiryTime;

        ArmorReductionData(double reduction, long expiryTime) {
            this.reduction = reduction;
            this.expiryTime = expiryTime;
        }
    }

    private static class FatalLine {
        final Location start;
        final Location end;
        final long expiryTime;
        final double damageBonus;

        FatalLine(Location start, Location end, long expiryTime, double damageBonus) {
            this.start = start;
            this.end = end;
            this.expiryTime = expiryTime;
            this.damageBonus = damageBonus;
        }
    }
}
