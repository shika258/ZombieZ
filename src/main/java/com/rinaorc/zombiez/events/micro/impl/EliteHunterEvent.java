package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.PacketDamageIndicator;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import com.rinaorc.zombiez.zones.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Fuyeur Doré - Un zombie doré fuit le joueur
 *
 * Mecanique:
 * - Un zombie doré spawn a distance et FUIT le joueur
 * - Le joueur doit le traquer et le tuer avant qu'il ne s'echappe
 * - Le zombie laisse une trainee de particules dorées
 * - Recompenses augmentent si tue rapidement
 * - Applique les dégâts ZombieZ (stats, crits, etc.)
 */
public class EliteHunterEvent extends MicroEvent {

    private Zombie eliteZombie;
    private UUID eliteUUID;
    private int ticksSinceLastParticle = 0;
    private boolean killed = false;
    private long killTime = 0;
    private final Random random = new Random();

    // TextDisplay flottant au-dessus du zombie
    private TextDisplay healthDisplay;

    // Configuration
    private static final double BASE_ELITE_HEALTH = 80.0;
    private static final double HEALTH_PER_ZONE = 15.0; // +15 HP par zone
    private static final double ELITE_SPEED = 0.32; // Vitesse augmentee pour fuir efficacement
    private static final double SPAWN_DISTANCE = 15.0; // Distance de spawn proche du joueur
    private static final int PARTICLE_INTERVAL = 5; // Particules toutes les 5 ticks
    private static final float TEXT_SCALE = 1.3f;

    // HP calcule dynamiquement selon la zone
    private double eliteMaxHealth;

    public EliteHunterEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.ELITE_HUNTER, player, location, zone);
        // Calculer les HP selon la zone (zone 1 = 95 HP, zone 10 = 230 HP, etc.)
        this.eliteMaxHealth = BASE_ELITE_HEALTH + (zone.getId() * HEALTH_PER_ZONE);
    }

    @Override
    protected void onStart() {
        // Calculer une position de spawn proche du joueur (15 blocs)
        Location playerLoc = player.getLocation();
        double angle = Math.random() * Math.PI * 2;
        Location spawnLoc = playerLoc.clone().add(
            Math.cos(angle) * SPAWN_DISTANCE,
            0,
            Math.sin(angle) * SPAWN_DISTANCE
        );
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        // Spawn le zombie elite
        eliteZombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        eliteUUID = eliteZombie.getUniqueId();
        registerEntity(eliteZombie);

        // Configuration du zombie - plus intelligent et menaçant
        eliteZombie.setCustomName("§6§l✦ Fuyeur Doré §7[§e" + (int) eliteMaxHealth + "§c❤§7]");
        eliteZombie.setCustomNameVisible(true);
        eliteZombie.setBaby(false);
        eliteZombie.setShouldBurnInDay(false);

        // Stats - HP scales avec la zone, vitesse elevee pour fuir
        eliteZombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(eliteMaxHealth);
        eliteZombie.setHealth(eliteMaxHealth);
        eliteZombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(ELITE_SPEED);
        eliteZombie.getAttribute(Attribute.ARMOR).setBaseValue(8.0); // Moins résistant pour faciliter le kill
        eliteZombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.3); // Moins de résistance au knockback

        // Garder l'IA active pour le pathfinding mais sans cible d'attaque
        eliteZombie.setAware(true);
        eliteZombie.setTarget(null); // Pas de cible à attaquer

        // Equipement distinctif (armure en or)
        eliteZombie.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        eliteZombie.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        eliteZombie.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
        eliteZombie.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
        eliteZombie.getEquipment().setHelmetDropChance(0f);
        eliteZombie.getEquipment().setChestplateDropChance(0f);
        eliteZombie.getEquipment().setLeggingsDropChance(0f);
        eliteZombie.getEquipment().setBootsDropChance(0f);

        // Effets visuels - glowing doré + bonus de vitesse pour fuir
        eliteZombie.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        eliteZombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));

        // Tag pour identification
        eliteZombie.addScoreboardTag("micro_event_entity");
        eliteZombie.addScoreboardTag("elite_hunter");
        eliteZombie.addScoreboardTag("event_" + id);

        // Creer le TextDisplay flottant au-dessus du zombie
        Location displayLoc = spawnLoc.clone().add(0, 2.8, 0);
        healthDisplay = displayLoc.getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(180, 0, 0, 0));
            display.setTransformation(new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                new org.joml.Quaternionf(),
                new Vector3f(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE),
                new org.joml.Quaternionf()
            ));
            display.text(createHealthDisplay(eliteMaxHealth, eliteMaxHealth));
        });
        registerEntity(healthDisplay);

        // Effet de spawn
        spawnLoc.getWorld().spawnParticle(Particle.SMOKE, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    /**
     * Cree le Component pour l'affichage de vie
     */
    private Component createHealthDisplay(double currentHealth, double maxHealth) {
        double healthPercent = currentHealth / maxHealth;
        NamedTextColor healthColor = healthPercent > 0.5 ? NamedTextColor.GREEN :
            (healthPercent > 0.25 ? NamedTextColor.YELLOW : NamedTextColor.RED);

        // Barre de vie visuelle
        int barLength = 10;
        int filled = (int) (healthPercent * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }

        return Component.text("✦ FUYEUR DORÉ ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
            .appendNewline()
            .append(Component.text(bar.toString(), healthColor))
            .append(Component.text(" " + (int) currentHealth + "❤", healthColor));
    }

    @Override
    protected void tick() {
        if (eliteZombie == null || eliteZombie.isDead()) {
            if (killed) {
                complete();
            } else {
                fail();
            }
            return;
        }

        // Mettre a jour le TextDisplay (suit le zombie et affiche la vie)
        if (healthDisplay != null && healthDisplay.isValid()) {
            healthDisplay.teleport(eliteZombie.getLocation().add(0, 2.8, 0));
            healthDisplay.text(createHealthDisplay(eliteZombie.getHealth(), eliteMaxHealth));
        }

        // Faire fuir le zombie LOIN du joueur (toutes les 5 ticks pour optimisation)
        if (elapsedTicks % 5 == 0) {
            makeZombieFlee();
        }

        // Spawn des particules de trainee (toutes les 10 ticks = 0.5s)
        ticksSinceLastParticle++;
        if (ticksSinceLastParticle >= 10) {
            ticksSinceLastParticle = 0;
            spawnTrailParticles();
        }

        // Verifier si le zombie est trop loin (echec)
        double distance = eliteZombie.getLocation().distance(player.getLocation());
        if (distance > 80) {
            // Le zombie s'est echappe!
            player.sendMessage("§c§l✗ §7Le Fuyeur Doré s'est echappé!");
            fail();
            return;
        }

        // ActionBar avec infos
        String direction = getDirectionToTarget();
        sendActionBar("§6✦ Fuyeur Doré §7| Distance: §e" + (int) distance + "m " + direction +
            " §7| Temps: §e" + getRemainingTimeSeconds() + "s");

        // Le nom du zombie n'est plus necessaire car on utilise le TextDisplay
        eliteZombie.setCustomNameVisible(false);
    }

    /**
     * Fait fuir le zombie loin du joueur - Navigation intelligente
     */
    private void makeZombieFlee() {
        if (eliteZombie == null || !eliteZombie.isValid() || player == null) return;

        Location zombieLoc = eliteZombie.getLocation();
        Location playerLoc = player.getLocation();

        // S'assurer que le zombie n'a pas de cible d'attaque
        eliteZombie.setTarget(null);

        // Direction opposee au joueur
        Vector fleeDirection = zombieLoc.toVector().subtract(playerLoc.toVector());

        // Eviter le cas ou le joueur est exactement sur le zombie
        if (fleeDirection.lengthSquared() < 0.1) {
            fleeDirection = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
        }
        fleeDirection.normalize();

        // Ajouter un peu de randomisation pour un comportement plus naturel
        fleeDirection.add(new Vector(
            (Math.random() - 0.5) * 0.4,
            0,
            (Math.random() - 0.5) * 0.4
        )).normalize();

        // Calculer la destination (12 blocs pour une fuite plus efficace)
        Location targetLoc = zombieLoc.clone().add(fleeDirection.clone().multiply(12));

        // Ajuster Y au sol pour eviter les bugs de navigation
        int highestY = targetLoc.getWorld().getHighestBlockYAt(targetLoc);
        targetLoc.setY(highestY + 1);

        // Verifier que la destination est valide (pas dans un mur)
        if (!targetLoc.getBlock().isPassable()) {
            // Essayer de trouver une position valide
            targetLoc.setY(highestY + 2);
        }

        // Utiliser le Pathfinder natif de Paper pour une navigation fluide
        try {
            boolean pathFound = eliteZombie.getPathfinder().moveTo(targetLoc, ELITE_SPEED * 1.5);

            // Si pas de chemin trouve, utiliser velocite directe
            if (!pathFound) {
                applyFleeVelocity(fleeDirection);
            }
        } catch (Exception e) {
            // Fallback: utiliser velocite directe si pathfinder echoue
            applyFleeVelocity(fleeDirection);
        }

        // Faire regarder dans la direction de fuite
        float yaw = (float) Math.toDegrees(Math.atan2(-fleeDirection.getX(), fleeDirection.getZ()));
        eliteZombie.setRotation(yaw, 0);
    }

    /**
     * Applique une velocite de fuite directe au zombie
     */
    private void applyFleeVelocity(Vector fleeDirection) {
        if (eliteZombie == null) return;

        // Calculer la velocite en gardant un peu de la velocite actuelle pour fluidite
        Vector currentVel = eliteZombie.getVelocity();
        Vector targetVel = fleeDirection.clone().multiply(ELITE_SPEED * 1.5);

        // Garder la composante Y pour la gravite
        targetVel.setY(currentVel.getY());

        // Ajouter un petit boost vers le haut si le zombie est bloque
        if (eliteZombie.isOnGround() && currentVel.lengthSquared() < 0.01) {
            targetVel.setY(0.2);
        }

        eliteZombie.setVelocity(targetVel);
    }

    /**
     * Spawn les particules de trainee dorées
     */
    private void spawnTrailParticles() {
        if (eliteZombie == null || eliteZombie.isDead()) return;

        Location loc = eliteZombie.getLocation().add(0, 0.5, 0);
        // Particules dorées pour le Fuyeur Doré
        loc.getWorld().spawnParticle(Particle.DUST, loc, 4, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.0f)); // Doré
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.1, 0.1, 0.1, 0.01);
    }

    /**
     * Obtient la direction vers la cible
     */
    private String getDirectionToTarget() {
        if (eliteZombie == null) return "";

        Location playerLoc = player.getLocation();
        Location targetLoc = eliteZombie.getLocation();

        double dx = targetLoc.getX() - playerLoc.getX();
        double dz = targetLoc.getZ() - playerLoc.getZ();

        if (Math.abs(dz) > Math.abs(dx)) {
            return dz < 0 ? "§b↑Nord" : "§c↓Sud";
        } else {
            return dx > 0 ? "§e→Est" : "§6←Ouest";
        }
    }

    @Override
    protected void onCleanup() {
        // Nettoyer le TextDisplay
        if (healthDisplay != null && healthDisplay.isValid()) {
            healthDisplay.remove();
        }
        healthDisplay = null;

        if (eliteZombie != null && !eliteZombie.isDead()) {
            // Effet de disparition
            Location loc = eliteZombie.getLocation();
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.3, 0.5, 0.3, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        }
        eliteZombie = null;
        eliteUUID = null;
    }

    @Override
    protected int getBonusPoints() {
        if (!killed) return 0;

        // Bonus basé sur la rapidité du kill
        long timeToKill = killTime - startTime;
        int secondsToKill = (int) (timeToKill / 1000);

        if (secondsToKill < 10) return 300;      // Tres rapide: +300
        if (secondsToKill < 20) return 200;      // Rapide: +200
        if (secondsToKill < 30) return 100;      // Normal: +100
        return 50;                                // Lent: +50
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (!entity.getUniqueId().equals(eliteUUID)) {
            return false;
        }

        // ============ CALCUL DES DÉGÂTS ZOMBIEZ ============
        double finalDamage = damage;
        boolean isCritical = false;

        // 1. STATS D'ÉQUIPEMENT
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(attacker);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // 2. SKILL TREE BONUSES
        var skillManager = plugin.getSkillTreeManager();
        double skillDamageBonus = skillManager.getSkillBonus(attacker, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // 3. SYSTÈME DE CRITIQUE
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(attacker, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (random.nextDouble() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0;
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(attacker, SkillBonus.CRIT_DAMAGE);
            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // 4. MOMENTUM SYSTEM
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(attacker);
        finalDamage *= momentumMultiplier;

        // 5. EXECUTE DAMAGE (<20% HP)
        double mobHealthPercent = entity.getHealth() / entity.getMaxHealth() * 100;
        double executeThreshold = playerStats.getOrDefault(StatType.EXECUTE_THRESHOLD, 20.0);
        if (mobHealthPercent <= executeThreshold) {
            double executeBonus = playerStats.getOrDefault(StatType.EXECUTE_DAMAGE, 0.0);
            double skillExecuteBonus = skillManager.getSkillBonus(attacker, SkillBonus.EXECUTE_DAMAGE);
            finalDamage *= (1 + (executeBonus + skillExecuteBonus) / 100.0);
        }

        // 6. LIFESTEAL
        double lifestealPercent = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);
        double skillLifesteal = skillManager.getSkillBonus(attacker, SkillBonus.LIFESTEAL);
        double totalLifesteal = lifestealPercent + skillLifesteal;
        if (totalLifesteal > 0) {
            double healAmount = finalDamage * (totalLifesteal / 100.0);
            double newHealth = Math.min(attacker.getHealth() + healAmount, attacker.getMaxHealth());
            attacker.setHealth(newHealth);
            if (healAmount > 1) {
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);
            }
        }

        // ============ APPLIQUER LES DÉGÂTS DIRECTEMENT À LA SANTÉ ============
        // (évite la récursion avec entity.damage() qui déclenche un nouvel événement)
        double newHealth = Math.max(0, entity.getHealth() - finalDamage);
        entity.setHealth(newHealth);

        // Effet de dégâts (animation rouge)
        entity.playHurtAnimation(0);

        // ============ AFFICHER L'INDICATEUR DE DÉGÂTS ============
        PacketDamageIndicator.display(plugin, entity.getLocation().add(0, 1, 0), finalDamage, isCritical, attacker);

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        } else {
            // Particules dorées pour le hit normal
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.2f));
        }

        // ============ METTRE À JOUR L'AFFICHAGE DE VIE ============
        if (healthDisplay != null && healthDisplay.isValid() && entity.isValid()) {
            healthDisplay.text(createHealthDisplay(newHealth, eliteMaxHealth));
        }

        // ============ GÉRER LA MORT SI NÉCESSAIRE ============
        if (newHealth <= 0) {
            // Le mob meurt - déclencher handleDeath manuellement via le scheduler
            // pour éviter les problèmes de timing
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    entity.setHealth(0); // Force la mort
                }
            });
        }

        // Retourner true pour annuler les dégâts vanilla (on a déjà appliqué les dégâts manuellement)
        return true;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (entity.getUniqueId().equals(eliteUUID)) {
            killed = true;
            killTime = System.currentTimeMillis();

            // Effet de mort spectaculaire
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.SOUL, loc, 20, 0.5, 1, 0.5, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.5f);

            // Annonce
            long timeToKill = killTime - startTime;
            player.sendMessage("§a§l✓ §7Fuyeur Doré éliminé en §e" + String.format("%.1f", timeToKill / 1000.0) + "s§7!");

            return true;
        }
        return false;
    }
}
