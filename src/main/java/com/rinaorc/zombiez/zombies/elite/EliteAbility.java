package com.rinaorc.zombiez.zombies.elite;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Capacités procédurales pour les mobs Élite
 * Chaque élite a 1-2 capacités aléatoires qui le rendent unique
 */
@Getter
public enum EliteAbility {

    // ==================== CAPACITÉS PASSIVES (TICK) ====================

    /**
     * RÉGÉNÉRATION - Régénère lentement ses PV
     * +1% max HP toutes les 3 secondes
     */
    REGEN("Régénération", "§a♥", "Régénère lentement ses PV") {
        @Override
        public void onTick(LivingEntity entity, EliteManager.EliteData data, ZombieZPlugin plugin) {
            long now = System.currentTimeMillis();
            if (now - data.getLastRegenTick() < 3000) return; // 3 secondes

            // Mettre à jour le timestamp via reflection ou autre méthode
            // Pour simplifier, on utilise le check de temps directement

            var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            double maxHealth = maxHealthAttr.getValue();
            double currentHealth = entity.getHealth();
            double regenAmount = maxHealth * 0.01; // 1% max HP

            if (currentHealth < maxHealth) {
                entity.setHealth(Math.min(maxHealth, currentHealth + regenAmount));

                // Particules de soin
                entity.getWorld().spawnParticle(
                    Particle.HEART,
                    entity.getLocation().add(0, 1.5, 0),
                    2, 0.3, 0.3, 0.3, 0
                );
            }
        }
    },

    /**
     * SPRINTER - Boost de vitesse périodique
     * Gagne un boost de vitesse toutes les 8 secondes pendant 2 secondes
     */
    SPRINTER("Sprinter", "§b⚡", "Sprinte périodiquement") {
        @Override
        public void onTick(LivingEntity entity, EliteManager.EliteData data, ZombieZPlugin plugin) {
            long now = System.currentTimeMillis();
            if (now - data.getLastDashTick() < 8000) return; // 8 secondes

            // Appliquer le boost de vitesse
            entity.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                40, // 2 secondes
                1,  // Speed II
                false, false
            ));

            // Effets visuels
            entity.getWorld().spawnParticle(
                Particle.CLOUD,
                entity.getLocation().add(0, 0.5, 0),
                10, 0.3, 0.1, 0.3, 0.05
            );
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 1.2f);
        }
    },

    // ==================== CAPACITÉS OFFENSIVES (ON HIT) ====================

    /**
     * BERSERKER - Plus de dégâts quand bas en PV
     * +50% dégâts sous 30% HP
     */
    BERSERKER("Berserker", "§c🔥", "Plus fort quand blessé") {
        @Override
        public void onDealDamage(LivingEntity attacker, LivingEntity target, EntityDamageByEntityEvent event, ZombieZPlugin plugin) {
            var maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            double healthPercent = attacker.getHealth() / maxHealthAttr.getValue();

            if (healthPercent <= 0.30) {
                // +50% dégâts
                event.setDamage(event.getDamage() * 1.5);

                // Effets visuels
                attacker.getWorld().spawnParticle(
                    Particle.ANGRY_VILLAGER,
                    attacker.getLocation().add(0, 1.5, 0),
                    3, 0.3, 0.3, 0.3, 0
                );
            }
        }
    },

    /**
     * VAMPIRIC - Vol de vie sur attaque
     * Récupère 15% des dégâts infligés
     */
    VAMPIRIC("Vampirique", "§4🩸", "Vole la vie de ses victimes") {
        @Override
        public void onDealDamage(LivingEntity attacker, LivingEntity target, EntityDamageByEntityEvent event, ZombieZPlugin plugin) {
            double damage = event.getFinalDamage();
            double healAmount = damage * 0.15;

            var maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            double newHealth = Math.min(maxHealthAttr.getValue(), attacker.getHealth() + healAmount);
            attacker.setHealth(newHealth);

            // Effets visuels
            attacker.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR,
                target.getLocation().add(0, 1, 0),
                5, 0.3, 0.3, 0.3, 0.1
            );
        }
    },

    /**
     * VENOMOUS - Empoisonne les cibles
     * Applique Poison I pendant 3 secondes
     */
    VENOMOUS("Venimeux", "§2☠", "Empoisonne ses victimes") {
        @Override
        public void onDealDamage(LivingEntity attacker, LivingEntity target, EntityDamageByEntityEvent event, ZombieZPlugin plugin) {
            if (ThreadLocalRandom.current().nextDouble() < 0.4) { // 40% de chance
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON,
                    60, // 3 secondes
                    0,  // Poison I
                    false, true
                ));

                // Effets visuels
                target.getWorld().spawnParticle(
                    Particle.ITEM_SLIME,
                    target.getLocation().add(0, 1, 0),
                    8, 0.3, 0.3, 0.3, 0.05
                );
            }
        }
    },

    // ==================== CAPACITÉS DÉFENSIVES (ON TAKE DAMAGE) ====================

    /**
     * THORNS - Renvoie une partie des dégâts
     * Renvoie 20% des dégâts reçus
     */
    THORNS("Épines", "§d🌵", "Renvoie les dégâts") {
        @Override
        public void onTakeDamage(LivingEntity entity, EntityDamageEvent event, ZombieZPlugin plugin) {
            if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return;
            if (!(damageEvent.getDamager() instanceof LivingEntity attacker)) return;

            double reflectDamage = event.getDamage() * 0.20;
            attacker.damage(reflectDamage, entity);

            // Effets visuels
            entity.getWorld().spawnParticle(
                Particle.CRIT_MAGIC,
                entity.getLocation().add(0, 1, 0),
                10, 0.3, 0.3, 0.3, 0.1
            );
            entity.getWorld().playSound(entity.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.5f, 1f);
        }
    },

    /**
     * TANK - Réduction de dégâts
     * -15% dégâts reçus
     */
    TANK("Blindé", "§8🛡", "Résiste aux dégâts") {
        @Override
        public void onTakeDamage(LivingEntity entity, EntityDamageEvent event, ZombieZPlugin plugin) {
            event.setDamage(event.getDamage() * 0.85);
        }
    },

    /**
     * DODGER - Esquive parfois les attaques
     * 15% de chance d'esquiver complètement
     */
    DODGER("Esquive", "§f💨", "Esquive parfois les attaques") {
        @Override
        public void onTakeDamage(LivingEntity entity, EntityDamageEvent event, ZombieZPlugin plugin) {
            if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                event.setCancelled(true);

                // Effets visuels d'esquive
                entity.getWorld().spawnParticle(
                    Particle.POOF,
                    entity.getLocation().add(0, 1, 0),
                    15, 0.4, 0.4, 0.4, 0.05
                );
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
            }
        }
    },

    // ==================== CAPACITÉS DE MORT (ON DEATH) ====================

    /**
     * EXPLOSIVE - Explose à la mort
     * Inflige des dégâts aux joueurs proches
     */
    EXPLOSIVE("Explosif", "§c💥", "Explose à la mort") {
        @Override
        public void onDeath(LivingEntity entity, EntityDeathEvent event, ZombieZPlugin plugin) {
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world == null) return;

            // Explosion visuelle (pas de destruction de blocs)
            world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

            // Dégâts aux entités proches (rayon 4 blocs, 10 dégâts)
            for (LivingEntity nearby : loc.getNearbyLivingEntities(4)) {
                if (nearby instanceof Player player) {
                    player.damage(10, entity);
                    player.sendMessage("§c§l💥 §7L'élite a explosé!");
                }
            }
        }
    },

    /**
     * SUMMONER - Invoque des minions à la mort
     * Spawn 2-3 mobs faibles
     */
    SUMMONER("Invocateur", "§5👻", "Invoque des serviteurs à sa mort") {
        @Override
        public void onDeath(LivingEntity entity, EntityDeathEvent event, ZombieZPlugin plugin) {
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world == null) return;

            var zombieManager = plugin.getZombieManager();
            if (zombieManager == null) return;

            int zoneId = plugin.getZoneManager().getZoneAt(loc).getId();
            int level = Math.max(1, (int) (entity.getPersistentDataContainer()
                .getOrDefault(plugin.getZombieManager().getPdcLevelKey(),
                    org.bukkit.persistence.PersistentDataType.INTEGER, 1) * 0.7));

            // Spawn 2-3 walkers
            int count = ThreadLocalRandom.current().nextInt(2, 4);
            for (int i = 0; i < count; i++) {
                Location spawnLoc = loc.clone().add(
                    ThreadLocalRandom.current().nextDouble(-2, 2),
                    0,
                    ThreadLocalRandom.current().nextDouble(-2, 2)
                );
                zombieManager.spawnZombie(ZombieType.WALKER, spawnLoc, level);
            }

            // Effets visuels
            world.spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            world.playSound(loc, Sound.ENTITY_VEX_AMBIENT, 1f, 0.5f);
        }
    },

    /**
     * MARTYR - Soigne les alliés à la mort
     * Soigne les mobs proches de 20% de leurs PV max
     */
    MARTYR("Martyr", "§e✝", "Soigne ses alliés à sa mort") {
        @Override
        public void onDeath(LivingEntity entity, EntityDeathEvent event, ZombieZPlugin plugin) {
            Location loc = entity.getLocation();

            // Soigner les mobs proches
            for (LivingEntity nearby : loc.getNearbyLivingEntities(6)) {
                if (nearby instanceof Player) continue;
                if (nearby == entity) continue;

                // Vérifier si c'est un mob ZombieZ
                if (!nearby.getPersistentDataContainer().has(
                    plugin.getZombieManager().getPdcMobKey(),
                    org.bukkit.persistence.PersistentDataType.BYTE)) continue;

                var maxHealthAttr = nearby.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr == null) continue;

                double healAmount = maxHealthAttr.getValue() * 0.20;
                nearby.setHealth(Math.min(maxHealthAttr.getValue(), nearby.getHealth() + healAmount));

                // Effets visuels
                nearby.getWorld().spawnParticle(
                    Particle.HEART,
                    nearby.getLocation().add(0, 1.5, 0),
                    5, 0.3, 0.3, 0.3, 0
                );
            }

            // Effets visuels de mort
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 1.5f);
        }
    };

    // ==================== PROPRIÉTÉS ====================

    private final String displayName;
    private final String icon;
    private final String description;

    EliteAbility(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    // ==================== MÉTHODES DE CALLBACK ====================

    /**
     * Appelé toutes les secondes
     */
    public void onTick(LivingEntity entity, EliteManager.EliteData data, ZombieZPlugin plugin) {
        // Override dans les capacités qui ont besoin de tick
    }

    /**
     * Appelé quand l'élite inflige des dégâts
     */
    public void onDealDamage(LivingEntity attacker, LivingEntity target, EntityDamageByEntityEvent event, ZombieZPlugin plugin) {
        // Override dans les capacités offensives
    }

    /**
     * Appelé quand l'élite reçoit des dégâts
     */
    public void onTakeDamage(LivingEntity entity, EntityDamageEvent event, ZombieZPlugin plugin) {
        // Override dans les capacités défensives
    }

    /**
     * Appelé quand l'élite meurt
     */
    public void onDeath(LivingEntity entity, EntityDeathEvent event, ZombieZPlugin plugin) {
        // Override dans les capacités de mort
    }

    /**
     * Retourne le texte complet avec icône et nom
     */
    public String getFullDisplay() {
        return icon + " " + displayName;
    }
}
