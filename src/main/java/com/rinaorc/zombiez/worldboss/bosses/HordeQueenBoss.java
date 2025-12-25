package com.rinaorc.zombiez.worldboss.bosses;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.WorldBoss;
import com.rinaorc.zombiez.worldboss.WorldBossType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La Reine de la Horde (Invocatrice - Taille x4)
 *
 * Capacité: Invoque 5 zombies rapides tous les 25% de vie perdus. Invincible tant que sbires en vie.
 * Stratégie: Focus les adds avant le boss.
 *
 * Mécaniques:
 * - Invoque des zombies à 75%, 50%, 25% de vie
 * - Devient invincible tant que les zombies invoqués sont vivants
 * - Les zombies invoqués sont rapides et agressifs
 * - Aura qui buff les zombies proches
 */
public class HordeQueenBoss extends WorldBoss {

    // État d'invincibilité
    private volatile boolean isInvincible = false;

    // Tracking des sbires (thread-safe pour accès depuis tick() et cleanup())
    private final Set<UUID> activeMinions = ConcurrentHashMap.newKeySet();

    // Seuils de vie pour invocation (75%, 50%, 25%)
    private final boolean[] invocationTriggered = {false, false, false};
    private final double[] invocationThresholds = {0.75, 0.50, 0.25};

    // Configuration
    private static final int MINIONS_PER_WAVE = 5;
    private static final double MINION_SPEED_BONUS = 0.15;

    public HordeQueenBoss(ZombieZPlugin plugin, int zoneId) {
        super(plugin, WorldBossType.HORDE_QUEEN, zoneId);
    }

    @Override
    protected void useAbility() {
        // La Reine n'a pas d'ability sur cooldown
        // Elle invoque uniquement quand elle perd de la vie
    }

    @Override
    public boolean canReceiveDamage() {
        return !isInvincible;
    }

    @Override
    protected void onDamageReceived(Player attacker, double damage) {
        if (entity == null || !entity.isValid()) return;

        // Feedback si invincible (les dégâts sont déjà annulés par le listener)
        if (isInvincible) {
            attacker.sendMessage("§5" + type.getDisplayName() + " §7est §d§lINVINCIBLE§7! Tuez ses sbires d'abord!");
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);

            // Particules de bouclier
            Location bossLoc = entity.getLocation().add(0, 2, 0);
            entity.getWorld().spawnParticle(Particle.WITCH, bossLoc, 10, 1, 1, 1, 0);
            return;
        }

        // Vérifier les seuils d'invocation
        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;

        double healthPercent = entity.getHealth() / maxHealth.getValue();

        for (int i = 0; i < invocationThresholds.length; i++) {
            if (!invocationTriggered[i] && healthPercent <= invocationThresholds[i]) {
                invocationTriggered[i] = true;
                summonMinions(i + 1);
                break;
            }
        }
    }

    /**
     * Invoque une vague de sbires
     */
    private void summonMinions(int waveNumber) {
        if (entity == null || !entity.isValid()) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Devenir invincible
        isInvincible = true;

        // Effets d'invocation
        world.playSound(bossLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 0.8f);
        world.playSound(bossLoc, Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);
        world.spawnParticle(Particle.WITCH, bossLoc.clone().add(0, 2, 0), 50, 2, 2, 2, 0.1);

        // Annonce
        for (Player player : getNearbyPlayers(40)) {
            player.sendTitle("§5§lINVOCATION!", "§7La Reine invoque sa horde!", 10, 40, 10);
            player.sendMessage("§5" + type.getDisplayName() + " §7invoque §c" + MINIONS_PER_WAVE + " sbires§7!");
            player.sendMessage("§7Elle est §d§lINVINCIBLE §7tant que ses sbires sont en vie!");
        }

        // Invoquer les sbires avec un délai
        for (int i = 0; i < MINIONS_PER_WAVE; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active || entity == null) return;
                    spawnMinion(bossLoc, index);
                }
            }.runTaskLater(plugin, i * 5L);
        }

        // Mise à jour de la boss bar
        updateBossBar();
    }

    /**
     * Spawn un sbire
     */
    private void spawnMinion(Location center, int index) {
        World world = center.getWorld();
        if (world == null) return;

        // Position autour du boss
        double angle = (2 * Math.PI / MINIONS_PER_WAVE) * index;
        double distance = 4 + Math.random() * 2;
        Location spawnLoc = center.clone().add(
            Math.cos(angle) * distance,
            0,
            Math.sin(angle) * distance
        );
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

        // Créer le zombie
        Zombie minion = world.spawn(spawnLoc, Zombie.class, zombie -> {
            zombie.setAdult();
            zombie.setCanPickupItems(false);
            zombie.setRemoveWhenFarAway(false);
            zombie.setPersistent(true);

            // Nom
            zombie.setCustomName("§5Sbire de la Reine");
            zombie.setCustomNameVisible(true);

            // Stats (zombies rapides)
            var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(50 + zoneId * 2);
                zombie.setHealth(maxHealth.getValue());
            }

            var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.3 + MINION_SPEED_BONUS);
            }

            var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(8 + zoneId * 0.3);
            }

            // Effets
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            zombie.setGlowing(true);

            // Tags
            zombie.addScoreboardTag("queen_minion");
            zombie.addScoreboardTag("minion_of_" + bossId.toString());
        });

        // Tracker le sbire
        activeMinions.add(minion.getUniqueId());

        // Effets de spawn
        world.spawnParticle(Particle.WITCH, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1.2f);
    }

    @Override
    protected void tick() {
        // Appliquer les effets procéduraux des traits
        super.tick();

        if (entity == null || !entity.isValid()) return;

        // Nettoyer les sbires morts et vérifier l'invincibilité
        activeMinions.removeIf(uuid -> {
            Entity e = plugin.getServer().getEntity(uuid);
            return e == null || !e.isValid() || (e instanceof LivingEntity le && le.isDead());
        });

        // Fin de l'invincibilité si tous les sbires sont morts
        if (isInvincible && activeMinions.isEmpty()) {
            endInvincibility();
        }

        // Aura qui buff les zombies proches
        if (Math.random() < 0.1) { // 10% chance par tick
            buffNearbyZombies();
        }

        // Particules royales
        Location loc = entity.getLocation().add(0, 3, 0);
        World world = loc.getWorld();
        if (world != null) {
            // Couronne de particules
            for (int i = 0; i < 8; i++) {
                double angle = (System.currentTimeMillis() / 100.0 + i * Math.PI / 4) % (2 * Math.PI);
                Location crownLoc = loc.clone().add(Math.cos(angle) * 0.8, 0.3, Math.sin(angle) * 0.8);
                world.spawnParticle(Particle.END_ROD, crownLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Fin de l'invincibilité
     */
    private void endInvincibility() {
        isInvincible = false;

        if (entity == null) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world != null) {
            world.playSound(bossLoc, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
            world.spawnParticle(Particle.ITEM_CRACK, bossLoc.clone().add(0, 2, 0), 30, 1, 1, 1, 0.1,
                new org.bukkit.inventory.ItemStack(Material.PURPLE_STAINED_GLASS));
        }

        // Annonce
        for (Player player : getNearbyPlayers(40)) {
            player.sendMessage("§a§l✓ §7Tous les sbires sont morts! " + type.getDisplayName() + " §7est vulnérable!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }

        updateBossBar();
    }

    /**
     * Buff les zombies proches
     */
    private void buffNearbyZombies() {
        if (entity == null) return;

        for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 20, 10, 20)) {
            if (e instanceof Zombie zombie && e != entity) {
                if (zombie.getScoreboardTags().contains("queen_minion") ||
                    zombie.getScoreboardTags().contains("ZZ_")) {
                    // Buff de régénération
                    zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                }
            }
        }
    }

    @Override
    protected void updateBossBar() {
        super.updateBossBar();

        if (bossBar != null) {
            String status = "";
            if (isInvincible) {
                status = " §d[INVINCIBLE - §c" + activeMinions.size() + " sbires§d]";
            }
            bossBar.setTitle(type.getTitleName() + status + " §7- §c" + getFormattedHealth());
        }
    }

    @Override
    public void cleanup() {
        // Tuer tous les sbires
        for (UUID uuid : activeMinions) {
            Entity e = plugin.getServer().getEntity(uuid);
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        activeMinions.clear();
        isInvincible = false;
        super.cleanup();
    }
}
