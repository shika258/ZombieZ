package com.rinaorc.zombiez.zombies.elite;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.ZombieManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Gestionnaire des mobs Élite
 *
 * Les élites sont des mobs rares (1-3% de chance) avec:
 * - Stats augmentées (x2.5 HP, x1.8 dmg, x1.1 speed)
 * - Nom doré avec préfixe ⚔
 * - Glow doré
 * - Scale +50%
 * - 1-2 capacités procédurales aléatoires
 * - Récompenses x3
 */
public class EliteManager implements Listener {

    private final ZombieZPlugin plugin;

    // Clés PDC pour identifier les élites
    @Getter
    private final NamespacedKey eliteKey;
    private final NamespacedKey eliteAbilitiesKey;

    // Tracking des élites actifs
    private final Map<UUID, EliteData> activeElites = new ConcurrentHashMap<>();

    // Configuration
    private static final double ELITE_CHANCE_MIN = 0.015; // 1.5%
    private static final double ELITE_CHANCE_MAX = 0.015; // 1.5% (fixe)

    // Multiplicateurs de stats
    private static final double HEALTH_MULTIPLIER = 2.5;
    private static final double DAMAGE_MULTIPLIER = 1.8;
    private static final double SPEED_MULTIPLIER = 1.1;
    private static final float SCALE_MULTIPLIER = 1.5f; // +50%

    // Multiplicateurs de récompenses
    public static final double XP_MULTIPLIER = 3.0;
    public static final double POINTS_MULTIPLIER = 3.0;
    public static final double DROP_RATE_MULTIPLIER = 2.0;
    public static final double RARE_CHANCE_BONUS = 0.15; // +15%

    // Distance de notification
    private static final double NOTIFICATION_RADIUS = 50.0;

    // Team pour le glow doré
    private Team eliteGlowTeam;

    public EliteManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.eliteKey = new NamespacedKey(plugin, "elite_mob");
        this.eliteAbilitiesKey = new NamespacedKey(plugin, "elite_abilities");

        // Initialiser la team de glow
        initGlowTeam();

        // Démarrer le tick des capacités
        startAbilityTicker();

        // Enregistrer les listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.log(Level.INFO, "§a✓ EliteManager initialisé");
    }

    /**
     * Initialise la team Scoreboard pour le glow doré
     */
    private void initGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        eliteGlowTeam = scoreboard.getTeam("elite_glow_gold");
        if (eliteGlowTeam == null) {
            eliteGlowTeam = scoreboard.registerNewTeam("elite_glow_gold");
        }
        eliteGlowTeam.color(NamedTextColor.GOLD);
        eliteGlowTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    /**
     * Vérifie si un mob devrait devenir élite
     * @return true si le mob doit être converti en élite
     */
    public boolean shouldBecomeElite(int zoneId) {
        // Chance fixe de 1.5%
        double chance = ELITE_CHANCE_MIN + ThreadLocalRandom.current().nextDouble() * (ELITE_CHANCE_MAX - ELITE_CHANCE_MIN);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Convertit un mob en élite
     * Applique les stats, visuels et capacités
     */
    public EliteData convertToElite(LivingEntity entity, String baseName, int level, int zoneId) {
        if (entity == null || !entity.isValid()) return null;

        // Générer les capacités aléatoires (1-2)
        Set<EliteAbility> abilities = generateRandomAbilities();

        // Appliquer les stats
        applyEliteStats(entity);

        // Appliquer les visuels
        applyEliteVisuals(entity, baseName, level);

        // Appliquer le scale
        applyEliteScale(entity);

        // Marquer comme élite dans PDC
        var pdc = entity.getPersistentDataContainer();
        pdc.set(eliteKey, PersistentDataType.BYTE, (byte) 1);

        // Stocker les capacités
        String abilitiesStr = serializeAbilities(abilities);
        pdc.set(eliteAbilitiesKey, PersistentDataType.STRING, abilitiesStr);

        // Créer les données de tracking
        EliteData data = new EliteData(entity.getUniqueId(), level, zoneId, abilities);
        activeElites.put(entity.getUniqueId(), data);

        // Notifier les joueurs proches
        notifyNearbyPlayers(entity, baseName, level, abilities);

        // Effets de spawn
        spawnEliteEffects(entity.getLocation());

        return data;
    }

    /**
     * Génère 1-2 capacités aléatoires uniques
     */
    private Set<EliteAbility> generateRandomAbilities() {
        Set<EliteAbility> abilities = EnumSet.noneOf(EliteAbility.class);
        EliteAbility[] allAbilities = EliteAbility.values();

        // 1-2 capacités
        int count = ThreadLocalRandom.current().nextInt(1, 3);

        while (abilities.size() < count) {
            EliteAbility ability = allAbilities[ThreadLocalRandom.current().nextInt(allAbilities.length)];
            abilities.add(ability);
        }

        return abilities;
    }

    /**
     * Applique les multiplicateurs de stats
     */
    private void applyEliteStats(LivingEntity entity) {
        // HP x2.5
        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double newMaxHealth = maxHealthAttr.getBaseValue() * HEALTH_MULTIPLIER;
            maxHealthAttr.setBaseValue(newMaxHealth);
            entity.setHealth(newMaxHealth);
        }

        // Vitesse x1.1
        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * SPEED_MULTIPLIER);
        }

        // Armure +20%
        var armorAttr = entity.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(armorAttr.getBaseValue() + 4); // +4 armor points
        }
    }

    /**
     * Applique le scale +50%
     */
    private void applyEliteScale(LivingEntity entity) {
        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(SCALE_MULTIPLIER);
        }
    }

    /**
     * Applique les visuels (nom doré, glow)
     */
    private void applyEliteVisuals(LivingEntity entity, String baseName, int level) {
        // Glow doré via team
        eliteGlowTeam.addEntry(entity.getUniqueId().toString());
        entity.setGlowing(true);

        // Le nom sera mis à jour par le système de display name existant
        // On ajoute juste un tag pour que le système sache que c'est un élite
        entity.addScoreboardTag("elite_mob");
    }

    /**
     * Génère le nom d'affichage d'un élite
     */
    public Component generateEliteDisplayName(String baseName, int level, double currentHealth, double maxHealth) {
        // Couleur de la vie selon le pourcentage
        double healthPercent = currentHealth / maxHealth;
        NamedTextColor healthColor = healthPercent > 0.5 ? NamedTextColor.YELLOW :
                                     healthPercent > 0.25 ? NamedTextColor.GOLD : NamedTextColor.RED;

        // Format: §6§l⚔ Nom Élite [Lv.X] §e150§7/§e150 §6❤
        return Component.text()
            .append(Component.text("⚔ ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(baseName + " Élite ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("[Lv." + level + "] ", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.0f", currentHealth), healthColor))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.0f", maxHealth), NamedTextColor.YELLOW))
            .append(Component.text(" ❤", NamedTextColor.GOLD))
            .build();
    }

    /**
     * Notifie les joueurs proches du spawn d'un élite
     */
    private void notifyNearbyPlayers(LivingEntity entity, String baseName, int level, Set<EliteAbility> abilities) {
        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            // Pas besoin de vérifier le world car on itère déjà sur les joueurs du même world
            if (player.getLocation().distanceSquared(loc) <= NOTIFICATION_RADIUS * NOTIFICATION_RADIUS) {
                // Message
                player.sendMessage("");
                player.sendMessage(Component.text("⚔ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("ÉLITE DÉTECTÉ ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text("⚔", NamedTextColor.GOLD, TextDecoration.BOLD)));
                player.sendMessage(Component.text("  Un ", NamedTextColor.GRAY)
                    .append(Component.text(baseName + " Élite ", NamedTextColor.GOLD))
                    .append(Component.text("Lv." + level, NamedTextColor.YELLOW))
                    .append(Component.text(" est apparu!", NamedTextColor.GRAY)));

                // Afficher les capacités
                if (!abilities.isEmpty()) {
                    StringBuilder abilitiesText = new StringBuilder("  Capacités: ");
                    for (EliteAbility ability : abilities) {
                        abilitiesText.append("§e").append(ability.getDisplayName()).append("§7, ");
                    }
                    player.sendMessage(abilitiesText.substring(0, abilitiesText.length() - 2));
                }
                player.sendMessage("");

                // Son d'alerte
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            }
        }
    }

    /**
     * Effets visuels au spawn d'un élite
     */
    private void spawnEliteEffects(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Particules dorées
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);

        // Son de spawn
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);
    }

    /**
     * Vérifie si une entité est un élite
     */
    public boolean isElite(LivingEntity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(eliteKey, PersistentDataType.BYTE);
    }

    /**
     * Récupère les données d'un élite
     */
    public EliteData getEliteData(UUID entityId) {
        return activeElites.get(entityId);
    }

    /**
     * Récupère les capacités d'un élite depuis son PDC
     */
    public Set<EliteAbility> getAbilities(LivingEntity entity) {
        if (!isElite(entity)) return EnumSet.noneOf(EliteAbility.class);

        String abilitiesStr = entity.getPersistentDataContainer().get(eliteAbilitiesKey, PersistentDataType.STRING);
        return deserializeAbilities(abilitiesStr);
    }

    /**
     * Tick les capacités actives des élites
     */
    private void startAbilityTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Utiliser un iterator pour éviter ConcurrentModificationException
                var iterator = activeElites.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    LivingEntity entity = (LivingEntity) plugin.getServer().getEntity(entry.getKey());
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        iterator.remove();
                        continue;
                    }

                    EliteData data = entry.getValue();
                    tickAbilities(entity, data);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes
    }

    /**
     * Exécute le tick des capacités pour un élite
     */
    private void tickAbilities(LivingEntity entity, EliteData data) {
        for (EliteAbility ability : data.getAbilities()) {
            ability.onTick(entity, data, plugin);
        }
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Gère les dégâts infligés par un élite (multiplicateur)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!isElite(attacker)) return;

        // Appliquer le multiplicateur de dégâts
        event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);

        // Déclencher les capacités offensives
        EliteData data = activeElites.get(attacker.getUniqueId());
        if (data != null && event.getEntity() instanceof LivingEntity target) {
            for (EliteAbility ability : data.getAbilities()) {
                ability.onDealDamage(attacker, target, event, plugin);
            }
        }
    }

    /**
     * Gère les dégâts reçus par un élite
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!isElite(entity)) return;

        EliteData data = activeElites.get(entity.getUniqueId());
        if (data == null) return;

        // Déclencher les capacités défensives
        for (EliteAbility ability : data.getAbilities()) {
            ability.onTakeDamage(entity, event, plugin);
        }
    }

    /**
     * Gère la mort d'un élite (effets de mort)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isElite(entity)) return;

        EliteData data = activeElites.remove(entity.getUniqueId());
        if (data == null) return;

        // Déclencher les capacités de mort
        for (EliteAbility ability : data.getAbilities()) {
            ability.onDeath(entity, event, plugin);
        }

        // Effets de mort
        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.15);
            world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        }

        // Retirer du team glow
        eliteGlowTeam.removeEntry(entity.getUniqueId().toString());
    }

    // ==================== SERIALIZATION ====================

    private String serializeAbilities(Set<EliteAbility> abilities) {
        StringBuilder sb = new StringBuilder();
        for (EliteAbility ability : abilities) {
            if (sb.length() > 0) sb.append(",");
            sb.append(ability.name());
        }
        return sb.toString();
    }

    private Set<EliteAbility> deserializeAbilities(String str) {
        Set<EliteAbility> abilities = EnumSet.noneOf(EliteAbility.class);
        if (str == null || str.isEmpty()) return abilities;

        for (String name : str.split(",")) {
            try {
                abilities.add(EliteAbility.valueOf(name.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        return abilities;
    }

    // ==================== STATS ====================

    /**
     * Compte le nombre d'élites actifs
     */
    public int getActiveEliteCount() {
        return activeElites.size();
    }

    /**
     * Nettoie les élites invalides
     */
    public void cleanup() {
        activeElites.entrySet().removeIf(entry -> {
            LivingEntity entity = (LivingEntity) plugin.getServer().getEntity(entry.getKey());
            return entity == null || !entity.isValid() || entity.isDead();
        });
    }

    /**
     * Arrête proprement le système d'élites (appelé au shutdown du plugin)
     */
    public void shutdown() {
        // Retirer tous les élites du glow team
        for (UUID entityId : activeElites.keySet()) {
            eliteGlowTeam.removeEntry(entityId.toString());
        }

        // Vider la map
        activeElites.clear();

        plugin.log(Level.INFO, "§a✓ EliteManager arrêté proprement");
    }

    // ==================== DATA CLASS ====================

    /**
     * Données de tracking d'un élite
     */
    @Getter
    public static class EliteData {
        private final UUID entityId;
        private final int level;
        private final int zoneId;
        private final Set<EliteAbility> abilities;
        private final long spawnTime;

        // Données pour les capacités
        private long lastRegenTick = 0;
        private long lastDashTick = 0;
        private int damageDealtSinceSpawn = 0;

        public EliteData(UUID entityId, int level, int zoneId, Set<EliteAbility> abilities) {
            this.entityId = entityId;
            this.level = level;
            this.zoneId = zoneId;
            this.abilities = abilities;
            this.spawnTime = System.currentTimeMillis();
        }

        public void addDamageDealt(double damage) {
            this.damageDealtSinceSpawn += (int) damage;
        }

        public void setLastRegenTick(long lastRegenTick) {
            this.lastRegenTick = lastRegenTick;
        }

        public void setLastDashTick(long lastDashTick) {
            this.lastDashTick = lastDashTick;
        }
    }
}
