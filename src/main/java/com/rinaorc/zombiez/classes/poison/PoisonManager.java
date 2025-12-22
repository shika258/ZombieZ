package com.rinaorc.zombiez.classes.poison;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager pour la Voie du Poison du Chasseur.
 *
 * REFONTE COMPLÈTE - Système "Fléau Rampant"
 *
 * Concept: Le poison est une corruption progressive qui se propage,
 * pas un système d'explosion qui freeze le serveur.
 *
 * Mécanique principale: VIRULENCE (0-100)
 * - S'accumule progressivement sur les cibles
 * - DoT proportionnel à la virulence
 * - À 100%: cible "Corrompue" = bonus dégâts + propagation à la mort
 */
public class PoisonManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // === CONSTANTES DE BALANCE ===
    public static final int MAX_VIRULENCE = 100;
    public static final int CORRUPTED_THRESHOLD = 100; // Seuil pour être "Corrompu"
    public static final int NECROSIS_THRESHOLD = 70;   // Seuil pour bonus Nécrose

    // Virulence appliquée par hit (base)
    public static final int BASE_VIRULENCE_PER_HIT = 15;

    // DoT: % des dégâts de base par seconde par 10 virulence
    public static final double DOT_PERCENT_PER_10_VIRULENCE = 0.08; // 8% par 10 virulence

    // Propagation à la mort: nombre max de cibles
    public static final int PROPAGATION_MAX_TARGETS = 3;
    public static final double PROPAGATION_RADIUS = 5.0;
    public static final int PROPAGATION_VIRULENCE = 40; // Virulence transmise

    // Avatar
    public static final long AVATAR_DURATION = 15000; // 15s (réduit de 20s)
    public static final long AVATAR_COOLDOWN = 60000; // 60s
    public static final double AVATAR_VIRULENCE_MULT = 3.0; // x3 virulence
    public static final double AVATAR_FINAL_EXPLOSION_DAMAGE = 5.0; // 500% dégâts max
    public static final double AVATAR_FINAL_EXPLOSION_CAP = 1000.0; // Cap absolu de dégâts

    // Durée du poison avant expiration
    public static final long POISON_DURATION = 6000; // 6s

    // === TRACKING ===

    // Virulence par entité (target UUID -> virulence 0-100)
    private final Map<UUID, Integer> virulence = new ConcurrentHashMap<>();

    // Propriétaire du poison (target UUID -> owner UUID)
    private final Map<UUID, UUID> poisonOwners = new ConcurrentHashMap<>();

    // Expiration du poison (target UUID -> expiry time)
    private final Map<UUID, Long> poisonExpiry = new ConcurrentHashMap<>();

    // Avatar de la Peste actif (player UUID -> end time)
    private final Map<UUID, Long> plagueAvatarActive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> plagueAvatarCooldown = new ConcurrentHashMap<>();

    // Double-sneak pour Avatar
    private final Map<UUID, Long> lastAvatarSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW = 400;

    // Cache des joueurs Poison actifs pour l'ActionBar
    private final Set<UUID> activePoisonPlayers = ConcurrentHashMap.newKeySet();

    // Cooldown entre ticks de DoT par cible (évite le spam)
    private final Map<UUID, Long> lastDotTick = new ConcurrentHashMap<>();

    public PoisonManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        startTasks();
    }

    /**
     * Démarre les tâches périodiques
     */
    private void startTasks() {
        // Tâche principale (toutes les 0.5s - 10 ticks) - DoT plus rapide = plus de puissance
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup poison expiré
                poisonExpiry.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        UUID targetUuid = entry.getKey();
                        virulence.remove(targetUuid);
                        poisonOwners.remove(targetUuid);
                        lastDotTick.remove(targetUuid);
                        return true;
                    }
                    return false;
                });

                // Cleanup Avatar expiré
                plagueAvatarActive.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        triggerAvatarFinalExplosion(entry.getKey());
                        return true;
                    }
                    return false;
                });

                // Appliquer les dégâts DoT (2x par seconde = DPS doublé)
                applyPoisonDamage();

                // Appliquer l'aura Fléau (passif)
                applyBlightAura();

                // Appliquer l'aura Avatar (si actif)
                applyAvatarAura();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Tâche d'enregistrement ActionBar (toutes les secondes)
        new BukkitRunnable() {
            @Override
            public void run() {
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Tâche de particules légères (toutes les 10 ticks = 0.5s)
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnAmbientPoisonParticles();
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // ==================== VIRULENCE ====================

    /**
     * Ajoute de la virulence à une cible
     * @return La nouvelle virulence
     */
    public int addVirulence(Player owner, LivingEntity target, int amount) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = owner.getUniqueId();

        int current = virulence.getOrDefault(targetUuid, 0);

        // Bonus Avatar: x3 virulence
        if (isPlagueAvatarActive(ownerUuid)) {
            amount = (int) (amount * AVATAR_VIRULENCE_MULT);
        }

        int newVirulence = Math.min(current + amount, MAX_VIRULENCE);

        virulence.put(targetUuid, newVirulence);
        poisonOwners.put(targetUuid, ownerUuid);
        poisonExpiry.put(targetUuid, System.currentTimeMillis() + POISON_DURATION);

        // Effet slowness progressif (Toxines Mortelles T3)
        if (hasTalent(owner, Talent.TalentEffectType.DEADLY_TOXINS)) {
            // Slowness level basé sur virulence: 0-49% = I, 50-69% = II, 70%+ = III
            int slowLevel;
            if (newVirulence >= NECROSIS_THRESHOLD) {
                slowLevel = 2; // Slowness III à 70%+
            } else if (newVirulence >= 50) {
                slowLevel = 1; // Slowness II à 50%+
            } else {
                slowLevel = 0; // Slowness I sinon
            }
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 60, slowLevel, false, false));
        }

        // Son subtil à l'application
        if (newVirulence > current) {
            float pitch = 1.2f + (newVirulence / 100f) * 0.5f;
            target.getWorld().playSound(target.getLocation(),
                Sound.BLOCK_HONEY_BLOCK_SLIDE, 0.3f, pitch);
        }

        // Message si atteint 100% (Corrompu)
        if (newVirulence == MAX_VIRULENCE && current < MAX_VIRULENCE) {
            owner.sendMessage("§2§l[POISON] §aCible §d§lCORROMPUE§a! (+30% dégâts, propagation à la mort)");
            target.getWorld().playSound(target.getLocation(),
                Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        }

        return newVirulence;
    }

    /**
     * Récupère la virulence d'une cible
     */
    public int getVirulence(UUID targetUuid) {
        return virulence.getOrDefault(targetUuid, 0);
    }

    /**
     * Vérifie si une cible est empoisonnée
     */
    public boolean isPoisoned(UUID targetUuid) {
        return virulence.containsKey(targetUuid) && virulence.get(targetUuid) > 0;
    }

    /**
     * Vérifie si une cible est Corrompue (100% virulence)
     */
    public boolean isCorrupted(UUID targetUuid) {
        return getVirulence(targetUuid) >= CORRUPTED_THRESHOLD;
    }

    /**
     * Vérifie si une cible a la Nécrose (70%+ virulence)
     */
    public boolean hasNecrosis(UUID targetUuid) {
        return getVirulence(targetUuid) >= NECROSIS_THRESHOLD;
    }

    // ==================== DÉGÂTS DE POISON (DoT) ====================

    /**
     * Applique les dégâts de poison à toutes les cibles empoisonnées
     * REFONTE: Les DoTs peuvent crit et utilisent les stats du joueur
     */
    private void applyPoisonDamage() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Integer> entry : virulence.entrySet()) {
            UUID targetUuid = entry.getKey();
            int vir = entry.getValue();

            if (vir <= 0) continue;

            Entity entity = Bukkit.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity target) || target.isDead()) continue;

            UUID ownerUuid = poisonOwners.get(targetUuid);
            if (ownerUuid == null) continue;

            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner == null) continue;

            // Éviter les ticks trop rapprochés (0.45s = tick toutes les 10 ticks)
            Long lastTick = lastDotTick.get(targetUuid);
            if (lastTick != null && now - lastTick < 450) continue; // Min 0.45s entre ticks
            lastDotTick.put(targetUuid, now);

            // === CALCUL DES DÉGÂTS DoT ===
            double baseDamage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue();

            // Stats ZombieZ
            Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(owner);
            double flatBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
            double percentBonus = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);

            // Skill tree
            var skillManager = plugin.getSkillTreeManager();
            double skillDamageBonus = skillManager.getSkillBonus(owner, SkillBonus.DAMAGE_PERCENT);

            // Dégâts de base avec stats
            baseDamage += flatBonus;
            baseDamage *= (1 + (percentBonus + skillDamageBonus) / 100.0);

            // DoT basé sur virulence: 8% par 10 virulence
            double dotPercent = (vir / 10.0) * DOT_PERCENT_PER_10_VIRULENCE;
            double dotDamage = baseDamage * dotPercent;

            // Bonus Nécrose (+25% dégâts DoT si 70%+ virulence)
            // Ce bonus est inclus avec le talent T1 (Frappe Venimeuse)
            if (hasNecrosis(targetUuid)) {
                Talent venomous = talentManager.getActiveTalentByEffect(owner, Talent.TalentEffectType.VENOMOUS_STRIKE);
                if (venomous != null) {
                    double necrosisBonus = venomous.getValue(3); // 0.25 = 25%
                    dotDamage *= (1 + necrosisBonus);
                }
            }

            // === SYSTÈME DE CRIT SUR DoT (Toxines Mortelles) ===
            boolean isCrit = false;
            if (hasTalent(owner, Talent.TalentEffectType.DEADLY_TOXINS)) {
                double critChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
                double skillCritChance = skillManager.getSkillBonus(owner, SkillBonus.CRIT_CHANCE);
                double totalCritChance = critChance + skillCritChance;

                if (Math.random() * 100 < totalCritChance) {
                    isCrit = true;
                    double baseCritDamage = 150.0;
                    double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
                    double skillCritDamage = skillManager.getSkillBonus(owner, SkillBonus.CRIT_DAMAGE);
                    double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
                    dotDamage *= critMultiplier;
                }
            }

            // Minimum de dégâts
            dotDamage = Math.max(1.0, dotDamage);

            // === APPLICATION DES DÉGÂTS ===
            // Metadata pour bypass CombatListener (dégâts déjà calculés)
            target.setMetadata("zombiez_talent_damage", new FixedMetadataValue(plugin, true));
            target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCrit));
            target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, ownerUuid.toString()));

            if (plugin.getZombieManager().isZombieZMob(target)) {
                target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, ownerUuid.toString()));
            }

            target.damage(dotDamage, owner);

            // === LIFESTEAL (Peste Noire) ===
            if (hasTalent(owner, Talent.TalentEffectType.BLACK_PLAGUE)) {
                double healPercent = 0.15; // 15% lifesteal
                double heal = dotDamage * healPercent;
                double maxHealth = owner.getAttribute(Attribute.MAX_HEALTH).getValue();
                double newHealth = Math.min(owner.getHealth() + heal, maxHealth);
                owner.setHealth(newHealth);
            }

            // === PARTICULES DE DoT (plus impactantes) ===
            Location loc = target.getLocation().add(0, 1, 0);
            if (isCrit) {
                // Crit: particules de crit + poison (satisfaisant)
                target.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.3, 0.3, 0.3, 0.15);
                target.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 255, 50), 1.0f));
                target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.2f);
            } else {
                // DoT normal: effet de "corrosion" visible
                int particleCount = 2 + vir / 50; // Plus de particules à haute virulence
                int green = 200 - (vir * 80 / 100);
                target.getWorld().spawnParticle(Particle.DUST, loc, particleCount, 0.25, 0.3, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(60, Math.max(80, green), 40), 0.9f));
            }
            // Son subtil de corrosion (pas à chaque tick - seulement haute virulence)
            if (vir >= 70 && Math.random() < 0.3) {
                target.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_HIT, 0.25f, 1.8f);
            }
        }
    }

    // ==================== PROPAGATION ====================

    /**
     * Propage le poison à la mort d'une cible corrompue
     * REFONTE: Simple propagation, pas d'explosion, pas de chaîne récursive
     */
    public void propagateOnDeath(Player owner, LivingEntity victim) {
        UUID victimUuid = victim.getUniqueId();

        // Seulement si la cible était empoisonnée
        if (!isPoisoned(victimUuid)) return;

        // Bonus si corrompue
        boolean wasCorrupted = isCorrupted(victimUuid);
        int propagationAmount = wasCorrupted ? PROPAGATION_VIRULENCE + 20 : PROPAGATION_VIRULENCE;

        Location deathLoc = victim.getLocation();

        // Trouver les cibles proches (max PROPAGATION_MAX_TARGETS)
        List<Monster> nearbyTargets = new ArrayList<>();
        for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc,
                PROPAGATION_RADIUS, PROPAGATION_RADIUS, PROPAGATION_RADIUS)) {
            if (entity instanceof Monster monster &&
                !monster.isDead() &&
                monster.getUniqueId() != victimUuid) {
                nearbyTargets.add(monster);
            }
        }

        // Limiter au max
        if (nearbyTargets.size() > PROPAGATION_MAX_TARGETS) {
            // Trier par distance et prendre les plus proches
            nearbyTargets.sort(Comparator.comparingDouble(
                m -> m.getLocation().distanceSquared(deathLoc)));
            nearbyTargets = nearbyTargets.subList(0, PROPAGATION_MAX_TARGETS);
        }

        // Appliquer la propagation
        for (Monster target : nearbyTargets) {
            addVirulence(owner, target, propagationAmount);

            // Effet visuel de propagation (ligne de particules)
            createPropagationLine(deathLoc.clone().add(0, 1, 0),
                target.getLocation().add(0, 1, 0));
        }

        // Effet visuel au centre (nuage qui se dissipe, pas explosion)
        spawnPropagationCloud(deathLoc);

        // Sons de propagation (plus satisfaisants)
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 1.4f);
        deathLoc.getWorld().playSound(deathLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.7f);
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 0.6f, 1.0f);

        // Message
        if (!nearbyTargets.isEmpty()) {
            String msg = wasCorrupted ?
                "§2§l[PROPAGATION] §d§lCORRUPTION§a propagée à §e" + nearbyTargets.size() + "§a cibles!" :
                "§2§l[PROPAGATION] §aPoison propagé à §e" + nearbyTargets.size() + "§a cibles";
            owner.sendMessage(msg);
        }

        // Cleanup la victime
        virulence.remove(victimUuid);
        poisonOwners.remove(victimUuid);
        poisonExpiry.remove(victimUuid);
    }

    /**
     * Crée une chaîne de poison animée entre deux points
     * Effet de "lien toxique" qui se propage visuellement
     */
    private void createPropagationLine(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();

        // Animation de la chaîne de poison (effet de vague qui se propage)
        new BukkitRunnable() {
            int step = 0;
            final int totalSteps = (int) (length / 0.4);

            @Override
            public void run() {
                if (step > totalSteps + 3) {
                    cancel();
                    return;
                }

                // Particules qui avancent comme une vague
                for (int i = Math.max(0, step - 3); i <= step && i <= totalSteps; i++) {
                    double dist = i * 0.4;
                    Location point = from.clone().add(direction.clone().multiply(dist));

                    // Couleur qui s'intensifie vers la cible
                    float progress = (float) i / totalSteps;
                    int green = (int) (180 - progress * 60); // 180 → 120
                    int red = (int) (progress * 100);        // 0 → 100

                    // Effet de spirale légère autour de la ligne
                    double angle = (step + i) * 0.8;
                    double offsetX = Math.cos(angle) * 0.15;
                    double offsetZ = Math.sin(angle) * 0.15;

                    point.getWorld().spawnParticle(Particle.DUST,
                        point.clone().add(offsetX, 0, offsetZ), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(red, green, 50), 0.7f));
                }

                // Son subtil à mi-chemin
                if (step == totalSteps / 2) {
                    from.getWorld().playSound(to, Sound.BLOCK_SLIME_BLOCK_HIT, 0.4f, 1.5f);
                }

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Crée un nuage de propagation avec effet de "burst" toxique
     */
    private void spawnPropagationCloud(Location loc) {
        // Burst initial + nuage qui monte et se dissipe
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 12) {
                    cancel();
                    return;
                }

                if (ticks == 0) {
                    // Burst initial - cercle de particules qui s'expand
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.toRadians(i * 45);
                        double x = Math.cos(angle) * 0.8;
                        double z = Math.sin(angle) * 0.8;
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.5, z),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(50, 200, 50), 1.2f));
                    }
                } else {
                    // Nuage qui monte avec effet spiral
                    double y = ticks * 0.12;
                    double spread = 0.3 + ticks * 0.08;
                    double angle = ticks * 30;

                    for (int i = 0; i < 3; i++) {
                        double a = Math.toRadians(angle + i * 120);
                        double x = Math.cos(a) * spread;
                        double z = Math.sin(a) * spread;

                        int green = 180 - ticks * 8;
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, y, z),
                            1, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(40, Math.max(80, green), 40),
                                Math.max(0.4f, 1.0f - ticks * 0.06f)));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ==================== AURA FLÉAU ====================

    /**
     * Applique l'aura passive de poison (Fléau)
     * Ajoute de la virulence aux ennemis proches chaque seconde
     */
    private void applyBlightAura() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isPoisonPlayer(player)) continue;
            if (!hasTalent(player, Talent.TalentEffectType.BLIGHT)) continue;

            double radius = 4.0;
            Location loc = player.getLocation();
            int virulencePerTick = 5; // 5 virulence/s dans l'aura

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof Monster monster && !monster.isDead()) {
                    addVirulence(player, monster, virulencePerTick);
                }
            }
        }
    }

    // ==================== AVATAR DE LA PESTE ====================

    /**
     * Active l'Avatar de la Peste
     * REFONTE: Plus de dégâts infinis, cap absolu sur l'explosion finale
     */
    public boolean activatePlagueAvatar(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier cooldown
        Long cooldown = plagueAvatarCooldown.get(uuid);
        if (cooldown != null && System.currentTimeMillis() < cooldown) {
            long remaining = (cooldown - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l[POISON] §7Avatar en cooldown: §c" + remaining + "s");
            return false;
        }

        // Activer l'Avatar
        plagueAvatarActive.put(uuid, System.currentTimeMillis() + AVATAR_DURATION);
        plagueAvatarCooldown.put(uuid, System.currentTimeMillis() + AVATAR_COOLDOWN);

        // Immunité poison/wither
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);

        // Buff de vitesse
        int durationTicks = (int) (AVATAR_DURATION / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, false, true));

        // Effets visuels (réduits - pas d'EXPLOSION)
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 1.5, 1, 1.5, 0,
            new Particle.DustOptions(Color.fromRGB(50, 200, 50), 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);

        // Aura visuelle
        startAvatarVisualAura(uuid);

        player.sendMessage("§2§l[AVATAR DE LA PESTE] §aTransformation! §ex3 virulence §7| §e" +
            (AVATAR_DURATION / 1000) + "s");

        return true;
    }

    /**
     * Vérifie si l'Avatar est actif
     */
    public boolean isPlagueAvatarActive(UUID playerUuid) {
        Long endTime = plagueAvatarActive.get(playerUuid);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    /**
     * Applique l'aura de l'Avatar (virulence aux ennemis proches)
     */
    private void applyAvatarAura() {
        for (Map.Entry<UUID, Long> entry : plagueAvatarActive.entrySet()) {
            if (System.currentTimeMillis() > entry.getValue()) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            double radius = 6.0;
            Location loc = player.getLocation();
            int virulencePerTick = 10; // 10 virulence/s (x3 avec Avatar = 30 effectif)

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof Monster monster && !monster.isDead()) {
                    // addVirulence applique déjà le x3 d'Avatar
                    addVirulence(player, monster, virulencePerTick);
                }
            }
        }
    }

    /**
     * Aura visuelle pendant l'Avatar (réduite)
     */
    private void startAvatarVisualAura(UUID playerUuid) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!plagueAvatarActive.containsKey(playerUuid)) {
                    cancel();
                    return;
                }

                Player player = Bukkit.getPlayer(playerUuid);
                if (player == null) {
                    cancel();
                    return;
                }

                // Particules réduites: 4 particules en cercle, toutes les 4 ticks
                if (ticks % 4 == 0) {
                    Location loc = player.getLocation();
                    double angle = (ticks * 5) % 360;

                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(angle + i * 90);
                        double x = Math.cos(a) * 1.5;
                        double z = Math.sin(a) * 1.5;

                        loc.getWorld().spawnParticle(Particle.DUST,
                            loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(50, 200, 50), 0.8f));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Explosion finale de l'Avatar
     * REFONTE: Cap absolu sur les dégâts pour éviter l'infini
     */
    private void triggerAvatarFinalExplosion(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        Location loc = player.getLocation();
        double radius = 8.0;

        // Dégâts basés sur les dégâts du joueur mais CAPPÉS
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double damage = baseDamage * AVATAR_FINAL_EXPLOSION_DAMAGE; // 500%
        damage = Math.min(damage, AVATAR_FINAL_EXPLOSION_CAP); // Cap à 1000

        // Effets visuels (pas d'EXPLOSION_EMITTER, juste des particules vertes)
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 50, 3, 2, 3, 0,
            new Particle.DustOptions(Color.fromRGB(50, 200, 50), 2.0f));
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 4, 1, 4, 0,
            new Particle.DustOptions(Color.fromRGB(100, 255, 100), 1.5f));

        // Sons
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);

        int affected = 0;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                // Metadata pour bypass CombatListener
                monster.setMetadata("zombiez_talent_damage", new FixedMetadataValue(plugin, true));
                monster.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
                monster.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, playerUuid.toString()));

                if (plugin.getZombieManager().isZombieZMob(monster)) {
                    monster.setMetadata("last_damage_player", new FixedMetadataValue(plugin, playerUuid.toString()));
                }

                monster.damage(damage, player);

                // Appliquer virulence max (100)
                virulence.put(monster.getUniqueId(), MAX_VIRULENCE);
                poisonOwners.put(monster.getUniqueId(), playerUuid);
                poisonExpiry.put(monster.getUniqueId(), System.currentTimeMillis() + 10000);

                affected++;
            }
        }

        player.sendMessage("§2§l[AVATAR] §aExplosion finale! §e" + affected + " ennemis §7| §c" +
            String.format("%.0f", damage) + " dégâts (max " + (int)AVATAR_FINAL_EXPLOSION_CAP + ")");
        player.sendMessage("§2§l[AVATAR] §7Transformation terminée.");
    }

    // ==================== SYNERGIE TOXIQUE ====================

    /**
     * Calcule le bonus de dégâts basé sur les cibles empoisonnées proches
     */
    public double getToxicSynergyBonus(Player player) {
        if (!hasTalent(player, Talent.TalentEffectType.TOXIC_SYNERGY)) return 0;

        Talent synergyTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.TOXIC_SYNERGY);
        if (synergyTalent == null) return 0;

        double bonusPerTenVir = synergyTalent.getValue(0); // 0.01 = 1% par 10 vir
        double range = synergyTalent.getValue(1);          // 8.0 blocs
        double maxBonus = synergyTalent.getValue(2);       // 0.25 = 25% max

        int totalVirulence = 0;
        Location loc = player.getLocation();

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (entity instanceof LivingEntity living) {
                totalVirulence += getVirulence(living.getUniqueId());
            }
        }

        // +1% dégâts par 10 virulence totale, max +25%
        double bonus = (totalVirulence / 10.0) * bonusPerTenVir;
        return Math.min(bonus, maxBonus);
    }

    /**
     * Vérifie si le bonus combo Fléau est actif
     * Activé quand 200+ virulence totale dans la zone
     */
    public boolean isBlightComboActive(Player player) {
        Talent blightTalent = talentManager.getActiveTalentByEffect(player, Talent.TalentEffectType.BLIGHT);
        if (blightTalent == null) return false;

        double range = blightTalent.getValue(0);           // 4.0 blocs (aura range)
        int comboThreshold = (int) blightTalent.getValue(2); // 200 virulence

        int totalVirulence = 0;
        Location loc = player.getLocation();

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (entity instanceof LivingEntity living) {
                totalVirulence += getVirulence(living.getUniqueId());
            }
        }

        return totalVirulence >= comboThreshold; // 200+ virulence = +20% dégâts
    }

    // ==================== ActionBar ====================

    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isPoisonPlayer(player)) {
                if (!activePoisonPlayers.contains(uuid)) {
                    activePoisonPlayers.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                if (activePoisonPlayers.contains(uuid)) {
                    activePoisonPlayers.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();

        StringBuilder bar = new StringBuilder();

        // Compter la virulence totale et les cibles corrompues
        int totalVirulence = 0;
        int corruptedCount = 0;
        for (Entity entity : player.getLocation().getWorld().getNearbyEntities(
                player.getLocation(), 10, 10, 10)) {
            if (entity instanceof LivingEntity living) {
                int vir = getVirulence(living.getUniqueId());
                totalVirulence += vir;
                if (vir >= CORRUPTED_THRESHOLD) corruptedCount++;
            }
        }

        bar.append("§2§l[§a☠§2§l] ");

        // Virulence totale
        bar.append("§aVir: §e").append(totalVirulence);

        // Cibles corrompues
        if (corruptedCount > 0) {
            bar.append("  §d§l").append(corruptedCount).append(" CORR");
        }

        // Bonus Synergie
        if (hasTalent(player, Talent.TalentEffectType.TOXIC_SYNERGY)) {
            double bonus = getToxicSynergyBonus(player);
            if (bonus > 0) {
                bar.append("  §b+").append(String.format("%.0f", bonus * 100)).append("%");
            }
        }

        // Combo Fléau
        if (isBlightComboActive(player)) {
            bar.append("  §c§lCOMBO!");
        }

        // Avatar actif
        if (isPlagueAvatarActive(uuid)) {
            long remaining = (plagueAvatarActive.get(uuid) - System.currentTimeMillis()) / 1000;
            bar.append("  §2§lAVATAR §a").append(remaining).append("s");
        }

        return bar.toString();
    }

    // ==================== PARTICULES AMBIANTES ====================

    /**
     * Génère des particules légères sur les cibles empoisonnées
     */
    private void spawnAmbientPoisonParticles() {
        for (UUID targetUuid : virulence.keySet()) {
            int vir = virulence.get(targetUuid);
            if (vir <= 0) continue;

            Entity entity = Bukkit.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity target) || target.isDead()) continue;

            Location loc = target.getLocation().add(0, target.getHeight() * 0.5, 0);

            // Intensité basée sur la virulence (1-3 particules)
            int count = 1 + vir / 40;
            count = Math.min(count, 3);

            // Couleur: vert -> violet foncé selon virulence
            int green = 200 - (vir * 150 / 100);
            int red = vir * 80 / 100;
            Color color = Color.fromRGB(Math.max(0, red), Math.max(50, green), 50);

            target.getWorld().spawnParticle(Particle.DUST, loc, count, 0.2, 0.3, 0.2, 0,
                new Particle.DustOptions(color, 0.5f));
        }
    }

    // ==================== UTILITAIRES ====================

    public boolean isPoisonPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return false;

        String branchId = data.getSelectedBranchId();
        return branchId != null && (branchId.toLowerCase().contains("poison") ||
                                    branchId.toLowerCase().contains("traque"));
    }

    public boolean hasTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType) != null;
    }

    public void handleSneakForAvatar(Player player) {
        if (!hasTalent(player, Talent.TalentEffectType.PLAGUE_AVATAR)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastSneak = lastAvatarSneakTime.get(uuid);

        if (lastSneak != null && now - lastSneak < DOUBLE_SNEAK_WINDOW) {
            activatePlagueAvatar(player);
            lastAvatarSneakTime.remove(uuid);
        } else {
            lastAvatarSneakTime.put(uuid, now);
        }
    }

    /**
     * Obtient le propriétaire du poison sur une cible
     */
    public UUID getPoisonOwner(UUID targetUuid) {
        return poisonOwners.get(targetUuid);
    }

    public void cleanupPlayer(UUID playerUuid) {
        plagueAvatarActive.remove(playerUuid);
        plagueAvatarCooldown.remove(playerUuid);
        lastAvatarSneakTime.remove(playerUuid);
        activePoisonPlayers.remove(playerUuid);

        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(playerUuid);
        }
    }
}
