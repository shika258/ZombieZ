package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DPSTracker;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour le système de trident amélioré ZombieZ
 *
 * Fonctionnalités:
 * - Empêche le lancer du trident (protection contre perte d'arme)
 * - Système d'estoc automatique après 3 attaques touchées
 * - Attaque perforante (Pierce) - traverse plusieurs ennemis
 * - Bonus aquatique passif (eau/pluie)
 */
public class TridentListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // ==================== CONFIGURATION ====================

    // Combo Thrust (Estoc)
    private static final int HITS_FOR_THRUST = 3;             // 3 attaques pour déclencher l'estoc
    private static final double THRUST_DAMAGE_MULTIPLIER = 2.0; // 200% dégâts pour l'estoc
    private static final double THRUST_DASH_DISTANCE = 3.0;   // Distance du dash
    private static final long HIT_COMBO_TIMEOUT = 4000;       // 4 secondes pour perdre le combo

    // Pierce Attack
    private static final int PIERCE_MAX_TARGETS = 3;          // Nombre max d'ennemis traversés
    private static final double PIERCE_DAMAGE_FALLOFF = 0.75; // 75% des dégâts par cible suivante
    private static final double PIERCE_RANGE = 4.0;           // Portée de perforation
    private static final double PIERCE_WIDTH = 1.5;           // Largeur du cône de perforation

    // Bonus Aquatique
    private static final double WATER_DAMAGE_BONUS = 1.20;    // +20% dégâts dans l'eau
    private static final double WATER_SPEED_BONUS = 1.30;     // +30% vitesse d'attaque dans l'eau
    private static final double RAIN_DAMAGE_BONUS = 1.10;     // +10% dégâts sous la pluie
    private static final double RAIN_SPEED_BONUS = 1.15;      // +15% vitesse d'attaque sous la pluie

    // ==================== TRACKING MAPS ====================

    // Compteur de hits pour le combo (UUID -> nombre de hits)
    private final Map<UUID, Integer> hitCounter = new ConcurrentHashMap<>();

    // Timestamp du dernier hit (UUID -> timestamp)
    private final Map<UUID, Long> lastHitTime = new ConcurrentHashMap<>();

    public TridentListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== EMPÊCHER LE LANCER ====================

    /**
     * Empêche le lancer du trident vanilla
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;

        if (trident.getShooter() instanceof Player player) {
            // Annuler le lancer
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
        }
    }

    /**
     * Empêche le riptide
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRiptide(PlayerRiptideEvent event) {
        // Le riptide est annulé par défaut quand on empêche le lancer
    }

    // ==================== SYSTÈME DE COMBO ESTOC ====================

    /**
     * Intercepte les attaques mêlée avec trident pour le système de combo
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onTridentMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.TRIDENT) return;

        // Ignorer si c'est une attaque d'estoc (déjà traitée)
        if (target.hasMetadata("zombiez_trident_thrust")) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Vérifier le timeout du combo
        Long lastHit = lastHitTime.get(playerId);
        if (lastHit != null && (currentTime - lastHit) > HIT_COMBO_TIMEOUT) {
            // Reset le combo si trop de temps s'est écoulé
            hitCounter.put(playerId, 0);
        }

        // Incrémenter le compteur de hits
        int currentHits = hitCounter.getOrDefault(playerId, 0) + 1;
        hitCounter.put(playerId, currentHits);
        lastHitTime.put(playerId, currentTime);

        // Appliquer le bonus aquatique aux dégâts de base
        double aquaticMultiplier = calculateAquaticDamageBonus(player);
        if (aquaticMultiplier > 1.0) {
            event.setDamage(event.getDamage() * aquaticMultiplier);

            // Particules d'eau
            if (isInWater(player)) {
                player.getWorld().spawnParticle(Particle.BUBBLE_POP,
                    target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            } else {
                player.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                    target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }

        // Marquer comme attaque trident pour éviter double-traitement
        target.setMetadata("zombiez_trident_attack", new FixedMetadataValue(plugin, true));

        // Feedback sonore pour le combo
        if (currentHits < HITS_FOR_THRUST) {
            // Son de progression du combo
            float pitch = 0.8f + (currentHits * 0.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, pitch);

            // Particules de progression
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, loc, currentHits * 2, 0.3, 0.3, 0.3, 0.02);
        }

        // Vérifier si on atteint le seuil pour l'estoc
        if (currentHits >= HITS_FOR_THRUST) {
            // Reset le compteur
            hitCounter.put(playerId, 0);

            // Exécuter l'estoc automatiquement
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                executeAutoThrust(player);
            });
        }
    }

    /**
     * Exécute l'estoc automatique après 3 hits
     */
    private void executeAutoThrust(Player player) {
        // Effectuer le dash
        performDash(player);

        // Sons d'estoc
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 1.2f);

        // Effectuer l'attaque perforante avec bonus de dégâts
        performPierceAttack(player, THRUST_DAMAGE_MULTIPLIER, true);
    }

    /**
     * Effectue le dash vers l'avant
     */
    private void performDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        Vector dashVelocity = direction.multiply(THRUST_DASH_DISTANCE * 0.5);
        dashVelocity.setY(Math.max(0.1, dashVelocity.getY() * 0.3)); // Limiter le Y

        player.setVelocity(player.getVelocity().add(dashVelocity));

        // Particules de dash
        Location loc = player.getLocation();
        for (int i = 0; i < 10; i++) {
            Location particleLoc = loc.clone().add(direction.clone().multiply(-i * 0.3));
            player.getWorld().spawnParticle(Particle.CLOUD, particleLoc.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.01);
        }

        // Particules d'estoc puissant
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
    }

    // ==================== ATTAQUE PERFORANTE ====================

    /**
     * Effectue l'attaque perforante
     * @param player Le joueur qui attaque
     * @param damageMultiplier Multiplicateur de dégâts
     * @param isThrustAttack Si c'est une attaque d'estoc (affecte toutes les cibles)
     */
    private void performPierceAttack(Player player, double damageMultiplier, boolean isThrustAttack) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        // Récupérer les stats du joueur
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        var skillManager = plugin.getSkillTreeManager();
        var momentumManager = plugin.getMomentumManager();

        // Calculer les dégâts de base
        double baseDamage = 9.0; // Dégâts de base du trident

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        baseDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        baseDamage *= (1 + damagePercent / 100.0);

        // Bonus skill tree
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        baseDamage *= (1 + skillDamageBonus / 100.0);

        // Appliquer le multiplicateur
        baseDamage *= damageMultiplier;

        // Bonus aquatique
        double aquaticMultiplier = calculateAquaticDamageBonus(player);
        baseDamage *= aquaticMultiplier;

        // Trouver les cibles dans le cône
        List<LivingEntity> targets = findPierceTargets(player, direction);

        if (targets.isEmpty()) {
            if (isThrustAttack) {
                // Animation d'estoc dans le vide
                spawnThrustParticles(player, direction, null);
            }
            return;
        }

        // Appliquer les dégâts à chaque cible avec falloff
        double currentDamage = baseDamage;
        int hitCount = 0;

        for (LivingEntity target : targets) {
            if (hitCount >= PIERCE_MAX_TARGETS) break;

            // Calculer les dégâts pour cette cible
            double targetDamage = currentDamage;
            boolean isCritical = false;

            // Système de critique
            double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
            double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
            double totalCritChance = baseCritChance + skillCritChance;

            if (random.nextDouble() * 100 < totalCritChance) {
                isCritical = true;
                double baseCritDamage = 150.0;
                double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
                double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);
                double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
                targetDamage *= critMultiplier;
            }

            // Momentum
            double momentumMultiplier = momentumManager.getDamageMultiplier(player);
            targetDamage *= momentumMultiplier;

            // Marquer comme dégâts de talent (bypass le traitement normal du CombatListener)
            target.setMetadata("zombiez_talent_damage", new FixedMetadataValue(plugin, true));
            target.setMetadata("zombiez_trident_thrust", new FixedMetadataValue(plugin, true));

            // Préparer l'indicateur de dégâts
            target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
            target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Pour les cibles secondaires, marquer comme dégâts secondaires
            if (hitCount > 0) {
                target.setMetadata("zombiez_secondary_damage", new FixedMetadataValue(plugin, true));
            }

            // Appliquer les dégâts
            target.damage(targetDamage, player);

            // DPS tracking
            DPSTracker.getInstance().recordDamage(player, targetDamage);

            // Lifesteal
            double lifestealPercent = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);
            double skillLifesteal = skillManager.getSkillBonus(player, SkillBonus.LIFESTEAL);
            double totalLifesteal = lifestealPercent + skillLifesteal;

            if (totalLifesteal > 0) {
                double healAmount = targetDamage * (totalLifesteal / 100.0);
                double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                player.setHealth(newHealth);
            }

            // Enregistrer pour le loot
            target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Mettre à jour l'affichage de vie si c'est un mob ZombieZ
            if (plugin.getZombieManager().isZombieZMob(target)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (target.isValid()) {
                        plugin.getZombieManager().updateZombieHealthDisplay(target);
                    }
                });
            }

            // Feedback visuel
            if (isCritical) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            }

            hitCount++;
            currentDamage *= PIERCE_DAMAGE_FALLOFF; // Réduire pour la prochaine cible
        }

        // Particules de perforation
        if (isThrustAttack || hitCount > 1) {
            spawnThrustParticles(player, direction, targets.isEmpty() ? null : targets.get(0).getLocation());
        }

        // Son de perforation si plusieurs cibles touchées
        if (hitCount > 1) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
    }

    /**
     * Trouve les cibles dans le cône de perforation
     */
    private List<LivingEntity> findPierceTargets(Player player, Vector direction) {
        List<LivingEntity> targets = new ArrayList<>();
        Location start = player.getEyeLocation();

        // Récupérer toutes les entités dans la portée
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
            start, PIERCE_RANGE, PIERCE_RANGE, PIERCE_RANGE
        );

        // Créer une liste triée par distance
        List<LivingEntity> potentialTargets = new ArrayList<>();

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == player) continue;
            if (entity instanceof Player) continue; // Pas de PvP avec pierce
            if (living.isDead()) continue;

            // Vérifier si l'entité est dans le cône
            Vector toEntity = entity.getLocation().add(0, 1, 0).toVector().subtract(start.toVector());
            double distance = toEntity.length();

            if (distance > PIERCE_RANGE) continue;
            if (distance < 0.5) continue; // Trop proche

            // Calculer l'angle avec la direction
            double dot = direction.dot(toEntity.normalize());
            double angle = Math.acos(Math.min(1, Math.max(-1, dot)));
            double maxAngle = Math.atan(PIERCE_WIDTH / distance);

            if (angle <= maxAngle) {
                potentialTargets.add(living);
            }
        }

        // Trier par distance
        potentialTargets.sort((a, b) -> {
            double distA = a.getLocation().distance(start);
            double distB = b.getLocation().distance(start);
            return Double.compare(distA, distB);
        });

        // Prendre les N premières cibles
        for (int i = 0; i < Math.min(PIERCE_MAX_TARGETS, potentialTargets.size()); i++) {
            targets.add(potentialTargets.get(i));
        }

        return targets;
    }

    /**
     * Affiche les particules de thrust/perforation
     */
    private void spawnThrustParticles(Player player, Vector direction, Location targetLoc) {
        Location start = player.getEyeLocation().add(0, -0.3, 0);
        double distance = targetLoc != null ? start.distance(targetLoc) : PIERCE_RANGE;

        // Ligne de particules
        for (double d = 0.5; d <= distance; d += 0.3) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0.05, 0.05, 0.05, 0);

            // Particules d'eau si bonus aquatique actif
            if (calculateAquaticDamageBonus(player) > 1.0) {
                player.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    // ==================== BONUS AQUATIQUE ====================

    /**
     * Calcule le bonus de dégâts aquatique
     */
    public double calculateAquaticDamageBonus(Player player) {
        if (isInWater(player)) {
            return WATER_DAMAGE_BONUS;
        }
        if (isInRain(player)) {
            return RAIN_DAMAGE_BONUS;
        }
        return 1.0;
    }

    /**
     * Calcule le bonus de vitesse d'attaque aquatique
     */
    public double calculateAquaticSpeedBonus(Player player) {
        if (isInWater(player)) {
            return WATER_SPEED_BONUS;
        }
        if (isInRain(player)) {
            return RAIN_SPEED_BONUS;
        }
        return 1.0;
    }

    /**
     * Vérifie si le joueur est dans l'eau
     */
    private boolean isInWater(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.WATER || player.isInWater();
    }

    /**
     * Vérifie si le joueur est sous la pluie
     */
    private boolean isInRain(Player player) {
        World world = player.getWorld();
        if (!world.hasStorm()) return false;

        Location loc = player.getLocation();
        // Vérifier si le joueur est exposé au ciel
        return world.getHighestBlockYAt(loc) <= loc.getBlockY();
    }

    // ==================== NETTOYAGE ====================

    /**
     * Reset le combo si le joueur change d'item
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem == null || newItem.getType() != Material.TRIDENT) {
            // Reset le combo quand on change d'arme
            hitCounter.remove(player.getUniqueId());
            lastHitTime.remove(player.getUniqueId());
        }
    }

    /**
     * Nettoie les données du joueur quand il se déconnecte
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    /**
     * Nettoie toutes les données d'un joueur
     */
    public void cleanupPlayer(UUID playerId) {
        hitCounter.remove(playerId);
        lastHitTime.remove(playerId);
    }

    /**
     * Obtient le nombre de hits actuels du combo
     */
    public int getHitCount(Player player) {
        return hitCounter.getOrDefault(player.getUniqueId(), 0);
    }
}
