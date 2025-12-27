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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour le système de trident amélioré ZombieZ
 *
 * Fonctionnalités:
 * - Empêche le lancer du trident (protection contre perte d'arme)
 * - Système de charge (Thrust Attack) avec clic droit maintenu
 * - Attaque perforante (Pierce) - traverse plusieurs ennemis
 * - Bonus aquatique passif (eau/pluie)
 */
public class TridentListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // ==================== CONFIGURATION ====================

    // Charge Attack
    private static final long CHARGE_TIME_FULL = 2000;        // 2 secondes pour charge complète
    private static final long CHARGE_TIME_MEDIUM = 1000;      // 1 seconde pour charge moyenne
    private static final double CHARGE_DAMAGE_MEDIUM = 1.5;   // 150% dégâts à charge moyenne
    private static final double CHARGE_DAMAGE_FULL = 2.0;     // 200% dégâts à charge complète
    private static final double CHARGE_DASH_DISTANCE = 3.0;   // Distance du dash à pleine charge
    private static final long CHARGE_COOLDOWN = 3000;         // 3 secondes de cooldown après thrust

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

    // Joueurs en train de charger leur attaque (UUID -> timestamp début charge)
    private final Map<UUID, Long> chargingPlayers = new ConcurrentHashMap<>();

    // Tâches de charge en cours (UUID -> task)
    private final Map<UUID, BukkitTask> chargeTasks = new ConcurrentHashMap<>();

    // Cooldown après thrust (UUID -> timestamp fin cooldown)
    private final Map<UUID, Long> thrustCooldowns = new ConcurrentHashMap<>();

    // Dernier niveau de charge atteint (pour feedback visuel)
    private final Map<UUID, Integer> lastChargeLevel = new ConcurrentHashMap<>();

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

    // ==================== SYSTÈME DE CHARGE ====================

    /**
     * Gère le clic droit pour commencer/relâcher la charge
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.TRIDENT) return;

        Action action = event.getAction();

        // Clic droit = commencer à charger
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // Empêcher le comportement vanilla

            // Vérifier le cooldown
            if (isOnThrustCooldown(player)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
                return;
            }

            // Commencer la charge
            startCharging(player);
        }
    }

    /**
     * Démarre la charge du trident
     */
    private void startCharging(Player player) {
        UUID playerId = player.getUniqueId();

        // Si déjà en charge, ne rien faire
        if (chargingPlayers.containsKey(playerId)) return;

        // Enregistrer le début de charge
        chargingPlayers.put(playerId, System.currentTimeMillis());
        lastChargeLevel.put(playerId, 0);

        // Son de début de charge
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 0.8f);

        // Tâche de mise à jour de la charge
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                cancelCharging(player);
                return;
            }

            // Vérifier si le joueur tient toujours un trident
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType() != Material.TRIDENT) {
                cancelCharging(player);
                return;
            }

            // Vérifier si le joueur maintient le clic droit (sneaking comme proxy ou vélocité faible)
            // On utilise isSneaking comme indicateur que le joueur "maintient" l'action
            // Alternative: on vérifie si le joueur n'a pas attaqué récemment

            long chargeTime = System.currentTimeMillis() - chargingPlayers.get(playerId);
            int chargeLevel = calculateChargeLevel(chargeTime);

            // Feedback visuel selon le niveau
            showChargeFeedback(player, chargeLevel, chargeTime);

            // Si charge complète depuis plus de 500ms, relâcher automatiquement
            if (chargeTime >= CHARGE_TIME_FULL + 500) {
                executeThrust(player, chargeLevel);
            }

        }, 2L, 2L); // Toutes les 2 ticks (100ms)

        chargeTasks.put(playerId, task);

        // Programmer le relâchement automatique si le joueur ne fait rien
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (chargingPlayers.containsKey(playerId)) {
                long chargeTime = System.currentTimeMillis() - chargingPlayers.get(playerId);
                int chargeLevel = calculateChargeLevel(chargeTime);
                executeThrust(player, chargeLevel);
            }
        }, 60L); // 3 secondes max
    }

    /**
     * Calcule le niveau de charge (0-2)
     */
    private int calculateChargeLevel(long chargeTime) {
        if (chargeTime >= CHARGE_TIME_FULL) return 2;  // Pleine charge
        if (chargeTime >= CHARGE_TIME_MEDIUM) return 1; // Charge moyenne
        return 0; // Charge faible
    }

    /**
     * Affiche le feedback visuel de charge
     */
    private void showChargeFeedback(Player player, int chargeLevel, long chargeTime) {
        UUID playerId = player.getUniqueId();
        int lastLevel = lastChargeLevel.getOrDefault(playerId, 0);

        // Particules continues
        Location loc = player.getLocation().add(0, 1, 0);

        switch (chargeLevel) {
            case 0 -> {
                // Charge faible - particules blanches discrètes
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.3, 0.3, 0.3, 0.01);
            }
            case 1 -> {
                // Charge moyenne - particules cyan
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.3, 0.3, 0.3, 0.02);
                if (lastLevel < 1) {
                    player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.6f, 1.0f);
                }
            }
            case 2 -> {
                // Pleine charge - particules intenses
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.4, 0.4, 0.4, 0.05);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.2, 0.2, 0.2, 0.03);
                if (lastLevel < 2) {
                    player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 1.2f);
                }
            }
        }

        lastChargeLevel.put(playerId, chargeLevel);
    }

    /**
     * Exécute l'attaque thrust
     */
    private void executeThrust(Player player, int chargeLevel) {
        UUID playerId = player.getUniqueId();

        // Nettoyer la charge
        cancelCharging(player);

        // Si charge trop faible, annuler
        if (chargeLevel == 0) {
            return;
        }

        // Appliquer le cooldown
        thrustCooldowns.put(playerId, System.currentTimeMillis() + CHARGE_COOLDOWN);

        // Calculer le multiplicateur de dégâts
        double damageMultiplier = chargeLevel == 2 ? CHARGE_DAMAGE_FULL : CHARGE_DAMAGE_MEDIUM;

        // Dash si pleine charge
        if (chargeLevel == 2) {
            performDash(player);
        }

        // Son d'attaque
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        // Effectuer l'attaque perforante avec le bonus de charge
        performPierceAttack(player, damageMultiplier, true);
    }

    /**
     * Effectue le dash vers l'avant
     */
    private void performDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        Vector dashVelocity = direction.multiply(CHARGE_DASH_DISTANCE * 0.5);
        dashVelocity.setY(Math.max(0.1, dashVelocity.getY() * 0.3)); // Limiter le Y

        player.setVelocity(player.getVelocity().add(dashVelocity));

        // Particules de dash
        Location loc = player.getLocation();
        for (int i = 0; i < 10; i++) {
            Location particleLoc = loc.clone().add(direction.clone().multiply(-i * 0.3));
            player.getWorld().spawnParticle(Particle.CLOUD, particleLoc.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /**
     * Annule la charge en cours
     */
    private void cancelCharging(Player player) {
        UUID playerId = player.getUniqueId();

        chargingPlayers.remove(playerId);
        lastChargeLevel.remove(playerId);

        BukkitTask task = chargeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    // ==================== ATTAQUE PERFORANTE ====================

    /**
     * Intercepte les attaques mêlée avec trident pour appliquer le pierce
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onTridentMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.TRIDENT) return;

        // Vérifier si c'est une attaque normale (pas une attaque chargée)
        // Les attaques chargées sont gérées séparément
        if (event.getEntity().hasMetadata("zombiez_trident_thrust")) return;

        // Appliquer le bonus aquatique aux dégâts de base
        double aquaticMultiplier = calculateAquaticDamageBonus(player);
        if (aquaticMultiplier > 1.0) {
            event.setDamage(event.getDamage() * aquaticMultiplier);

            // Particules d'eau
            if (isInWater(player)) {
                player.getWorld().spawnParticle(Particle.BUBBLE_POP,
                    event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            } else {
                player.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                    event.getEntity().getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }

        // Marquer comme attaque trident pour éviter double-traitement
        event.getEntity().setMetadata("zombiez_trident_attack", new FixedMetadataValue(plugin, true));

        // Programmer l'attaque perforante après que les dégâts initiaux soient appliqués
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            performPierceAttack(player, 1.0, false);
        });
    }

    /**
     * Effectue l'attaque perforante
     * @param player Le joueur qui attaque
     * @param chargeMultiplier Multiplicateur de charge (1.0 pour normal, jusqu'à 2.0 pour charge)
     * @param isChargedAttack Si c'est une attaque chargée (affecte toutes les cibles)
     */
    private void performPierceAttack(Player player, double chargeMultiplier, boolean isChargedAttack) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        // Récupérer les stats du joueur
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        var skillManager = plugin.getSkillTreeManager();
        var momentumManager = plugin.getMomentumManager();

        // Calculer les dégâts de base
        ItemStack weapon = player.getInventory().getItemInMainHand();
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

        // Appliquer le multiplicateur de charge
        baseDamage *= chargeMultiplier;

        // Bonus aquatique
        double aquaticMultiplier = calculateAquaticDamageBonus(player);
        baseDamage *= aquaticMultiplier;

        // Trouver les cibles dans le cône
        List<LivingEntity> targets = findPierceTargets(player, direction);

        if (targets.isEmpty()) {
            if (isChargedAttack) {
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
            if (hitCount > 0 && !isChargedAttack) break; // Attaque normale = 1 cible seulement en pierce auto

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
        if (isChargedAttack || hitCount > 1) {
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

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si le joueur est en cooldown de thrust
     */
    private boolean isOnThrustCooldown(Player player) {
        Long cooldownEnd = thrustCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Obtient le temps de cooldown restant
     */
    private long getThrustCooldownRemaining(Player player) {
        Long cooldownEnd = thrustCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    // ==================== NETTOYAGE ====================

    /**
     * Annule la charge si le joueur change d'item
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem == null || newItem.getType() != Material.TRIDENT) {
            if (chargingPlayers.containsKey(player.getUniqueId())) {
                cancelCharging(player);
            }
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
        chargingPlayers.remove(playerId);
        lastChargeLevel.remove(playerId);
        thrustCooldowns.remove(playerId);

        BukkitTask task = chargeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Vérifie si un joueur est en train de charger
     */
    public boolean isCharging(Player player) {
        return chargingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Obtient le temps de charge actuel d'un joueur
     */
    public long getChargeTime(Player player) {
        Long startTime = chargingPlayers.get(player.getUniqueId());
        if (startTime == null) return 0;
        return System.currentTimeMillis() - startTime;
    }
}
