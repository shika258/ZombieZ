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
import org.bukkit.Color;
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
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Faille Temporelle - Un portail crache des zombies
 *
 * Mecanique:
 * - Un portail visuel apparait
 * - Des zombies spawn toutes les 1-2 secondes
 * - Tuer tous les zombies (15-25) ferme le portail
 * - Si le timer expire, les zombies restants deviennent enrages
 */
public class TemporalRiftEvent extends MicroEvent {

    private final Set<UUID> riftZombies = new HashSet<>();
    private int zombiesToSpawn;
    private int zombiesSpawned = 0;
    private int zombiesKilled = 0;
    private int ticksSinceLastSpawn = 0;
    private int spawnInterval = 30; // 1.5 secondes entre spawns (accelere)
    private boolean riftClosed = false;
    private final Random random = new Random();

    // TextDisplay flottant au-dessus du portail
    private TextDisplay riftDisplay;
    private static final float TEXT_SCALE = 1.4f;

    // Configuration
    private static final int MIN_ZOMBIES = 15;
    private static final int MAX_ZOMBIES = 25;
    private static final double BASE_ZOMBIE_HEALTH = 30.0;
    private static final double HEALTH_PER_ZONE = 8.0; // +8 HP par zone

    // Couleur violette pour l'armure des zombies temporels
    private static final Color TEMPORAL_COLOR = Color.fromRGB(138, 43, 226); // Violet

    // HP calcule dynamiquement selon la zone
    private double zombieHealth;

    public TemporalRiftEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.TEMPORAL_RIFT, player, location, zone);

        // Nombre de zombies base sur la zone
        int baseZombies = MIN_ZOMBIES + (zone.getId() / 10);
        this.zombiesToSpawn = Math.min(MAX_ZOMBIES, baseZombies);

        // Calculer les HP selon la zone (zone 1 = 38 HP, zone 10 = 110 HP, etc.)
        this.zombieHealth = BASE_ZOMBIE_HEALTH + (zone.getId() * HEALTH_PER_ZONE);
    }

    @Override
    protected void onStart() {
        // Effet d'ouverture du portail
        spawnRiftOpenEffect();

        // Creer le TextDisplay flottant au-dessus du portail
        Location displayLoc = location.clone().add(0, 3, 0);
        riftDisplay = displayLoc.getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(180, 80, 0, 120));
            display.setTransformation(new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                new org.joml.Quaternionf(),
                new Vector3f(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE),
                new org.joml.Quaternionf()
            ));
            display.text(createRiftDisplay());
        });
        registerEntity(riftDisplay);

        // Spawn les premiers zombies immediatement
        spawnZombie();
        spawnZombie();
    }

    @Override
    protected void tick() {
        // Effet visuel du portail
        if (elapsedTicks % 5 == 0) {
            spawnRiftParticles();
        }

        // Son ambiant du portail
        if (elapsedTicks % 40 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.2f);
        }

        // Mettre a jour le TextDisplay
        if (riftDisplay != null && riftDisplay.isValid()) {
            riftDisplay.text(createRiftDisplay());
        }

        // Spawn des zombies
        ticksSinceLastSpawn++;
        if (zombiesSpawned < zombiesToSpawn && ticksSinceLastSpawn >= spawnInterval) {
            ticksSinceLastSpawn = 0;
            spawnZombie();

            // Accelerer les spawns au fur et a mesure
            spawnInterval = Math.max(15, spawnInterval - 1);
        }

        // Verifier si tous les zombies sont tues
        if (zombiesKilled >= zombiesToSpawn) {
            riftClosed = true;
            complete();
            return;
        }

        // Cleanup des zombies morts de la liste
        riftZombies.removeIf(uuid -> {
            var entity = plugin.getServer().getEntity(uuid);
            return entity == null || entity.isDead();
        });

        // ActionBar
        int remaining = zombiesToSpawn - zombiesKilled;
        String progress = createProgressBar(zombiesKilled, zombiesToSpawn);
        sendActionBar("§d⚡ Faille Temporelle §7| " + progress + " §c" + remaining + " restants §7| §e" + getRemainingTimeSeconds() + "s");
    }

    /**
     * Spawn un zombie temporel custom avec armure violette
     */
    private void spawnZombie() {
        if (zombiesSpawned >= zombiesToSpawn) return;

        // Position aleatoire autour du portail
        double angle = Math.random() * Math.PI * 2;
        double radius = 1 + Math.random() * 2;
        Location spawnLoc = location.clone().add(
            Math.cos(angle) * radius,
            0,
            Math.sin(angle) * radius
        );
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        // Spawn le zombie temporel
        Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        riftZombies.add(zombie.getUniqueId());
        registerEntity(zombie);

        // Configuration du zombie temporel - afficher les HP comme les mobs ZombieZ
        zombie.setCustomName(createZombieName((int) zombieHealth, (int) zombieHealth));
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);
        zombie.setShouldBurnInDay(false);
        zombie.setGlowing(true);

        // Stats adaptees a la zone
        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombieHealth);
        zombie.setHealth(zombieHealth);
        zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.26 + (zone.getId() * 0.005)); // Vitesse qui augmente legerement
        zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(3.0 + (zone.getId() * 0.3)); // Degats qui augmentent

        // Armure en cuir violette (style temporel)
        zombie.getEquipment().setHelmet(createTemporalArmor(Material.LEATHER_HELMET));
        zombie.getEquipment().setChestplate(createTemporalArmor(Material.LEATHER_CHESTPLATE));
        zombie.getEquipment().setLeggings(createTemporalArmor(Material.LEATHER_LEGGINGS));
        zombie.getEquipment().setBoots(createTemporalArmor(Material.LEATHER_BOOTS));
        zombie.getEquipment().setHelmetDropChance(0f);
        zombie.getEquipment().setChestplateDropChance(0f);
        zombie.getEquipment().setLeggingsDropChance(0f);
        zombie.getEquipment().setBootsDropChance(0f);

        // Tags pour identification
        zombie.addScoreboardTag("micro_event_entity");
        zombie.addScoreboardTag("temporal_rift");
        zombie.addScoreboardTag("event_" + id);

        // Cibler le joueur
        zombie.setTarget(player);

        zombiesSpawned++;

        // Effet de spawn depuis le portail
        spawnLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 15, 0.3, 0.5, 0.3, 0.1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    /**
     * Cree une piece d'armure en cuir coloree violette (style temporel)
     */
    private ItemStack createTemporalArmor(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(TEMPORAL_COLOR);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cree le nom du zombie avec affichage des HP (style ZombieZ)
     */
    private String createZombieName(int currentHealth, int maxHealth) {
        double healthPercent = (double) currentHealth / maxHealth;
        String healthColor;
        if (healthPercent > 0.66) {
            healthColor = "§a"; // Vert
        } else if (healthPercent > 0.33) {
            healthColor = "§e"; // Jaune
        } else {
            healthColor = "§c"; // Rouge
        }
        return "§d⚡ Zombie Temporel " + healthColor + currentHealth + "§7/§a" + maxHealth + " §c❤";
    }

    /**
     * Effet d'ouverture du portail
     */
    private void spawnRiftOpenEffect() {
        Location center = location.clone().add(0, 1, 0);
        location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 40, 0.8, 1.2, 0.8, 0.15);
        location.getWorld().spawnParticle(Particle.END_ROD, center, 15, 0.4, 0.8, 0.4, 0.08);
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_TRIGGER, 1f, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2f);
    }

    /**
     * Particules continues du portail (optimise)
     */
    private void spawnRiftParticles() {
        Location center = location.clone().add(0, 1.5, 0);

        // Cercle de particules (4 points au lieu de 8)
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 2 / 4) * i + (elapsedTicks * 0.1);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            location.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 0, z), 1, 0, 0, 0, 0.5);
        }

        // Centre du portail
        location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 3, 0.2, 0.4, 0.2, 0.03);
    }

    /**
     * Cree une barre de progression
     */
    private String createProgressBar(int current, int total) {
        int bars = 10;
        int filled = (int) ((double) current / total * bars);

        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a█");
            } else {
                sb.append("§8░");
            }
        }
        sb.append("§7]");
        return sb.toString();
    }

    /**
     * Cree le Component pour l'affichage du portail
     */
    private Component createRiftDisplay() {
        int remaining = zombiesToSpawn - zombiesKilled;
        double progressPercent = (double) zombiesKilled / zombiesToSpawn;

        // Couleur basee sur la progression
        NamedTextColor progressColor = progressPercent > 0.66 ? NamedTextColor.GREEN :
            (progressPercent > 0.33 ? NamedTextColor.YELLOW : NamedTextColor.RED);

        // Barre de vie visuelle
        int barLength = 10;
        int filled = (int) (progressPercent * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }

        return Component.text("⚡ FAILLE TEMPORELLE ⚡", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
            .appendNewline()
            .append(Component.text(bar.toString(), progressColor))
            .append(Component.text(" " + remaining + " restants", NamedTextColor.RED))
            .appendNewline()
            .append(Component.text("⏱ " + getRemainingTimeSeconds() + "s", NamedTextColor.YELLOW));
    }

    @Override
    protected void onCleanup() {
        // Nettoyer le TextDisplay
        if (riftDisplay != null && riftDisplay.isValid()) {
            riftDisplay.remove();
        }
        riftDisplay = null;

        // Effet de fermeture du portail
        if (riftClosed) {
            Location center = location.clone().add(0, 1, 0);
            location.getWorld().spawnParticle(Particle.FLASH, center, 1);
            location.getWorld().spawnParticle(Particle.END_ROD, center, 20, 0.8, 1.2, 0.8, 0.15);
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
            // Nettoyer la liste
            riftZombies.clear();
        } else {
            // Echec - les zombies restants deviennent enrages
            for (UUID zombieId : riftZombies) {
                var entity = plugin.getServer().getEntity(zombieId);
                if (entity instanceof Zombie zombie && !zombie.isDead()) {
                    // Effet d'enragement
                    zombie.setCustomName("§4§l⚡ Zombie Enragé");
                    zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(10);
                    zombie.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                    // Retirer le tag pour qu'ils restent apres l'event
                    zombie.removeScoreboardTag("micro_event_entity");
                }
            }
            riftZombies.clear();
            spawnedEntities.clear(); // Ne pas supprimer les zombies enrages
        }
    }

    @Override
    protected int getBonusPoints() {
        if (!riftClosed) return 0;

        // Bonus si tous tues rapidement
        int timeSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (timeSeconds < 15) return 200;  // Perfect clear
        if (timeSeconds < 20) return 100;  // Fast clear
        return 50;                          // Normal clear
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (!riftZombies.contains(entity.getUniqueId())) {
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
            // Particules violettes pour le hit temporel
            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
        }

        // ============ METTRE À JOUR L'AFFICHAGE DES HP ============
        if (entity.isValid() && !entity.isDead()) {
            entity.setCustomName(createZombieName((int) newHealth, (int) zombieHealth));
        }

        // ============ GÉRER LA MORT SI NÉCESSAIRE ============
        if (newHealth <= 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    entity.setHealth(0);
                }
            });
        }

        return true;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (riftZombies.contains(entity.getUniqueId())) {
            zombiesKilled++;
            riftZombies.remove(entity.getUniqueId());

            // Effet de mort
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.1);

            // Son de compteur
            float pitch = 0.5f + (zombiesKilled / (float) zombiesToSpawn);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, pitch);

            // Message a certains paliers
            int remaining = zombiesToSpawn - zombiesKilled;
            if (remaining == 10 || remaining == 5 || remaining == 1) {
                player.sendMessage("§d⚡ §7" + remaining + " zombie" + (remaining > 1 ? "s" : "") + " restant" + (remaining > 1 ? "s" : "") + "!");
            }

            return true;
        }
        return false;
    }
}
