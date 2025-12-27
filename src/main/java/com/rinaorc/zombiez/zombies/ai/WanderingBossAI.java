package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IA pour le Boss Errant (Wandering Boss)
 *
 * Comportement spécifique:
 * - Se déplace vers une destination fixe
 * - Cible les joueurs proches pour les attaquer
 * - Utilise des capacités spéciales périodiques
 * - Spawne des adds
 * - Mode enragé à 30% de vie
 */
public class WanderingBossAI extends ZombieAI {

    // Destination du boss
    @Getter @Setter
    private Location destination;

    @Getter
    private double totalDistance = 0;

    // Vitesse de déplacement vers la destination (blocs par tick d'IA)
    @Getter @Setter
    private double moveSpeed = 0.15;

    // État
    @Getter
    private boolean isEnraged = false;

    // Timers (en ticks d'IA, 1 tick IA = 3 ticks Minecraft)
    private int tickCounter = 0;
    private int specialAbilityTimer = 0;
    private int addSpawnTimer = 0;
    private int targetingTimer = 0;

    // Intervalles (en ticks d'IA)
    private int specialAbilityInterval = 30; // ~4.5 secondes
    private int addSpawnInterval = 40;       // ~6 secondes

    // Joueurs dans le combat
    private final Set<UUID> playersInFight = new HashSet<>();

    // Nom custom du boss (pour les messages)
    @Getter @Setter
    private String bossName = "Boss Errant";

    public WanderingBossAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 8000;

        applyBossBuffs();
    }

    /**
     * Applique les buffs de boss
     */
    private void applyBossBuffs() {
        // Résistance au knockback
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.8);
        }

        // Effets permanents
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    /**
     * Définit la destination et calcule la distance totale
     */
    public void setDestinationWithDistance(Location dest) {
        this.destination = dest;
        if (zombie != null && zombie.isValid()) {
            this.totalDistance = zombie.getLocation().distance(dest);
        }
    }

    @Override
    public void tick() {
        if (!zombie.isValid() || zombie.isDead()) return;

        tickCounter++;

        // Vérifier le mode enragé
        if (!isEnraged && isHealthBelow(0.30)) {
            triggerEnrage();
        }

        // Mouvement vers la destination
        moveTowardsDestination();

        // Ciblage des joueurs (toutes les 4 ticks IA = ~0.6 secondes)
        targetingTimer++;
        if (targetingTimer >= 4) {
            targetNearestPlayer();
            updatePlayersInFight();
            targetingTimer = 0;
        }

        // Capacités spéciales
        specialAbilityTimer++;
        if (specialAbilityTimer >= specialAbilityInterval && canUseAbility()) {
            useSpecialAbility();
            specialAbilityTimer = 0;
            useAbility();
        }

        // Spawn d'adds
        addSpawnTimer++;
        if (addSpawnTimer >= addSpawnInterval) {
            spawnAdds();
            addSpawnTimer = 0;
        }

        // Particules ambiantes
        if (tickCounter % 4 == 0) {
            Location bossLoc = zombie.getLocation();
            playParticles(Particle.SOUL_FIRE_FLAME, bossLoc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5);

            if (isEnraged) {
                playParticles(Particle.ANGRY_VILLAGER, bossLoc.clone().add(0, 2, 0), 3, 0.3, 0.3, 0.3);
            }
        }
    }

    /**
     * Déplace le boss vers sa destination en utilisant le pathfinding natif
     */
    private void moveTowardsDestination() {
        if (destination == null || !zombie.isValid()) return;

        Location currentLoc = zombie.getLocation();
        World world = currentLoc.getWorld();
        if (world == null || !world.equals(destination.getWorld())) return;

        double distToDestination = currentLoc.distance(destination);

        // Si arrivé à destination, ne plus bouger
        if (distToDestination < 3) return;

        // Utiliser le pathfinding natif de Minecraft (meilleur comportement)
        if (zombie instanceof Mob mob) {
            // Si pas de cible ou cible trop loin, pathfind vers la destination
            if (mob.getTarget() == null ||
                currentLoc.distance(mob.getTarget().getLocation()) > 15) {

                // Créer un point intermédiaire vers la destination
                Vector direction = destination.toVector().subtract(currentLoc.toVector()).normalize();
                double speed = isEnraged ? moveSpeed * 1.5 : moveSpeed;

                Location nextPoint = currentLoc.clone().add(direction.multiply(speed * 5));

                // Ajuster la hauteur au sol
                int groundY = world.getHighestBlockYAt(nextPoint);
                nextPoint.setY(groundY + 1);

                // Utiliser le pathfinder natif
                mob.getPathfinder().moveTo(nextPoint, isEnraged ? 1.3 : 1.0);
            }
        }
    }

    /**
     * Cible le joueur le plus proche
     */
    private void targetNearestPlayer() {
        if (!(zombie instanceof Mob mob)) return;

        World world = zombie.getWorld();
        Player nearestPlayer = null;
        double nearestDistance = 25; // Rayon de détection

        for (Entity entity : world.getNearbyEntities(zombie.getLocation(), 25, 10, 25)) {
            if (entity instanceof Player player && !player.isDead()) {
                double distance = zombie.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        // Cibler le joueur le plus proche s'il est assez proche
        if (nearestPlayer != null && nearestDistance < 20) {
            mob.setTarget(nearestPlayer);
            currentTarget = nearestPlayer;
        }
    }

    /**
     * Met à jour la liste des joueurs dans le combat
     */
    private void updatePlayersInFight() {
        World world = zombie.getWorld();

        // Ajouter les joueurs proches
        for (Entity entity : world.getNearbyEntities(zombie.getLocation(), 50, 30, 50)) {
            if (entity instanceof Player player) {
                playersInFight.add(player.getUniqueId());
            }
        }

        // Retirer les joueurs trop loin ou déconnectés
        playersInFight.removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            return p == null || !p.isOnline() ||
                   p.getLocation().distance(zombie.getLocation()) > 80;
        });
    }

    /**
     * Active le mode enragé
     */
    private void triggerEnrage() {
        isEnraged = true;

        // Annonce aux joueurs
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§4§l⚠ " + bossName.toUpperCase() + " EST ENRAGÉ!",
                           "§cIl devient plus dangereux!", 10, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
            }
        }

        // Effets visuels
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 1, 1, 1);
        playSound(Sound.ENTITY_WARDEN_ROAR, 2f, 0.7f);

        // Boost de stats
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.5);
        }

        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.3);
        }

        // Effets de potion
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, true));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, true));

        // Réduire les intervalles d'attaque
        specialAbilityInterval = 20;
        addSpawnInterval = 25;
    }

    /**
     * Utilise une capacité spéciale aléatoire
     */
    private void useSpecialAbility() {
        int ability = random.nextInt(4);

        switch (ability) {
            case 0 -> shockwave();
            case 1 -> terrifyingRoar();
            case 2 -> groundSlam();
            case 3 -> selfRegeneration();
        }
    }

    /**
     * Onde de choc - repousse les joueurs proches
     */
    private void shockwave() {
        Location bossLoc = zombie.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        playSound(Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        playParticles(Particle.EXPLOSION, bossLoc, 20, 3, 1, 3);

        for (Entity entity : world.getNearbyEntities(bossLoc, 8, 4, 8)) {
            if (entity instanceof Player player) {
                Vector knockback = player.getLocation().toVector()
                    .subtract(bossLoc.toVector()).normalize().multiply(2).setY(0.8);
                player.setVelocity(knockback);
                player.damage(8 + level / 5.0, zombie);
                player.sendMessage("§c" + bossName + " §7vous repousse!");
            }
        }
    }

    /**
     * Rugissement terrifiant - applique des effets négatifs
     */
    private void terrifyingRoar() {
        Location bossLoc = zombie.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        playSound(Sound.ENTITY_WARDEN_ROAR, 2f, 0.6f);
        playParticles(Particle.SONIC_BOOM, bossLoc.add(0, 1, 0), 3, 1, 0.5, 1);

        for (Entity entity : world.getNearbyEntities(bossLoc, 15, 10, 15)) {
            if (entity instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                player.sendMessage("§c" + bossName + " §7vous terrorise!");
            }
        }
    }

    /**
     * Frappe au sol - dégâts et projection vers le haut
     */
    private void groundSlam() {
        Location bossLoc = zombie.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        playParticles(Particle.BLOCK, bossLoc, 50, 3, 0.5, 3,
                     org.bukkit.Material.DIRT);

        for (Entity entity : world.getNearbyEntities(bossLoc, 6, 4, 6)) {
            if (entity instanceof Player player) {
                player.damage(15 + level / 3.0, zombie);
                player.setVelocity(new Vector(0, 1, 0));
            }
        }
    }

    /**
     * Auto-régénération - soigne le boss
     */
    private void selfRegeneration() {
        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;

        double healAmount = maxHealth.getValue() * 0.05; // 5% de regen
        zombie.setHealth(Math.min(maxHealth.getValue(), zombie.getHealth() + healAmount));

        playSound(Sound.ENTITY_WITCH_DRINK, 1f, 0.8f);
        playParticles(Particle.HEART, zombie.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§c" + bossName + " §7se régénère!");
            }
        }
    }

    /**
     * Spawne des zombies supplémentaires
     */
    private void spawnAdds() {
        World world = zombie.getWorld();
        Location bossLoc = zombie.getLocation();

        int addCount = 2 + level / 15;

        for (int i = 0; i < addCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3 + random.nextDouble() * 3;
            double x = bossLoc.getX() + Math.cos(angle) * distance;
            double z = bossLoc.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);

            // Utiliser le SpawnSystem pour spawner les adds
            if (plugin.getSpawnSystem() != null) {
                plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, level);
            }
        }

        playSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.8f);
    }

    /**
     * Calcule la distance restante jusqu'à la destination
     */
    public double getDistanceToDestination() {
        if (destination == null || !zombie.isValid()) return Double.MAX_VALUE;

        Location currentLoc = zombie.getLocation();
        if (!currentLoc.getWorld().equals(destination.getWorld())) return Double.MAX_VALUE;

        return currentLoc.distance(destination);
    }

    /**
     * Vérifie si le boss a atteint sa destination
     */
    public boolean hasReachedDestination() {
        return getDistanceToDestination() < 10;
    }

    /**
     * Retourne les joueurs impliqués dans le combat
     */
    public Set<UUID> getPlayersInFight() {
        return new HashSet<>(playersInFight);
    }

    @Override
    public void onAttack(Player target) {
        if (target.isDead()) return;

        currentTarget = target;
        playersInFight.add(target.getUniqueId());

        // Dégâts bonus de boss
        double bonusDamage = 5 + level / 10.0;
        target.damage(bonusDamage, zombie);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Ajouter l'attaquant aux participants
        if (attacker instanceof Player player) {
            playersInFight.add(player.getUniqueId());

            // Contre-attaque occasionnelle
            if (random.nextFloat() < 0.1f) {
                player.damage(3, zombie);
                playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Effets de mort épiques
        playSound(Sound.ENTITY_WITHER_DEATH, 2f, 1f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 5, 2, 2, 2);
        playParticles(Particle.TOTEM_OF_UNDYING, zombie.getLocation(), 100, 3, 3, 3);

        // Message de victoire
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§a§lVICTOIRE!", "§7" + bossName + " vaincu!", 10, 60, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }
}
