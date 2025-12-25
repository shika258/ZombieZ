package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe abstraite représentant un World Boss
 * Chaque boss spécifique étend cette classe et implémente ses mécaniques uniques
 */
@Getter
public abstract class WorldBoss {

    protected final ZombieZPlugin plugin;
    protected final WorldBossType type;
    protected final UUID bossId;
    protected final int zoneId;

    // Entité du boss
    protected Zombie entity;
    protected BossBar bossBar;

    // État
    protected boolean active = false;
    protected boolean dead = false;
    protected long spawnTime;
    protected long lastPlayerNearby;

    // Tracking des dégâts
    protected final Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();

    // Tâches
    protected BukkitTask abilityTask;
    protected BukkitTask despawnCheckTask;
    protected BukkitTask glowingTask;

    // Configuration
    protected static final int ALERT_RADIUS = 100;
    protected static final int DESPAWN_TIME_SECONDS = 600; // 10 minutes sans joueur

    public WorldBoss(ZombieZPlugin plugin, WorldBossType type, int zoneId) {
        this.plugin = plugin;
        this.type = type;
        this.zoneId = zoneId;
        this.bossId = UUID.randomUUID();
    }

    /**
     * Spawn le boss à une location donnée
     */
    public void spawn(Location location) {
        if (active) return;

        World world = location.getWorld();
        if (world == null) return;

        // Créer le zombie
        entity = world.spawn(location, Zombie.class, zombie -> {
            zombie.setAdult();
            zombie.setCanPickupItems(false);
            zombie.setRemoveWhenFarAway(false); // Ne despawn pas naturellement
            zombie.setPersistent(true);

            // Nom personnalisé
            zombie.setCustomName(type.getTitleName() + " §c[WORLD BOSS]");
            zombie.setCustomNameVisible(true);

            // Stats
            var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double health = type.calculateHealth(zoneId);
                maxHealth.setBaseValue(health);
                zombie.setHealth(health);
            }

            var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(type.calculateDamage(zoneId));
            }

            var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.25);
            }

            var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(0.8);
            }

            // Scale (taille)
            var scale = zombie.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(type.getScale());
            }

            // Résistances
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

            // Tags pour identification
            zombie.addScoreboardTag("world_boss");
            zombie.addScoreboardTag("boss_" + bossId.toString());
            zombie.addScoreboardTag("boss_type_" + type.name());

            // Glowing permanent
            zombie.setGlowing(true);
        });

        // Créer la boss bar
        createBossBar();

        // Effets de spawn
        spawnEffects(location);

        // Alerter les joueurs
        alertNearbyPlayers(location);

        // Démarrer les tâches
        startTasks();

        active = true;
        spawnTime = System.currentTimeMillis();
        lastPlayerNearby = System.currentTimeMillis();

        plugin.getLogger().info("[WorldBoss] " + type.getDisplayName() + " spawné en " + formatLocation(location));
    }

    /**
     * Crée la boss bar
     */
    protected void createBossBar() {
        BarColor color = switch (type) {
            case THE_BUTCHER -> BarColor.RED;
            case SHADOW_UNSTABLE -> BarColor.PURPLE;
            case PYROMANCER -> BarColor.YELLOW;
            case HORDE_QUEEN -> BarColor.PINK;
            case ICE_BREAKER -> BarColor.BLUE;
        };

        bossBar = Bukkit.createBossBar(
            type.getTitleName() + " §7- §c" + getFormattedHealth(),
            color,
            BarStyle.SEGMENTED_10
        );
        bossBar.setVisible(true);
    }

    /**
     * Effets visuels et sonores lors du spawn
     */
    protected void spawnEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Lightning
        world.strikeLightningEffect(location);

        // Sons
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 2f, 0.7f);
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);

        // Particules
        world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 100, 3, 3, 3, 0.1);
        world.spawnParticle(Particle.DRAGON_BREATH, location, 50, 2, 2, 2, 0.05);
    }

    /**
     * Alerte les joueurs à proximité
     */
    protected void alertNearbyPlayers(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        for (Entity e : world.getNearbyEntities(location, ALERT_RADIUS, ALERT_RADIUS, ALERT_RADIUS)) {
            if (e instanceof Player player) {
                // Title
                player.sendTitle(
                    "§c§lUN BOSS VIENT D'APPARAÎTRE",
                    "§7" + type.getDisplayName() + " §7est proche de vous!",
                    10, 60, 20
                );

                // Son d'alerte
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
                player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2f, 0.5f);

                // Message chat
                player.sendMessage("");
                player.sendMessage("§c§l⚠ ALERTE WORLD BOSS ⚠");
                player.sendMessage("§7" + type.getTitleName() + " §7vient d'apparaître!");
                player.sendMessage("§7Capacité: §e" + type.getAbilityDescription());
                player.sendMessage("§7Le boss brille pour être visible!");
                player.sendMessage("");
            }
        }
    }

    /**
     * Démarre les tâches récurrentes
     */
    protected void startTasks() {
        // Tâche d'utilisation des capacités
        if (type.getAbilityCooldownSeconds() > 0) {
            abilityTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active || entity == null || !entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }
                    useAbility();
                }
            }.runTaskTimer(plugin, type.getAbilityCooldownSeconds() * 20L, type.getAbilityCooldownSeconds() * 20L);
        }

        // Tâche de vérification de despawn (toutes les 30 secondes)
        despawnCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || entity == null || !entity.isValid() || entity.isDead()) {
                    cancel();
                    return;
                }

                // Vérifier si des joueurs sont à proximité
                boolean playersNearby = !entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 50, 50, 50)
                    .stream()
                    .anyMatch(e -> e instanceof Player);

                if (!playersNearby) {
                    // Vérifier le temps depuis le dernier joueur
                    long timeSincePlayer = System.currentTimeMillis() - lastPlayerNearby;
                    if (timeSincePlayer > DESPAWN_TIME_SECONDS * 1000L) {
                        despawn("Aucun joueur à proximité depuis 10 minutes");
                    }
                } else {
                    lastPlayerNearby = System.currentTimeMillis();
                }

                // Mettre à jour la boss bar
                updateBossBar();
            }
        }.runTaskTimer(plugin, 600L, 600L); // Toutes les 30 secondes

        // Tâche pour maintenir le glowing et mettre à jour la boss bar (toutes les secondes)
        glowingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || entity == null || !entity.isValid() || entity.isDead()) {
                    cancel();
                    return;
                }

                // Maintenir le glowing
                if (!entity.isGlowing()) {
                    entity.setGlowing(true);
                }

                // Mettre à jour les joueurs dans la boss bar
                updateBossBarPlayers();

                // Particules ambiantes
                ambientParticles();

                // Tick spécifique au boss
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Met à jour la boss bar
     */
    protected void updateBossBar() {
        if (bossBar == null || entity == null) return;

        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;

        double healthPercent = entity.getHealth() / maxHealth.getValue();
        bossBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
        bossBar.setTitle(type.getTitleName() + " §7- §c" + getFormattedHealth());
    }

    /**
     * Met à jour les joueurs dans la boss bar
     * Optimisé: utilise getNearbyEntities au lieu d'itérer sur tous les joueurs
     */
    protected void updateBossBarPlayers() {
        if (bossBar == null || entity == null) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Set des joueurs qui devraient voir la boss bar (rayon 60)
        Set<Player> nearbyPlayers = new java.util.HashSet<>();
        for (Entity e : world.getNearbyEntities(bossLoc, 60, 60, 60)) {
            if (e instanceof Player p) {
                nearbyPlayers.add(p);
            }
        }

        // Ajouter les joueurs proches qui ne sont pas dans la bar
        for (Player player : nearbyPlayers) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        // Retirer les joueurs qui ne sont plus proches
        for (Player player : new java.util.ArrayList<>(bossBar.getPlayers())) {
            if (!nearbyPlayers.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    /**
     * Particules ambiantes spécifiques au type de boss
     */
    protected void ambientParticles() {
        if (entity == null) return;
        Location loc = entity.getLocation().add(0, 1.5 * type.getScale(), 0);
        World world = loc.getWorld();
        if (world == null) return;

        switch (type) {
            case THE_BUTCHER -> world.spawnParticle(Particle.DUST, loc, 5,
                0.5, 0.5, 0.5, new Particle.DustOptions(Color.RED, 2f));
            case SHADOW_UNSTABLE -> world.spawnParticle(Particle.SMOKE, loc, 10, 0.5, 0.5, 0.5, 0.02);
            case PYROMANCER -> world.spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.5, 0.5, 0.02);
            case HORDE_QUEEN -> world.spawnParticle(Particle.WITCH, loc, 5, 0.5, 0.5, 0.5, 0);
            case ICE_BREAKER -> world.spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.5, 0.5, 0.5, 0.02);
        }
    }

    /**
     * Tick appelé chaque seconde - à surcharger par les sous-classes
     */
    protected void tick() {
        // Implémenté par les sous-classes
    }

    /**
     * Vérifie si le boss peut recevoir des dégâts
     * Surcharger pour implémenter l'invincibilité (ex: HordeQueen)
     * @return true si le boss peut être endommagé
     */
    public boolean canReceiveDamage() {
        return true;
    }

    /**
     * Utilise la capacité spéciale du boss - à implémenter par les sous-classes
     */
    protected abstract void useAbility();

    /**
     * Appelé quand le boss prend des dégâts
     */
    public void onDamage(Player attacker, double damage) {
        if (!active || entity == null) return;

        // Enregistrer les dégâts
        damageDealt.merge(attacker.getUniqueId(), damage, Double::sum);

        // Mettre à jour la boss bar
        updateBossBar();

        // Hook pour les sous-classes
        onDamageReceived(attacker, damage);
    }

    /**
     * Hook pour réagir aux dégâts - à surcharger par les sous-classes
     */
    protected void onDamageReceived(Player attacker, double damage) {
        // Implémenté par les sous-classes si nécessaire
    }

    /**
     * Appelé quand le boss est tué
     */
    public void onDeath(Player killer) {
        if (!active || dead) return;
        dead = true;
        active = false;

        Location deathLoc = entity != null ? entity.getLocation() : null;

        // Effets de mort
        if (deathLoc != null) {
            deathEffects(deathLoc);
        }

        // Distribuer les récompenses
        distributeRewards(killer);

        // Cleanup
        cleanup();

        plugin.getLogger().info("[WorldBoss] " + type.getDisplayName() + " tué par " + killer.getName());
    }

    /**
     * Effets visuels lors de la mort
     */
    protected void deathEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Sons
        world.playSound(location, Sound.ENTITY_WITHER_DEATH, 2f, 1f);
        world.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);

        // Particules
        world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 10, 3, 3, 3, 0);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 200, 3, 3, 3, 0.5);
        world.spawnParticle(Particle.DRAGON_BREATH, location, 100, 2, 2, 2, 0.1);
    }

    /**
     * Distribue les récompenses
     */
    protected void distributeRewards(Player killer) {
        if (entity == null) return;

        Location dropLocation = entity.getLocation();

        // Trouver le top damager
        UUID topDamager = getTopDamager();
        Player topPlayer = topDamager != null ? plugin.getServer().getPlayer(topDamager) : killer;

        // Points pour le top damager (bonus)
        long basePoints = 1000 + (zoneId * 50L);
        if (topPlayer != null) {
            plugin.getEconomyManager().addPoints(topPlayer, basePoints, "World Boss - Top Damager");
            topPlayer.sendMessage("§a§l+++ BONUS TOP DAMAGER +++");
            topPlayer.sendMessage("§7Vous avez infligé le plus de dégâts!");
        }

        // Points pour tous les participants
        long participantPoints = basePoints / 2;
        for (UUID uuid : damageDealt.keySet()) {
            Player participant = plugin.getServer().getPlayer(uuid);
            if (participant != null && participant != topPlayer) {
                plugin.getEconomyManager().addPoints(participant, participantPoints, "World Boss - Participation");
            }
        }

        // Drop du loot
        dropLoot(dropLocation);

        // Message de victoire à tous les participants
        sendVictoryMessage();
    }

    /**
     * Drop le loot du boss
     */
    protected void dropLoot(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        var itemManager = plugin.getItemManager();
        if (itemManager == null) return;

        // 100% chance d'une pièce d'équipement
        // +50% chance de rareté supérieure = luckBonus de 0.5
        double luckBonus = 0.5;

        // Drop 1-3 items selon la zone
        int itemCount = 1 + Math.min(2, zoneId / 15);

        for (int i = 0; i < itemCount; i++) {
            ItemStack item = itemManager.generateItem(zoneId, com.rinaorc.zombiez.items.types.Rarity.EPIC, luckBonus);
            if (item != null) {
                world.dropItemNaturally(location, item);
            }
        }

        // Item garanti LEGENDARY pour le premier drop
        ItemStack legendaryItem = itemManager.generateItem(zoneId, com.rinaorc.zombiez.items.types.Rarity.LEGENDARY, luckBonus);
        if (legendaryItem != null) {
            world.dropItemNaturally(location, legendaryItem);
        }

        // Consommables bonus
        if (plugin.getConsumableManager() != null) {
            for (int i = 0; i < 3; i++) {
                var consumable = plugin.getConsumableManager().generateConsumable(zoneId, luckBonus);
                if (consumable != null) {
                    world.dropItemNaturally(location, consumable.createItemStack());
                }
            }
        }
    }

    /**
     * Envoie un message de victoire
     */
    protected void sendVictoryMessage() {
        for (UUID uuid : damageDealt.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.sendTitle("§a§lVICTOIRE!", "§7" + type.getDisplayName() + " vaincu!", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                player.sendMessage("");
                player.sendMessage("§a§l✓ WORLD BOSS VAINCU!");
                player.sendMessage("§7Vous avez terrassé " + type.getTitleName() + "§7!");
                player.sendMessage("§7Dégâts infligés: §e" + String.format("%.0f", damageDealt.get(uuid)));
                player.sendMessage("§7Loot déposé au sol!");
                player.sendMessage("");
            }
        }
    }

    /**
     * Obtient le joueur ayant infligé le plus de dégâts
     */
    protected UUID getTopDamager() {
        return damageDealt.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Despawn le boss (timeout ou autre raison)
     */
    public void despawn(String reason) {
        if (!active) return;
        active = false;

        plugin.getLogger().info("[WorldBoss] " + type.getDisplayName() + " despawné: " + reason);

        // Effets de despawn
        if (entity != null && entity.isValid()) {
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.5f);
                world.spawnParticle(Particle.PORTAL, loc, 100, 2, 2, 2, 0.5);
            }
        }

        // Cleanup
        cleanup();
    }

    /**
     * Nettoyage complet
     */
    public void cleanup() {
        active = false;

        // Annuler les tâches
        if (abilityTask != null) {
            abilityTask.cancel();
            abilityTask = null;
        }
        if (despawnCheckTask != null) {
            despawnCheckTask.cancel();
            despawnCheckTask = null;
        }
        if (glowingTask != null) {
            glowingTask.cancel();
            glowingTask = null;
        }

        // Supprimer la boss bar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        // Supprimer l'entité si elle existe encore
        if (entity != null && entity.isValid() && !entity.isDead()) {
            entity.remove();
        }
        entity = null;

        // Nettoyer les données
        damageDealt.clear();
    }

    /**
     * Vérifie si le boss est actif
     */
    public boolean isActive() {
        return active && entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Obtient la santé formatée
     */
    protected String getFormattedHealth() {
        if (entity == null) return "0";
        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return "0";

        return String.format("%.0f/%.0f", entity.getHealth(), maxHealth.getValue());
    }

    /**
     * Formate une location
     */
    protected String formatLocation(Location loc) {
        return String.format("[%s: %.0f, %.0f, %.0f]",
            loc.getWorld() != null ? loc.getWorld().getName() : "?",
            loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Obtient tous les joueurs à proximité
     */
    protected List<Player> getNearbyPlayers(double radius) {
        if (entity == null) return List.of();

        return entity.getWorld()
            .getNearbyEntities(entity.getLocation(), radius, radius, radius)
            .stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .toList();
    }
}
