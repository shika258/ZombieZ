package com.rinaorc.zombiez.classes.poison;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager pour le système de la Voie du Poison du Chasseur.
 * Gère les stacks de poison, les explosions, l'aura et les synergies.
 *
 * Gameplay dynamique inspiré de Plague Doctor/Necromancer.
 */
public class PoisonManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // === CONSTANTES ===
    public static final int BASE_MAX_STACKS = 5;
    public static final int NECROSIS_THRESHOLD = 3;
    public static final int EPIDEMIC_EXPLOSION_THRESHOLD = 10;
    public static final long POISON_TICK_INTERVAL = 1000; // 1s
    public static final long AVATAR_DURATION = 20000; // 20s
    public static final long AVATAR_COOLDOWN = 60000; // 60s

    // === TRACKING ===

    // Poison stacks par entité (target UUID -> stacks)
    private final Map<UUID, Integer> poisonStacks = new ConcurrentHashMap<>();

    // Qui a empoisonné qui (target UUID -> owner UUID)
    private final Map<UUID, UUID> poisonOwners = new ConcurrentHashMap<>();

    // Expiration du poison (target UUID -> expiry time)
    private final Map<UUID, Long> poisonExpiry = new ConcurrentHashMap<>();

    // Avatar de la Peste actif (player UUID -> end time)
    private final Map<UUID, Long> plagueAvatarActive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> plagueAvatarCooldown = new ConcurrentHashMap<>();

    // Tracking des chaînes d'explosion Pandémie
    private final Map<UUID, Integer> pandemicChainCount = new ConcurrentHashMap<>();

    // Double-sneak pour Avatar
    private final Map<UUID, Long> lastAvatarSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW = 400;

    // Cache des joueurs Poison actifs pour l'ActionBar
    private final Set<UUID> activePoisonPlayers = ConcurrentHashMap.newKeySet();

    public PoisonManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        startTasks();
    }

    /**
     * Démarre les tâches périodiques
     */
    private void startTasks() {
        // Tâche principale (toutes les secondes)
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup poison expiré
                poisonExpiry.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        UUID targetUuid = entry.getKey();
                        poisonStacks.remove(targetUuid);
                        poisonOwners.remove(targetUuid);
                        return true;
                    }
                    return false;
                });

                // Cleanup Avatar expiré
                plagueAvatarActive.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        // Déclencher l'explosion finale
                        triggerAvatarFinalExplosion(entry.getKey());
                        return true;
                    }
                    return false;
                });

                // Appliquer les dégâts DoT de poison
                applyPoisonDamage();

                // Appliquer l'aura Fléau
                applyBlightAura();

                // Appliquer l'aura Avatar
                applyAvatarAura();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes

        // Tâche d'enregistrement ActionBar (toutes les 20 ticks = 1s)
        new BukkitRunnable() {
            @Override
            public void run() {
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ==================== STACKS DE POISON ====================

    /**
     * Ajoute des stacks de poison à une cible
     */
    public int addPoisonStacks(Player owner, LivingEntity target, int amount) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = owner.getUniqueId();

        int currentStacks = poisonStacks.getOrDefault(targetUuid, 0);
        int maxStacks = getMaxStacks(owner);

        // Avatar = max stacks instant
        if (isPlagueAvatarActive(ownerUuid)) {
            amount = maxStacks;
        }

        int newStacks = Math.min(currentStacks + amount, maxStacks);

        // Si Épidémie, pas de limite
        if (hasTalent(owner, Talent.TalentEffectType.EPIDEMIC)) {
            newStacks = currentStacks + amount;
        }

        poisonStacks.put(targetUuid, newStacks);
        poisonOwners.put(targetUuid, ownerUuid);
        poisonExpiry.put(targetUuid, System.currentTimeMillis() + 5000); // 5s durée

        // Appliquer effet slowness (Toxines Mortelles)
        if (hasTalent(owner, Talent.TalentEffectType.DEADLY_TOXINS)) {
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 60, 0, false, false));
        }

        // Vérifier explosion Épidémie
        if (newStacks >= EPIDEMIC_EXPLOSION_THRESHOLD && hasTalent(owner, Talent.TalentEffectType.EPIDEMIC)) {
            triggerEpidemicExplosion(owner, target);
        }

        // Effet visuel
        spawnPoisonParticles(target, newStacks);

        // Son subtil
        if (newStacks > currentStacks) {
            target.getWorld().playSound(target.getLocation(),
                Sound.BLOCK_SLIME_BLOCK_PLACE, 0.3f, 1.5f + (newStacks * 0.1f));
        }

        return newStacks;
    }

    /**
     * Récupère les stacks de poison d'une cible
     */
    public int getPoisonStacks(UUID targetUuid) {
        return poisonStacks.getOrDefault(targetUuid, 0);
    }

    /**
     * Vérifie si une cible est empoisonnée
     */
    public boolean isPoisoned(UUID targetUuid) {
        return poisonStacks.containsKey(targetUuid) && poisonStacks.get(targetUuid) > 0;
    }

    /**
     * Vérifie si la cible a la Nécrose (3+ stacks)
     */
    public boolean hasNecrosis(UUID targetUuid) {
        return getPoisonStacks(targetUuid) >= NECROSIS_THRESHOLD;
    }

    /**
     * Récupère le nombre max de stacks selon les talents
     */
    private int getMaxStacks(Player player) {
        if (hasTalent(player, Talent.TalentEffectType.EPIDEMIC)) {
            return Integer.MAX_VALUE; // Illimité
        }
        return BASE_MAX_STACKS;
    }

    // ==================== DÉGÂTS DE POISON ====================

    /**
     * Applique les dégâts de poison à toutes les cibles
     */
    private void applyPoisonDamage() {
        for (Map.Entry<UUID, Integer> entry : poisonStacks.entrySet()) {
            UUID targetUuid = entry.getKey();
            int stacks = entry.getValue();

            Entity entity = Bukkit.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity target) || target.isDead()) continue;

            UUID ownerUuid = poisonOwners.get(targetUuid);
            if (ownerUuid == null) continue;

            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner == null) continue;

            // Calculer les dégâts de base
            double baseDamage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
            double damagePercent = 0.50; // 50% par défaut (Venin Corrosif)

            // Bonus Nécrose (+25%)
            if (hasNecrosis(targetUuid) && hasTalent(owner, Talent.TalentEffectType.VENOMOUS_STRIKE)) {
                damagePercent *= 1.25;
            }

            // Bonus par stack (Épidémie)
            if (hasTalent(owner, Talent.TalentEffectType.EPIDEMIC)) {
                damagePercent += stacks * 0.10; // +10% par stack
            }

            // Crit poison (Toxines Mortelles)
            boolean isCrit = false;
            if (hasTalent(owner, Talent.TalentEffectType.DEADLY_TOXINS)) {
                if (Math.random() < 0.25) { // 25% chance
                    damagePercent *= 1.50; // +50% crit
                    isCrit = true;
                }
            }

            double finalDamage = baseDamage * damagePercent * stacks / 5.0; // Scaled par stacks

            // Appliquer les dégâts
            target.damage(finalDamage, owner);

            // Lifesteal (Peste Noire)
            if (hasTalent(owner, Talent.TalentEffectType.BLACK_PLAGUE)) {
                double heal = finalDamage * 0.10;
                double newHealth = Math.min(owner.getHealth() + heal,
                    owner.getAttribute(Attribute.MAX_HEALTH).getValue());
                owner.setHealth(newHealth);
            }

            // Particules de dégâts
            if (isCrit) {
                target.getWorld().spawnParticle(Particle.CRIT,
                    target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    // ==================== EXPLOSIONS ====================

    /**
     * Déclenche l'explosion Épidémie (à 10 stacks)
     */
    public void triggerEpidemicExplosion(Player owner, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();
        int stacks = poisonStacks.getOrDefault(targetUuid, 0);

        if (stacks < EPIDEMIC_EXPLOSION_THRESHOLD) return;

        // Reset les stacks
        poisonStacks.put(targetUuid, 0);

        // Calculer les dégâts
        double baseDamage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double explosionDamage = baseDamage * 2.0 * stacks; // 200% par stack

        Location loc = target.getLocation().add(0, 1, 0);

        // Effet visuel
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 1.5, 1, 1.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.5f);

        // Dégâts AoE
        double radius = isPlagueAvatarActive(owner.getUniqueId()) ? 8.0 : 4.0; // x2 avec Avatar

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                monster.damage(explosionDamage, owner);
                addPoisonStacks(owner, monster, 2); // Appliquer 2 stacks
            }
        }

        // Heal (Synergie Toxique)
        if (hasTalent(owner, Talent.TalentEffectType.TOXIC_SYNERGY)) {
            double heal = explosionDamage * 0.08; // 8% heal
            double newHealth = Math.min(owner.getHealth() + heal,
                owner.getAttribute(Attribute.MAX_HEALTH).getValue());
            owner.setHealth(newHealth);
            owner.sendMessage("§2§l[POISON] §aSoigné de " + String.format("%.0f", heal) + " PV!");
        }

        owner.sendMessage("§2§l[ÉPIDÉMIE] §c" + stacks + " stacks EXPLOSÉS! §eDégâts: " +
            String.format("%.0f", explosionDamage));
    }

    /**
     * Déclenche l'explosion Pandémie (à la mort d'un empoisonné)
     */
    public void triggerPandemicExplosion(Player owner, Location deathLoc, double victimMaxHealth, int chainCount) {
        if (chainCount > 3) return; // Max 3 chaînes

        double radius = 5.0;
        double damage = victimMaxHealth * 0.30; // 30% des PV max du mort

        // Effet visuel
        deathLoc.getWorld().spawnParticle(Particle.SNEEZE, deathLoc, 40, 2, 1, 2, 0.05);
        deathLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, deathLoc, 20, 2, 1, 2, 0);
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_SPIDER_DEATH, 1.0f, 0.5f);

        int killed = 0;

        for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, radius, radius, radius)) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                addPoisonStacks(owner, monster, 2);
                monster.damage(damage, owner);

                // Tracker pour chaîne
                if (monster.getHealth() <= 0) {
                    killed++;
                    pandemicChainCount.put(monster.getUniqueId(), chainCount + 1);
                }
            }
        }

        if (killed > 0) {
            owner.sendMessage("§2§l[PANDÉMIE] §eExplosion chaîne " + chainCount + "! §c" + killed + " morts");
        }
    }

    // ==================== NUAGE MORTEL (Peste Noire) ====================

    /**
     * Crée un nuage toxique à la mort par poison
     */
    public void createDeathCloud(Player owner, Location loc) {
        if (!hasTalent(owner, Talent.TalentEffectType.BLACK_PLAGUE)) return;

        double radius = 4.0;
        double dps = 0.15; // 15% dégâts/s

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60; // 3 secondes

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Particules de nuage
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * radius * 2;
                    double z = (Math.random() - 0.5) * radius * 2;
                    loc.getWorld().spawnParticle(Particle.SNEEZE,
                        loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                }

                // Dégâts toutes les secondes
                if (ticks % 20 == 0) {
                    double damage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * dps;

                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, 2, radius)) {
                        if (entity instanceof Monster monster && !monster.isDead()) {
                            monster.damage(damage, owner);
                            addPoisonStacks(owner, monster, 1);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== AURA FLÉAU ====================

    /**
     * Applique l'aura de poison du Fléau
     */
    private void applyBlightAura() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isPoisonPlayer(player)) continue;
            if (!hasTalent(player, Talent.TalentEffectType.BLIGHT)) continue;

            double radius = 5.0;
            Location loc = player.getLocation();

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof Monster monster && !monster.isDead()) {
                    addPoisonStacks(player, monster, 1);
                }
            }
        }
    }

    // ==================== AVATAR DE LA PESTE ====================

    /**
     * Active l'Avatar de la Peste
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 400, 0, false, false));
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);

        // Effets visuels
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 100, 3, 2, 3, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.8f);

        // Aura visuelle pendant l'Avatar
        startAvatarVisualAura(uuid);

        player.sendMessage("§2§l[AVATAR DE LA PESTE] §aTransformation activée! §e20s");

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
     * Applique l'aura de l'Avatar
     */
    private void applyAvatarAura() {
        for (Map.Entry<UUID, Long> entry : plagueAvatarActive.entrySet()) {
            if (System.currentTimeMillis() > entry.getValue()) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            double radius = 8.0;
            Location loc = player.getLocation();

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof Monster monster && !monster.isDead()) {
                    addPoisonStacks(player, monster, 3); // 3 stacks/s
                }
            }
        }
    }

    /**
     * Aura visuelle pendant l'Avatar
     */
    private void startAvatarVisualAura(UUID playerUuid) {
        new BukkitRunnable() {
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

                Location loc = player.getLocation();
                double time = System.currentTimeMillis() / 200.0;

                // Cercle de particules
                for (int i = 0; i < 8; i++) {
                    double angle = time + (i * 45);
                    double x = Math.cos(Math.toRadians(angle)) * 2;
                    double z = Math.sin(Math.toRadians(angle)) * 2;

                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Explosion finale de l'Avatar
     */
    private void triggerAvatarFinalExplosion(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        Location loc = player.getLocation();
        double radius = 10.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 5.0; // 500%

        // Effets visuels massifs
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 200, 5, 3, 5, 0);
        loc.getWorld().spawnParticle(Particle.SNEEZE, loc, 100, 5, 2, 5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.5f);

        int affected = 0;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                monster.damage(damage, player);

                // Max stacks sur tous
                int maxStacks = getMaxStacks(player);
                poisonStacks.put(monster.getUniqueId(), Math.min(maxStacks, 10));
                poisonOwners.put(monster.getUniqueId(), playerUuid);
                poisonExpiry.put(monster.getUniqueId(), System.currentTimeMillis() + 10000);

                affected++;
            }
        }

        player.sendMessage("§2§l[AVATAR] §cMÉGA EXPLOSION! §e" + affected + " ennemis touchés! §cDégâts: " +
            String.format("%.0f", damage));
        player.sendMessage("§2§l[AVATAR] §7Transformation terminée.");
    }

    // ==================== SYNERGIE TOXIQUE ====================

    /**
     * Calcule le bonus d'attack speed basé sur les stacks proches
     */
    public double getAttackSpeedBonus(Player player) {
        if (!hasTalent(player, Talent.TalentEffectType.TOXIC_SYNERGY)) return 0;

        int totalStacks = 0;
        double range = 8.0;
        Location loc = player.getLocation();

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (entity instanceof LivingEntity living) {
                totalStacks += getPoisonStacks(living.getUniqueId());
            }
        }

        // +5% par stack, max +40%
        return Math.min(totalStacks * 0.05, 0.40);
    }

    /**
     * Vérifie si le bonus combo Fléau est actif
     */
    public boolean isBlightComboActive(Player player) {
        if (!hasTalent(player, Talent.TalentEffectType.BLIGHT)) return false;

        int totalStacks = 0;
        double range = 5.0;
        Location loc = player.getLocation();

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (entity instanceof LivingEntity living) {
                totalStacks += getPoisonStacks(living.getUniqueId());
            }
        }

        return totalStacks >= 50; // 50+ stacks = +25% dégâts
    }

    // ==================== ActionBar ====================

    /**
     * Enregistre les providers d'ActionBar pour les joueurs Poison auprès du ActionBarManager
     */
    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isPoisonPlayer(player)) {
                // Enregistrer le provider si pas déjà fait
                if (!activePoisonPlayers.contains(uuid)) {
                    activePoisonPlayers.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                // Retirer le provider si le joueur n'est plus Poison
                if (activePoisonPlayers.contains(uuid)) {
                    activePoisonPlayers.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    /**
     * Construit le contenu de l'ActionBar pour un joueur Poison
     * Appelé par ActionBarManager quand le joueur est en combat
     */
    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();

        StringBuilder bar = new StringBuilder();

        // Compter les stacks totaux proches
        int totalStacks = 0;
        for (Entity entity : player.getLocation().getWorld().getNearbyEntities(
                player.getLocation(), 8, 8, 8)) {
            if (entity instanceof LivingEntity living) {
                totalStacks += getPoisonStacks(living.getUniqueId());
            }
        }

        bar.append("§2§l[POISON] §aStacks: §e").append(totalStacks);

        // Bonus AS (Synergie Toxique)
        if (hasTalent(player, Talent.TalentEffectType.TOXIC_SYNERGY)) {
            double bonus = getAttackSpeedBonus(player);
            bar.append("  §aAS: §b+").append(String.format("%.0f", bonus * 100)).append("%");
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

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si un joueur est dans la voie Poison
     */
    public boolean isPoisonPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return false;

        String branchId = data.getSelectedBranchId();
        return branchId != null && (branchId.toLowerCase().contains("poison") || branchId.toLowerCase().contains("traque"));
    }

    public boolean hasTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType) != null;
    }

    /**
     * Génère des particules de poison
     */
    private void spawnPoisonParticles(LivingEntity target, int stacks) {
        Location loc = target.getLocation().add(0, 1, 0);

        // Plus de particules = plus de stacks
        int count = Math.min(stacks * 2, 15);

        // Couleur change avec Nécrose
        if (stacks >= NECROSIS_THRESHOLD) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, count, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(50, 0, 50), 1.0f)); // Violet foncé
        } else {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, count, 0.3, 0.3, 0.3, 0);
        }
    }

    /**
     * Gère le double-sneak pour Avatar
     */
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
     * Récupère le compteur de chaîne Pandémie
     */
    public int getPandemicChainCount(UUID entityUuid) {
        return pandemicChainCount.getOrDefault(entityUuid, 0);
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID playerUuid) {
        plagueAvatarActive.remove(playerUuid);
        plagueAvatarCooldown.remove(playerUuid);
        lastAvatarSneakTime.remove(playerUuid);
        pandemicChainCount.remove(playerUuid);
        activePoisonPlayers.remove(playerUuid);

        // Désenregistrer de l'ActionBarManager
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(playerUuid);
        }
    }
}
