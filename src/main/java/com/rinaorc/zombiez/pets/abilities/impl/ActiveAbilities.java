package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.abilities.PetDamageUtils;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Implémentations des capacités actives des pets
 */

// ==================== ECHO SCAN ====================

@Getter
class EchoScanActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int radius;
    private final int durationSeconds;

    EchoScanActive(String id, String name, String desc, int radius, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.radius = radius;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());
        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);

        // Effet sonore d'écholocation
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.8f, 2.0f);

        int enemiesFound = 0;
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                // Marquer avec effet glowing
                monster.setGlowing(true);
                enemiesFound++;
                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("ZombieZ"),
                    () -> monster.setGlowing(false),
                    durationSeconds * 20L
                );
            }
        }

        // Particules d'onde - OPTIMISÉ : seulement 3 anneaux espacés
        int ringCount = 3;
        int ringSpacing = Math.max(1, adjustedRadius / ringCount);
        for (int i = 1; i <= ringCount; i++) {
            final int ringRadius = i * ringSpacing;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> spawnRing(player.getLocation(), ringRadius),
                i * 4L // Espacement temporel plus visible
            );
        }

        // Feedback au joueur
        if (enemiesFound > 0) {
            player.sendMessage("§d[Pet] §7Écho-Scan: §e" + enemiesFound + " §7ennemi(s) détecté(s)!");
        } else {
            player.sendMessage("§d[Pet] §7Écho-Scan: §aAucun ennemi à proximité.");
        }

        return true;
    }

    private void spawnRing(Location center, int radius) {
        // Particules légères : END_ROD au lieu de SONIC_BOOM
        // 12 points par anneau au lieu de 36
        for (int angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            Location loc = center.clone().add(
                Math.cos(rad) * radius,
                0.5,
                Math.sin(rad) * radius
            );
            center.getWorld().spawnParticle(Particle.END_ROD, loc, 3, 0.1, 0.2, 0.1, 0.02);
        }
        // Son d'écho à chaque anneau
        center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f + (radius * 0.05f));
    }
}

// ==================== SEARCH ====================

@Getter
class SearchActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final Random random = new Random();

    SearchActive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 60; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int itemCount = 1 + random.nextInt(3); // 1-3 items
        itemCount = (int) (itemCount * petData.getStatMultiplier());

        player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(),
            20, 0.5, 0.1, 0.5, Material.DIRT.createBlockData());

        // Donner des ressources aléatoires
        for (int i = 0; i < itemCount; i++) {
            Material[] resources = {
                Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.COAL,
                Material.EMERALD, Material.DIAMOND, Material.BONE
            };
            Material mat = resources[random.nextInt(resources.length)];
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, 1 + random.nextInt(3)));
        }

        player.sendMessage("§a[Pet] §7Votre rat a trouvé §e" + itemCount + " §7ressources!");
        return true;
    }
}

// ==================== FLASH ====================

@Getter
class FlashActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int blindDurationSeconds;

    FlashActive(String id, String name, String desc, int cooldown, int blindDuration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.blindDurationSeconds = blindDuration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (blindDurationSeconds * 20 * petData.getStatMultiplier());

        Collection<Entity> nearby = player.getNearbyEntities(5, 5, 5);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                // Annuler la cible
                if (monster instanceof Mob mob) {
                    mob.setTarget(null);
                }
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedDuration, 4, false, false));
            }
        }

        // Effet visuel
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 2.0f);

        return true;
    }
}

// ==================== SCOUT ====================

@Getter
class ScoutActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    ScoutActive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 40; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Révèle une zone dans la direction regardée
        Location target = player.getTargetBlock(null, 50).getLocation();
        int radius = (int) (15 * petData.getStatMultiplier());

        Collection<Entity> nearby = target.getWorld().getNearbyEntities(target, radius, radius, radius);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.setGlowing(true);
                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("ZombieZ"),
                    () -> monster.setGlowing(false),
                    100L
                );
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PARROT_FLY, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §7Zone explorée! Ennemis marqués.");

        return true;
    }
}

// ==================== HOWL ====================

@Getter
class HowlActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageBoost;
    private final int durationSeconds;

    HowlActive(String id, String name, String desc, int cooldown, double boost, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageBoost = boost;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (durationSeconds * 20 * petData.getStatMultiplier());
        int level = (int) (damageBoost * 10);

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, adjustedDuration, level, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 10);

        player.sendMessage("§a[Pet] §7Hurlement! §c+" + (int)(damageBoost * 100) + "% §7dégâts!");
        return true;
    }
}

// ==================== HEAL ====================

@Getter
class HealActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healAmount;

    HealActive(String id, String name, String desc, int cooldown, double heal) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healAmount = heal;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 40; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double adjustedHeal = healAmount * petData.getStatMultiplier();
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + adjustedHeal * 2);
        player.setHealth(newHealth);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
        player.sendMessage("§a[Pet] §7Soigné de §c" + (int)(adjustedHeal * 2) + "❤");

        return true;
    }
}

// ==================== WALL ====================

@Getter
class WallActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;

    WallActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize();

        // Créer un mur perpendiculaire à la direction du joueur
        Vector perpendicular = new Vector(-dir.getZ(), 0, dir.getX());
        List<Location> wallBlocks = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 2; y++) {
                Location blockLoc = loc.clone().add(perpendicular.clone().multiply(x)).add(0, y, 0);
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    wallBlocks.add(blockLoc);
                    block.setType(Material.COBBLESTONE);
                }
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);

        // Supprimer le mur après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                for (Location blockLoc : wallBlocks) {
                    if (blockLoc.getBlock().getType() == Material.COBBLESTONE) {
                        blockLoc.getBlock().setType(Material.AIR);
                        blockLoc.getWorld().spawnParticle(Particle.BLOCK,
                            blockLoc.add(0.5, 0.5, 0.5), 10, Material.COBBLESTONE.createBlockData());
                    }
                }
            },
            durationSeconds * 20L
        );

        return true;
    }
}

// ==================== IGNITE AREA ====================

@Getter
class IgniteAreaActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int radius;

    IgniteAreaActive(String id, String name, String desc, int cooldown, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());
        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);

        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.setFireTicks(100);
            }
        }

        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, radius, 1, radius, 0.1);
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        return true;
    }
}

// ==================== WEB ====================

@Getter
class WebActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;

    WebActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location target = player.getTargetBlock(null, 10).getLocation();
        List<Location> webBlocks = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location blockLoc = target.clone().add(x, 0, z);
                if (blockLoc.getBlock().getType() == Material.AIR) {
                    blockLoc.getBlock().setType(Material.COBWEB);
                    webBlocks.add(blockLoc);
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.0f);

        // Supprimer les toiles après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                for (Location blockLoc : webBlocks) {
                    if (blockLoc.getBlock().getType() == Material.COBWEB) {
                        blockLoc.getBlock().setType(Material.AIR);
                    }
                }
            },
            durationSeconds * 20L
        );

        return true;
    }
}

// ==================== FIRE NOVA ====================

@Getter
class FireNovaActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePercent;  // Pourcentage des dégâts du joueur (ex: 1.5 = 150%)
    private final int radius;
    private final int cooldown;

    FireNovaActive(String id, String name, String desc, int cooldown, double damagePercent, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cooldown;
        this.damagePercent = damagePercent;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // Calculer les dégâts basés sur les dégâts du joueur
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double adjustedDamage = playerDamage * damagePercent * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius + (petData.getStatMultiplier() - 1) * 2);

        int hitCount = 0;
        double totalDamage = 0;

        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(adjustedDamage, player);
                monster.setFireTicks(100); // 5 secondes de feu
                petData.addDamage((long) adjustedDamage);
                hitCount++;
                totalDamage += adjustedDamage;

                // Particules sur chaque cible touchée
                world.spawnParticle(Particle.FLAME, monster.getLocation().add(0, 1, 0),
                    15, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Effet visuel de nova de feu expansive
        world.spawnParticle(Particle.EXPLOSION, loc, 3, 1, 0.5, 1, 0);
        world.spawnParticle(Particle.FLAME, loc, 150, adjustedRadius, 1, adjustedRadius, 0.15);
        world.spawnParticle(Particle.LAVA, loc, 30, adjustedRadius * 0.5, 0.5, adjustedRadius * 0.5, 0);

        // Cercle de feu au sol
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            Location fireLoc = loc.clone().add(
                Math.cos(rad) * adjustedRadius,
                0.1,
                Math.sin(rad) * adjustedRadius
            );
            world.spawnParticle(Particle.FLAME, fireLoc, 5, 0.1, 0.1, 0.1, 0.02);
        }

        // Sons
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

        // Message
        if (hitCount > 0) {
            player.sendMessage("§a[Pet] §6§l🔥 NOVA DE FEU! §7" + hitCount + " cibles, §c" +
                (int) totalDamage + " §7dégâts totaux!");
        } else {
            player.sendMessage("§a[Pet] §6🔥 Nova de Feu! §7Aucun ennemi touché.");
        }

        return true;
    }
}

// ==================== FREEZE ====================

@Getter
class FreezeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int freezeDurationSeconds;

    FreezeActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.freezeDurationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (freezeDurationSeconds * 20 * petData.getStatMultiplier());
        Vector direction = player.getLocation().getDirection().normalize();

        // Cône devant le joueur
        Collection<Entity> nearby = player.getNearbyEntities(8, 4, 8);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                Vector toEntity = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                if (direction.dot(toEntity) > 0.5) { // Dans le cône
                    monster.setFreezeTicks(adjustedDuration);
                    monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, adjustedDuration, 127, false, false));
                }
            }
        }

        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(direction.multiply(3)), 50, 2, 1, 2, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);

        return true;
    }
}

// ==================== RESET COOLDOWN ====================

@Getter
class ResetCooldownActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    ResetCooldownActive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 90; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Cette capacité reset les cooldowns de classe via le ClassManager
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);
        player.sendMessage("§a[Pet] §7Cooldowns de classe réinitialisés!");
        return true;
    }
}

// ==================== SWARM ====================

@Getter
class SwarmActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double totalDamage;
    private final int durationSeconds;

    SwarmActive(String id, String name, String desc, int cooldown, double damage, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.totalDamage = damage;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        LivingEntity target = getTarget(player);
        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucune cible en vue!");
            return false;
        }

        double adjustedDamage = totalDamage * petData.getStatMultiplier();
        double damagePerTick = adjustedDamage / (durationSeconds * 4);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationSeconds * 4 || !target.isValid()) {
                    cancel();
                    return;
                }
                target.damage(damagePerTick, player);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5);
                petData.addDamage((long) damagePerTick);
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 5L);

        player.playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, 1.0f);
        return true;
    }

    private LivingEntity getTarget(Player player) {
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Monster monster) {
                Vector toEntity = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                if (player.getLocation().getDirection().dot(toEntity) > 0.8) {
                    return monster;
                }
            }
        }
        return null;
    }
}

// ==================== RIPOSTE ====================

@Getter
class RiposteActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageMultiplier;
    private final Map<UUID, Boolean> riposteReady = new HashMap<>();

    RiposteActive(String id, String name, String desc, int cooldown, double multiplier) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageMultiplier = multiplier;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        riposteReady.put(player.getUniqueId(), true);
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §7Riposte prête! Prochaine attaque subie = contre-attaque!");

        // Auto-désactivation après 10 secondes
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> riposteReady.remove(player.getUniqueId()),
            200L
        );

        return true;
    }

    public boolean isReady(UUID uuid) {
        return riposteReady.getOrDefault(uuid, false);
    }

    public void consume(UUID uuid) {
        riposteReady.remove(uuid);
    }

    public double getMultiplier() {
        return damageMultiplier;
    }
}

// ==================== BREATH (Dragon) ====================

@Getter
class BreathActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damage;

    BreathActive(String id, String name, String desc, int cooldown, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damage = damage;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double adjustedDamage = damage * petData.getStatMultiplier();
        Vector direction = player.getLocation().getDirection().normalize();

        // Souffle en cône
        for (int i = 1; i <= 8; i++) {
            final int distance = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    Location loc = player.getLocation().add(direction.clone().multiply(distance)).add(0, 1, 0);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.5, 0.5, 0.5, 0.05);

                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                        if (entity instanceof Monster monster) {
                            monster.damage(adjustedDamage / 4, player);
                            monster.setFireTicks(60);
                            petData.addDamage((long) (adjustedDamage / 4));
                        }
                    }
                },
                i * 2L
            );
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
        return true;
    }
}

// ==================== ABYSS LASER (Guardian) ====================

@Getter
class AbyssLaserActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double damagePercent;          // % des dégâts du joueur
    private final int range;                      // Portée du laser

    AbyssLaserActive(String id, String name, String desc, int cd, double dmgPercent, int range) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.damagePercent = dmgPercent;
        this.range = range;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        UUID playerUUID = player.getUniqueId();

        // Calculer les dégâts basés sur les stats du joueur
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double adjustedPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.10;
        double laserDamage = playerDamage * adjustedPercent;

        int adjustedRange = range + (int)((petData.getStatMultiplier() - 1) * 2);

        // Trouver des cibles
        List<Monster> targets = player.getNearbyEntities(adjustedRange, 5, adjustedRange).stream()
            .filter(e -> e instanceof Monster)
            .map(e -> (Monster) e)
            .toList();

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à portée du laser!");
            return false;
        }

        // Trouver le Guardian pet pour l'animation
        Entity guardianPet = findGuardianPet(player);

        player.sendMessage("§a[Pet] §b§l⚡ RAYON DES ABYSSES!");
        world.playSound(playerLoc, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);

        // Phase 1: Charge du laser (1.5s) - marquer les cibles
        Set<UUID> markedTargets = new java.util.HashSet<>();
        for (Monster target : targets) {
            markedTargets.add(target.getUniqueId());

            // Effet de marquage sur la cible
            Location targetLoc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.SOUL, targetLoc, 10, 0.3, 0.3, 0.3, 0.02);

            // Particules de charge vers la cible
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 30 || !target.isValid()) { // 1.5s charge
                        cancel();
                        return;
                    }

                    // Particules de charge orbitant autour de la cible
                    Location loc = target.getLocation().add(0, 1, 0);
                    double angle = ticks * 0.4;
                    double radius = 0.8 - (ticks / 30.0) * 0.5; // Rétrécit

                    for (int i = 0; i < 3; i++) {
                        double a = angle + i * (2 * Math.PI / 3);
                        Location orbLoc = loc.clone().add(
                            Math.cos(a) * radius,
                            Math.sin(ticks * 0.2) * 0.3,
                            Math.sin(a) * radius
                        );
                        world.spawnParticle(Particle.ELECTRIC_SPARK, orbLoc, 1, 0, 0, 0, 0);
                    }

                    // Son de charge
                    if (ticks % 10 == 0) {
                        world.playSound(loc, Sound.ENTITY_GUARDIAN_AMBIENT, 0.5f, 1.5f + (ticks / 30.0f));
                    }

                    ticks++;
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
        }

        // Faire briller l'œil du Guardian pendant l'ultimate
        if (guardianPet instanceof org.bukkit.entity.Guardian guardian) {
            guardian.setLaser(true);
        }

        // Phase 2: Tir des lasers (après 1.5s de charge)
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), () -> {
            // Son de tir
            world.playSound(playerLoc, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
            world.playSound(playerLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

            // Trouver la position du Guardian pet ou utiliser celle du joueur
            Location laserSource = guardianPet != null ?
                guardianPet.getLocation().add(0, 0.5, 0) :
                playerLoc.add(0, 1.5, 0);

            // Tirer un laser vers chaque cible marquée
            for (Monster target : targets) {
                if (!target.isValid() || target.isDead()) continue;
                if (!markedTargets.contains(target.getUniqueId())) continue;

                Location targetLoc = target.getLocation().add(0, 1, 0);

                // Dessiner le laser
                drawGuardianLaser(world, laserSource, targetLoc);

                // Infliger les dégâts
                target.damage(laserDamage, player);
                petData.addDamage((long) laserDamage);

                // Effet d'impact
                world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 30, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.BUBBLE_POP, targetLoc, 20, 0.4, 0.4, 0.4, 0.05);
                world.spawnParticle(Particle.FLASH, targetLoc, 1, 0, 0, 0, 0);

                // Mining Fatigue (signature du Guardian)
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, 60, 1, false, true));

                // Léger knockback
                Vector knockback = targetLoc.toVector().subtract(laserSource.toVector()).normalize().multiply(0.3);
                target.setVelocity(target.getVelocity().add(knockback));
            }

            // Désactiver le laser du Guardian après un court délai
            if (guardianPet instanceof org.bukkit.entity.Guardian guardian) {
                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("ZombieZ"),
                    () -> guardian.setLaser(false),
                    20L
                );
            }

        }, 30L); // Délai de 1.5s pour la charge

        return true;
    }

    /**
     * Trouve le Guardian pet du joueur
     */
    private Entity findGuardianPet(Player player) {
        String ownerTag = "pet_owner_" + player.getUniqueId();
        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Guardian
                && entity.getScoreboardTags().contains(ownerTag)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Dessine le laser de Guardian entre deux points
     */
    private void drawGuardianLaser(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        // Couleurs du laser Guardian (cyan/turquoise)
        Color laserColor = Color.fromRGB(0, 200, 200);
        Color coreColor = Color.fromRGB(150, 255, 255);

        for (double d = 0; d < distance; d += 0.3) {
            Location point = from.clone().add(direction.clone().multiply(d));

            // Cœur du laser (blanc-cyan)
            Particle.DustOptions coreDust = new Particle.DustOptions(coreColor, 1.0f);
            world.spawnParticle(Particle.DUST, point, 2, 0.02, 0.02, 0.02, coreDust);

            // Aura du laser (cyan)
            Particle.DustOptions auraDust = new Particle.DustOptions(laserColor, 0.7f);
            world.spawnParticle(Particle.DUST, point, 3, 0.08, 0.08, 0.08, auraDust);

            // Étincelles électriques
            if (d % 1 < 0.3) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
}

// ==================== VEX SWARM (Evoker) ====================

@Getter
class VexSwarmActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int vexCount;                   // Nombre de Vex invoqués
    private final int durationSeconds;            // Durée de vie des Vex
    private final double damagePercent;           // % des dégâts du joueur par Vex

    VexSwarmActive(String id, String name, String desc, int cd, int count, int duration, double dmgPercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.vexCount = count;
        this.durationSeconds = duration;
        this.damagePercent = dmgPercent;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        UUID playerUUID = player.getUniqueId();

        // Calculer les stats ajustées par niveau
        int adjustedVexCount = vexCount + (int)((petData.getStatMultiplier() - 1) * 2);
        int adjustedDuration = durationSeconds + (int)((petData.getStatMultiplier() - 1) * 2);
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double adjustedDmgPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.05;
        double vexDamage = playerDamage * adjustedDmgPercent;

        // Vérifier qu'il y a des ennemis
        List<Monster> targets = player.getNearbyEntities(15, 8, 15).stream()
            .filter(e -> e instanceof Monster)
            .map(e -> (Monster) e)
            .toList();

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi pour les Vex!");
            return false;
        }

        player.sendMessage("§a[Pet] §5§l👻 NUÉE DE VEX! §r§d(" + adjustedVexCount + " Vex pendant " + adjustedDuration + "s)");

        // Animation de l'Evoker pet
        animateEvokerSummon(player);

        // Sons d'invocation
        world.playSound(playerLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
        world.playSound(playerLoc, Sound.ENTITY_VEX_AMBIENT, 1.0f, 0.8f);

        // Particules de rituel
        spawnSummonCircle(world, playerLoc);

        // Invoquer les Vex avec un petit délai entre chaque
        List<org.bukkit.entity.Vex> summonedVex = new ArrayList<>();
        String allyTag = "pet_vex_" + playerUUID;

        for (int i = 0; i < adjustedVexCount; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    // Position de spawn en cercle autour du joueur
                    double angle = (2 * Math.PI / adjustedVexCount) * index;
                    double spawnRadius = 1.5;
                    Location spawnLoc = playerLoc.clone().add(
                        Math.cos(angle) * spawnRadius,
                        1.5 + Math.random() * 0.5,
                        Math.sin(angle) * spawnRadius
                    );

                    // Spawner le Vex
                    org.bukkit.entity.Vex vex = world.spawn(spawnLoc, org.bukkit.entity.Vex.class, v -> {
                        v.setCustomName("§d✦ Vex de " + player.getName());
                        v.setCustomNameVisible(false);
                        v.addScoreboardTag(allyTag);
                        v.addScoreboardTag("pet_ally");
                        v.setPersistent(false);

                        // Empêcher le Vex d'attaquer le joueur
                        v.setCharging(true);
                    });

                    summonedVex.add(vex);

                    // Particules d'apparition
                    world.spawnParticle(Particle.WITCH, spawnLoc, 15, 0.2, 0.3, 0.2, 0.05);
                    world.spawnParticle(Particle.SMOKE, spawnLoc, 10, 0.2, 0.2, 0.2, 0.02);
                    world.playSound(spawnLoc, Sound.ENTITY_VEX_CHARGE, 0.8f, 1.2f);
                },
                i * 3L // Délai progressif pour l'effet visuel
            );
        }

        // IA des Vex - attaquer les ennemis
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> startVexAI(player, summonedVex, vexDamage, petData, adjustedDuration * 20),
            adjustedVexCount * 3L + 5L // Après que tous les Vex soient spawnés
        );

        return true;
    }

    private void startVexAI(Player player, List<org.bukkit.entity.Vex> vexList, double damage, PetData petData, int durationTicks) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Fin de la durée ou plus de Vex
                vexList.removeIf(v -> !v.isValid() || v.isDead());

                if (ticks >= durationTicks || vexList.isEmpty()) {
                    // Faire disparaître les Vex restants
                    for (org.bukkit.entity.Vex vex : vexList) {
                        if (vex.isValid()) {
                            // Particules de disparition
                            vex.getWorld().spawnParticle(Particle.WITCH, vex.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                            vex.getWorld().spawnParticle(Particle.SMOKE, vex.getLocation(), 8, 0.2, 0.2, 0.2, 0.02);
                            vex.remove();
                        }
                    }

                    if (player.isOnline()) {
                        player.sendMessage("§a[Pet] §7Les Vex se dissipent...");
                    }
                    cancel();
                    return;
                }

                // Toutes les 10 ticks, faire attaquer les Vex
                if (ticks % 10 == 0) {
                    // Trouver les cibles
                    List<Monster> targets = new ArrayList<>();
                    for (Entity entity : player.getNearbyEntities(20, 10, 20)) {
                        if (entity instanceof Monster m && m.isValid() && !m.isDead()) {
                            targets.add(m);
                        }
                    }

                    if (!targets.isEmpty()) {
                        for (org.bukkit.entity.Vex vex : vexList) {
                            if (!vex.isValid()) continue;

                            // Choisir une cible aléatoire
                            Monster target = targets.get((int) (Math.random() * targets.size()));

                            // Mouvement vers la cible
                            Vector direction = target.getLocation().toVector()
                                .subtract(vex.getLocation().toVector()).normalize().multiply(0.3);
                            vex.setVelocity(direction.add(new Vector(0, 0.1, 0)));

                            // Attaquer si assez proche
                            if (vex.getLocation().distanceSquared(target.getLocation()) < 4) {
                                target.damage(damage, player);
                                petData.addDamage((long) damage);

                                // Effets d'attaque
                                vex.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                                    5, 0.2, 0.2, 0.2, 0.05);
                                vex.getWorld().playSound(target.getLocation(), Sound.ENTITY_VEX_HURT, 0.5f, 1.5f);

                                // Animation de charge
                                vex.setCharging(true);
                                Bukkit.getScheduler().runTaskLater(
                                    Bukkit.getPluginManager().getPlugin("ZombieZ"),
                                    () -> { if (vex.isValid()) vex.setCharging(false); },
                                    5L
                                );
                            }
                        }
                    }
                }

                // Particules ambiantes sur les Vex
                if (ticks % 20 == 0) {
                    for (org.bukkit.entity.Vex vex : vexList) {
                        if (vex.isValid()) {
                            vex.getWorld().spawnParticle(Particle.WITCH, vex.getLocation(),
                                3, 0.1, 0.1, 0.1, 0.01);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    private void animateEvokerSummon(Player player) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Evoker evoker
                && entity.getScoreboardTags().contains(ownerTag)) {

                Location loc = evoker.getLocation().add(0, 1.5, 0);

                // Particules d'invocation
                evoker.getWorld().spawnParticle(Particle.WITCH, loc, 30, 0.4, 0.4, 0.4, 0.05);
                evoker.getWorld().spawnParticle(Particle.ENCHANT, loc, 25, 0.5, 0.5, 0.5, 0.5);
                evoker.getWorld().spawnParticle(Particle.SOUL, loc, 15, 0.3, 0.3, 0.3, 0.02);

                break;
            }
        }
    }

    private void spawnSummonCircle(World world, Location center) {
        // Cercle de particules au sol
        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;

            @Override
            public void run() {
                if (ticks >= 20) { // 1 seconde d'animation
                    cancel();
                    return;
                }

                double radius = 1.5 + (ticks / 20.0) * 0.5;

                for (int i = 0; i < 12; i++) {
                    double angle = rotation + i * (Math.PI / 6);
                    Location particleLoc = center.clone().add(
                        Math.cos(angle) * radius,
                        0.1,
                        Math.sin(angle) * radius
                    );

                    // Alterner les couleurs
                    if (i % 2 == 0) {
                        world.spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.ENCHANT, particleLoc, 2, 0, 0.1, 0, 0.1);
                    }
                }

                rotation += 0.3;
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== BENEVOLENT RAIN (Happy Ghast) ====================

@Getter
class BenevolentRainActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int radius;                     // 6 blocs
    private final int durationSeconds;            // 5 secondes
    private final double regenPerSecond;          // % HP regen par seconde
    private final int slowLevel;                  // Niveau de slow sur ennemis

    BenevolentRainActive(String id, String name, String desc, int cd, int radius,
                          int duration, double regenPct, int slow) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.radius = radius;
        this.durationSeconds = duration;
        this.regenPerSecond = regenPct;
        this.slowLevel = slow;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Ajuster par niveau
        int adjustedRadius = radius + (int)((petData.getStatMultiplier() - 1) * 2);
        int adjustedDuration = durationSeconds + (int)((petData.getStatMultiplier() - 1) * 1);
        double adjustedRegen = regenPerSecond + (petData.getStatMultiplier() - 1) * 0.01;

        player.sendMessage("§a[Pet] §d§l☔ PLUIE BIENFAISANTE! §r§d(rayon " + adjustedRadius + " blocs, " + adjustedDuration + "s)");

        // Sons de début
        world.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.0f, 1.5f);
        world.playSound(center, Sound.WEATHER_RAIN, 1.0f, 1.2f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        // Trouver le Ghast pet pour les effets
        Entity ghastPet = findGhastPet(player);
        if (ghastPet != null) {
            // Effet de "pleurs" du Ghast
            Location ghastLoc = ghastPet.getLocation();
            world.spawnParticle(Particle.FALLING_WATER, ghastLoc, 30, 0.3, 0.2, 0.3, 0);
        }

        // Animation de pluie bienfaisante
        new BukkitRunnable() {
            int ticks = 0;
            final Location rainCenter = center.clone();
            final Random random = new Random();

            @Override
            public void run() {
                if (ticks >= adjustedDuration * 20) {
                    // Fin de la pluie
                    world.playSound(rainCenter, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, rainCenter.clone().add(0, 1, 0), 30,
                        adjustedRadius * 0.5, 1, adjustedRadius * 0.5, 0);

                    player.sendMessage("§a[Pet] §7La pluie s'arrête...");
                    cancel();
                    return;
                }

                // Particules de pluie arc-en-ciel tombantes
                if (ticks % 2 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist = random.nextDouble() * adjustedRadius;
                        double height = 4 + random.nextDouble() * 2;

                        Location dropLoc = rainCenter.clone().add(
                            Math.cos(angle) * dist,
                            height,
                            Math.sin(angle) * dist
                        );

                        // Gouttes arc-en-ciel
                        Color[] colors = {
                            Color.fromRGB(255, 150, 200),  // Rose
                            Color.fromRGB(200, 255, 200),  // Vert clair
                            Color.fromRGB(200, 200, 255),  // Bleu clair
                            Color.fromRGB(255, 255, 200)   // Jaune clair
                        };
                        Color dropColor = colors[random.nextInt(colors.length)];
                        Particle.DustOptions dust = new Particle.DustOptions(dropColor, 0.6f);

                        world.spawnParticle(Particle.DUST, dropLoc, 1, 0, 0, 0, dust);
                        world.spawnParticle(Particle.FALLING_WATER, dropLoc, 1, 0.1, 0, 0.1, 0);
                    }
                }

                // Cercle au sol qui pulse
                if (ticks % 10 == 0) {
                    double pulseRadius = adjustedRadius * (0.8 + 0.2 * Math.sin(ticks * 0.1));
                    for (int i = 0; i < 24; i++) {
                        double angle = i * (Math.PI / 12);
                        Location circleLoc = rainCenter.clone().add(
                            Math.cos(angle) * pulseRadius,
                            0.1,
                            Math.sin(angle) * pulseRadius
                        );
                        world.spawnParticle(Particle.HAPPY_VILLAGER, circleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Effets chaque seconde
                if (ticks % 20 == 0) {
                    // Son ambiant de pluie douce
                    world.playSound(rainCenter, Sound.WEATHER_RAIN, 0.5f, 1.5f);
                    world.playSound(rainCenter, Sound.ENTITY_GHAST_AMBIENT, 0.3f, 2.0f);

                    // Heal le joueur
                    if (player.getLocation().distanceSquared(rainCenter) <= adjustedRadius * adjustedRadius) {
                        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                        double healAmount = maxHealth * adjustedRegen;
                        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
                        player.setHealth(newHealth);

                        // Effet de soin
                        player.getWorld().spawnParticle(Particle.HEART,
                            player.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3, 0);
                    }

                    // Slow les ennemis dans la zone
                    for (Entity entity : rainCenter.getWorld().getNearbyEntities(rainCenter, adjustedRadius, 4, adjustedRadius)) {
                        if (entity instanceof Monster m) {
                            m.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 30, slowLevel, false, true));

                            // Particules de slow sur les ennemis
                            Location mLoc = m.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.FALLING_WATER, mLoc, 5, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }

                // Nuages doux au-dessus de la zone
                if (ticks % 15 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist = random.nextDouble() * adjustedRadius * 0.7;
                        Location cloudLoc = rainCenter.clone().add(
                            Math.cos(angle) * dist,
                            5 + random.nextDouble(),
                            Math.sin(angle) * dist
                        );
                        world.spawnParticle(Particle.CLOUD, cloudLoc, 3, 0.5, 0.2, 0.5, 0.01);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private Entity findGhastPet(Player player) {
        String ownerTag = "pet_owner_" + player.getUniqueId();
        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Ghast
                && entity.getScoreboardTags().contains(ownerTag)) {
                return entity;
            }
        }
        return null;
    }
}

// ==================== RESURRECT ====================

@Getter
class ResurrectActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int allyDuration;

    ResurrectActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.allyDuration = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Spawn un zombie allié
        Zombie ally = (Zombie) player.getWorld().spawnEntity(
            player.getLocation().add(2, 0, 0), EntityType.ZOMBIE);
        ally.setCustomName("§a" + player.getName() + "'s Minion");
        ally.setCustomNameVisible(true);
        ally.addScoreboardTag("zombiez_pet_ally");
        ally.addScoreboardTag("owner_" + player.getUniqueId());

        // Configurer l'allié
        ally.setTarget(null);
        ally.setBaby(false);

        player.getWorld().spawnParticle(Particle.WITCH, ally.getLocation(), 30, 0.5, 1, 0.5, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);

        // Supprimer après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                if (ally.isValid()) {
                    ally.getWorld().spawnParticle(Particle.SMOKE, ally.getLocation(), 20);
                    ally.remove();
                }
            },
            allyDuration * 20L
        );

        return true;
    }
}

// ==================== SACRIFICE ====================

@Getter
class SacrificeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;
    private final Map<UUID, Boolean> sacrificeActive = new HashMap<>();

    SacrificeActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 60; }

    @Override
    public boolean activate(Player player, PetData petData) {
        sacrificeActive.put(player.getUniqueId(), true);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, durationSeconds * 20, 254, false, true));

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 50, 1, 1, 1, 0);

        player.sendMessage("§a[Pet] §7Sacrifice cristallin activé! Invincible pendant §e" + durationSeconds + "s§7!");

        // Explosion à la fin
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                sacrificeActive.remove(player.getUniqueId());
                Collection<Entity> nearby = player.getNearbyEntities(5, 5, 5);
                for (Entity entity : nearby) {
                    if (entity instanceof Monster monster) {
                        monster.damage(30 * petData.getStatMultiplier(), player);
                    }
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, 2, 2, 2, 0);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            },
            durationSeconds * 20L
        );

        return true;
    }

    public boolean isActive(UUID uuid) {
        return sacrificeActive.getOrDefault(uuid, false);
    }
}

// ==================== AMBUSH ====================

@Getter
class AmbushActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double critMultiplier;
    private final Map<UUID, Boolean> ambushReady = new HashMap<>();

    AmbushActive(String id, String name, String desc, int cooldown, double multiplier) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.critMultiplier = multiplier;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        ambushReady.put(player.getUniqueId(), true);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY, 60, 0, false, false));

        player.playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §7Embuscade prête! Prochaine attaque = §cx" + (int) critMultiplier + " §7critique!");

        // Auto-désactivation après 5 secondes
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                ambushReady.remove(player.getUniqueId());
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            },
            100L
        );

        return true;
    }

    public boolean isReady(UUID uuid) {
        return ambushReady.getOrDefault(uuid, false);
    }

    public void consume(UUID uuid) {
        ambushReady.remove(uuid);
    }

    public double getMultiplier() {
        return critMultiplier;
    }
}

// ==================== CHAOS ACTIVE ====================

@Getter
class ChaosActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final Random random = new Random();

    ChaosActive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int effect = random.nextInt(5);
        double power = petData.getStatMultiplier();

        switch (effect) {
            case 0 -> {
                // Explosion massive
                Collection<Entity> nearby = player.getNearbyEntities(10, 10, 10);
                for (Entity e : nearby) {
                    if (e instanceof Monster m) {
                        m.damage(50 * power, player);
                    }
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 5, 2, 5, 0);
                player.sendMessage("§a[Pet] §7Chaos: §cExplosion massive!");
            }
            case 1 -> {
                // Full heal + buffs
                player.setHealth(player.getMaxHealth());
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 2, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2, false, false));
                player.sendMessage("§a[Pet] §7Chaos: §aSoins complets + buffs!");
            }
            case 2 -> {
                // Gel de zone
                Collection<Entity> nearby = player.getNearbyEntities(8, 8, 8);
                for (Entity e : nearby) {
                    if (e instanceof Monster m) {
                        m.setFreezeTicks(200);
                    }
                }
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 100, 8, 2, 8, 0);
                player.sendMessage("§a[Pet] §7Chaos: §bGel de zone!");
            }
            case 3 -> {
                // Téléportation avec dégâts
                Location newLoc = player.getLocation().add(
                    (random.nextDouble() - 0.5) * 20,
                    0,
                    (random.nextDouble() - 0.5) * 20
                );
                player.teleport(newLoc);
                Collection<Entity> nearby = player.getNearbyEntities(3, 3, 3);
                for (Entity e : nearby) {
                    if (e instanceof Monster m) {
                        m.damage(30 * power, player);
                    }
                }
                player.getWorld().spawnParticle(Particle.PORTAL, newLoc, 50, 1, 1, 1, 0.5);
                player.sendMessage("§a[Pet] §7Chaos: §5Téléportation offensive!");
            }
            case 4 -> {
                // Invincibilité temporaire
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 254, false, true));
                player.sendMessage("§a[Pet] §7Chaos: §eInvincibilité temporaire!");
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
        return true;
    }
}

// ==================== DIVINE ====================

@Getter
class DivineActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int invincibilitySeconds;
    private static final double ALLY_BLESSING_RADIUS = 10.0; // Rayon pour bénir les alliés

    DivineActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.invincibilitySeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 120; }

    /**
     * Vérifie si la bénédiction affecte aussi les alliés (étoiles max)
     */
    private boolean shouldBlessAllies(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (invincibilitySeconds * 20 * petData.getStatMultiplier());
        boolean blessAllies = shouldBlessAllies(petData);

        // Appliquer au joueur
        applyBlessing(player, adjustedDuration);

        int alliesBlessed = 0;

        // Étoiles max : Bénir aussi les alliés dans 10 blocs
        if (blessAllies) {
            for (Entity entity : player.getNearbyEntities(ALLY_BLESSING_RADIUS, ALLY_BLESSING_RADIUS, ALLY_BLESSING_RADIUS)) {
                if (entity instanceof Player ally && !ally.equals(player)) {
                    applyBlessing(ally, adjustedDuration);
                    alliesBlessed++;

                    // Effet visuel sur l'allié
                    ally.getWorld().spawnParticle(Particle.END_ROD, ally.getLocation().add(0, 1, 0), 50, 1, 1.5, 1, 0.05);
                    ally.playSound(ally.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                    ally.sendMessage("§a[Pet] §6Bénédiction Divine reçue! §7Invincible pendant §e" + invincibilitySeconds + "s§7!");
                }
            }
        }

        // Effet visuel principal
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 2, 2, 2, 0.1);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Rayon de lumière si bénédiction de groupe
        if (blessAllies) {
            spawnDivineRay(player.getLocation());
        }

        String allyMsg = alliesBlessed > 0 ? " §7(+" + alliesBlessed + " alliés)" : "";
        player.sendMessage("§a[Pet] §6Bénédiction Divine! §7Invincible pendant §e" + invincibilitySeconds + "s§7!" + allyMsg);
        return true;
    }

    /**
     * Applique la bénédiction divine à un joueur (full heal + invincibilité)
     */
    private void applyBlessing(Player player, int durationTicks) {
        // Full heal
        player.setHealth(player.getMaxHealth());

        // Invincibilité (Resistance 254 = immune aux dégâts)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, durationTicks, 254, false, true));

        // Régénération V
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION, durationTicks, 4, false, true));

        // Retirer tous les effets négatifs
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    /**
     * Crée un rayon de lumière divine descendant du ciel
     */
    private void spawnDivineRay(Location center) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            double height = 15;

            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }

                // Rayon descendant
                for (double y = 0; y < height; y += 0.5) {
                    Location loc = center.clone().add(0, y, 0);
                    center.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0, 0.2, 0);
                }

                // Cercle au sol
                for (double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle + ticks * 12);
                    double x = ALLY_BLESSING_RADIUS * Math.cos(rad);
                    double z = ALLY_BLESSING_RADIUS * Math.sin(rad);
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
                }

                height -= 0.5;
                ticks++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== PORTAL ====================

@Getter
class PortalActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int maxDistance;

    PortalActive(String id, String name, String desc, int cooldown, int distance) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.maxDistance = distance;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDistance = (int) (maxDistance * petData.getStatMultiplier());
        Block target = player.getTargetBlock(null, adjustedDistance);

        if (target.getType() == Material.AIR || target.getLocation().distance(player.getLocation()) < 3) {
            player.sendMessage("§c[Pet] §7Cible invalide!");
            return false;
        }

        Location origin = player.getLocation();
        Location destination = target.getLocation().add(0, 1, 0);

        // Téléportation
        player.teleport(destination);

        // Effets
        origin.getWorld().spawnParticle(Particle.PORTAL, origin, 50, 0.5, 1, 0.5, 0.5);
        destination.getWorld().spawnParticle(Particle.PORTAL, destination, 50, 0.5, 1, 0.5, 0.5);
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        return true;
    }
}

// ==================== SMASH ====================

@Getter
class SmashActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damage;
    private final int radius;

    SmashActive(String id, String name, String desc, int cooldown, double damage, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damage = damage;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double adjustedDamage = damage * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(adjustedDamage, player);
                // Knockback
                Vector knockback = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(2)
                    .setY(0.5);
                entity.setVelocity(knockback);
                petData.addDamage((long) adjustedDamage);
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, radius, 0.5, radius, 0);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(),
            50, radius, 0.1, radius, Material.STONE.createBlockData());
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);

        return true;
    }
}

// ==================== SANCTUARY ====================

@Getter
class SanctuaryActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healPerSecond;
    private final int durationSeconds;

    SanctuaryActive(String id, String name, String desc, int cooldown, double heal, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healPerSecond = heal;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 60; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double adjustedHeal = healPerSecond * petData.getStatMultiplier();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationSeconds * 20) {
                    cancel();
                    return;
                }

                // Particules de zone
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    Location loc = center.clone().add(Math.cos(rad) * 5, 0.5, Math.sin(rad) * 5);
                    center.getWorld().spawnParticle(Particle.COMPOSTER, loc, 3);
                }

                // Soin toutes les 20 ticks
                if (ticks % 20 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
                        if (entity instanceof Player p && p.getLocation().distance(center) <= 5) {
                            double newHealth = Math.min(p.getMaxHealth(), p.getHealth() + adjustedHeal * 2);
                            p.setHealth(newHealth);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §7Sanctuaire Naturel créé!");

        return true;
    }
}

// ==================== APOCALYPSE ====================

@Getter
class ApocalypseActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double totalDamage;
    private final int radius;

    ApocalypseActive(String id, String name, String desc, int cooldown, double damage, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.totalDamage = damage;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double adjustedDamage = totalDamage * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        // Pluie de feu
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 20) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 3; i++) {
                    Location dropLoc = center.clone().add(
                        (Math.random() - 0.5) * adjustedRadius * 2,
                        10,
                        (Math.random() - 0.5) * adjustedRadius * 2
                    );

                    center.getWorld().spawnParticle(Particle.FLAME, dropLoc, 20, 0.5, 5, 0.5, 0.1);

                    // Impact
                    Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("ZombieZ"),
                        () -> {
                            Location impact = dropLoc.clone();
                            impact.setY(center.getY());
                            center.getWorld().spawnParticle(Particle.LAVA, impact, 10, 1, 0.5, 1, 0);

                            for (Entity e : center.getWorld().getNearbyEntities(impact, 3, 3, 3)) {
                                if (e instanceof Monster m) {
                                    m.damage(adjustedDamage / 10, player);
                                    m.setFireTicks(100);
                                }
                            }
                        },
                        10L
                    );
                }

                count++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 5L);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        player.sendMessage("§a[Pet] §c§lAPOCALYPSE DE FEU!");

        return true;
    }
}

// ==================== DEATH SENTENCE ====================

@Getter
class DeathSentenceActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int delaySeconds;

    DeathSentenceActive(String id, String name, String desc, int cooldown, int delay) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.delaySeconds = delay;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 90; }

    @Override
    public boolean activate(Player player, PetData petData) {
        LivingEntity target = getTarget(player);
        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucune cible en vue!");
            return false;
        }

        target.setGlowing(true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, delaySeconds * 20, 2, false, true));

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        player.sendMessage("§a[Pet] §c§lSentence Mortelle! §7La cible mourra dans §e" + delaySeconds + "s§7!");

        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                if (target.isValid()) {
                    // Boss: -50% HP, sinon mort
                    if (target.getMaxHealth() > 500) {
                        target.damage(target.getMaxHealth() * 0.5, player);
                    } else {
                        target.setHealth(0);
                    }
                    target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 50, 1, 1, 1, 0.1);
                    petData.addKill();
                }
            },
            delaySeconds * 20L
        );

        return true;
    }

    private LivingEntity getTarget(Player player) {
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof Monster monster) {
                Vector toEntity = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                if (player.getLocation().getDirection().dot(toEntity) > 0.9) {
                    return monster;
                }
            }
        }
        return null;
    }
}

// ==================== BLACK HOLE ====================

@Getter
class BlackHoleActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;

    BlackHoleActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 60; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getTargetBlock(null, 20).getLocation().add(0, 1, 0);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationSeconds * 20) {
                    // Explosion finale
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 20, 2, 2, 2, 0);
                    for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
                        if (e instanceof Monster m) {
                            m.damage(30 * petData.getStatMultiplier(), player);
                        }
                    }
                    cancel();
                    return;
                }

                // Particules du trou noir
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle + ticks * 10);
                    double r = 2 * (1 - (double) ticks / (durationSeconds * 20));
                    Location loc = center.clone().add(Math.cos(rad) * r, Math.sin(ticks * 0.1), Math.sin(rad) * r);
                    center.getWorld().spawnParticle(Particle.PORTAL, loc, 3);
                }

                // Aspirer les ennemis
                for (Entity e : center.getWorld().getNearbyEntities(center, 8, 8, 8)) {
                    if (e instanceof Monster) {
                        Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.3);
                        e.setVelocity(pull);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        player.sendMessage("§a[Pet] §8§lTrou Noir créé!");

        return true;
    }
}

// ==================== TIME STOP ====================

@Getter
class TimeStopActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int freezeSeconds;

    TimeStopActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.freezeSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 75; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (freezeSeconds * 20 * petData.getStatMultiplier());

        Collection<Entity> nearby = player.getNearbyEntities(15, 15, 15);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.setAI(false);
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING, adjustedDuration, 0, false, false));
            }
        }

        // Réactiver l'IA après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                for (Entity entity : nearby) {
                    if (entity instanceof Monster monster && monster.isValid()) {
                        monster.setAI(true);
                    }
                }
            },
            adjustedDuration
        );

        // Buff au joueur
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, adjustedDuration, 1, false, true));

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 10, 5, 10, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, 0.1f);

        player.sendMessage("§a[Pet] §e§lARRÊT DU TEMPS! §7(" + freezeSeconds + "s)");
        return true;
    }
}

// ==================== TRIPLE BREATH ====================

@Getter
class TripleBreathActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    TripleBreathActive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Vector direction = player.getLocation().getDirection().normalize();
        double damage = 30 * petData.getStatMultiplier();

        // 3 souffles: feu (centre), glace (gauche), poison (droite)
        Vector left = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        Vector right = new Vector(direction.getZ(), 0, -direction.getX()).normalize();

        spawnBreath(player, direction, Particle.FLAME, damage, 60, petData); // Feu
        spawnBreath(player, direction.clone().add(left.multiply(0.5)), Particle.SNOWFLAKE, damage, 0, petData); // Glace
        spawnBreath(player, direction.clone().add(right.multiply(0.5)), Particle.FALLING_SPORE_BLOSSOM, damage, 0, petData); // Poison

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        return true;
    }

    private void spawnBreath(Player player, Vector direction, Particle particle, double damage, int fireTicks, PetData petData) {
        for (int i = 1; i <= 8; i++) {
            final int distance = i;
            final Vector dir = direction.clone();
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    Location loc = player.getLocation().add(dir.clone().multiply(distance)).add(0, 1, 0);
                    loc.getWorld().spawnParticle(particle, loc, 15, 0.5, 0.5, 0.5, 0.02);

                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                        if (entity instanceof Monster monster) {
                            monster.damage(damage / 4, player);
                            if (fireTicks > 0) monster.setFireTicks(fireTicks);
                            if (particle == Particle.SNOWFLAKE) {
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 60, 2, false, false));
                            }
                            if (particle == Particle.FALLING_SPORE_BLOSSOM) {
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.POISON, 100, 1, false, false));
                            }
                            petData.addDamage((long) (damage / 4));
                        }
                    }
                },
                i * 2L
            );
        }
    }
}

// ==================== COLOSSUS ====================

@Getter
class ColossusActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;

    ColossusActive(String id, String name, String desc, int cooldown, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 120; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int adjustedDuration = (int) (durationSeconds * 20 * petData.getStatMultiplier());

        // Buffs massifs
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, adjustedDuration, 4, false, true));
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, adjustedDuration, 254, false, true));
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, adjustedDuration, 1, false, false));

        // Effet visuel continu
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= adjustedDuration) {
                    cancel();
                    return;
                }

                // Ondes de choc
                if (ticks % 40 == 0) {
                    Collection<Entity> nearby = player.getNearbyEntities(5, 3, 5);
                    for (Entity e : nearby) {
                        if (e instanceof Monster m) {
                            m.damage(20 * petData.getStatMultiplier(), player);
                            Vector kb = e.getLocation().toVector()
                                .subtract(player.getLocation().toVector())
                                .normalize().multiply(0.5);
                            e.setVelocity(kb);
                        }
                    }
                    player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(),
                        30, 3, 0.5, 3, Material.ANCIENT_DEBRIS.createBlockData());
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        player.getWorld().strikeLightningEffect(player.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);

        player.sendMessage("§a[Pet] §6§lÉVEIL DU COLOSSE! §7(" + durationSeconds + "s)");
        return true;
    }
}

// ==================== VORTEX CHAOTIQUE (BREEZE) ====================

@Getter
class ChaoticVortexActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double baseDamage;
    private final double radius;

    ChaoticVortexActive(String id, String name, String desc, int cd, double damage, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.baseDamage = damage;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        Location center = player.getLocation();

        double adjustedDamage = baseDamage * petData.getStatMultiplier();
        double adjustedRadius = radius + (petData.getStatMultiplier() - 1) * 2;

        player.sendMessage("§a[Pet] §b§l🌪️ VORTEX CHAOTIQUE! §7Les vents se déchaînent!");

        // Sons initiaux
        world.playSound(center, Sound.ENTITY_BREEZE_CHARGE, 1.0f, 0.8f);
        world.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.5f);

        // Phase 1: Aspiration (2 secondes)
        new BukkitRunnable() {
            int ticks = 0;
            final int suctionDuration = 40; // 2 secondes
            final List<Monster> trappedMonsters = new ArrayList<>();

            @Override
            public void run() {
                if (ticks < suctionDuration) {
                    // Phase d'aspiration
                    double progress = ticks / (double) suctionDuration;

                    // Particules de tourbillon
                    for (int i = 0; i < 20; i++) {
                        double angle = (ticks * 0.3) + i * (Math.PI * 2 / 20);
                        double currentRadius = adjustedRadius * (1 - progress * 0.5);
                        double height = Math.sin(ticks * 0.2 + i * 0.3) * 2 + 1;

                        Location particleLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            height,
                            Math.sin(angle) * currentRadius
                        );

                        world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                        if (i % 4 == 0) {
                            world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }

                    // Aspirer les ennemis vers le centre
                    for (Entity entity : center.getNearbyEntities(adjustedRadius, 4, adjustedRadius)) {
                        if (entity instanceof Monster m && m.isValid() && !m.isDead()) {
                            if (!trappedMonsters.contains(m)) {
                                trappedMonsters.add(m);
                            }

                            Vector toCenter = center.toVector().subtract(m.getLocation().toVector());
                            double dist = toCenter.length();
                            if (dist > 1) {
                                toCenter.normalize().multiply(0.4);
                                toCenter.setY(0.1);
                                m.setVelocity(toCenter);
                            }

                            // Désorientation (slow + faiblesse)
                            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));
                        }
                    }

                    // Son périodique de vent
                    if (ticks % 10 == 0) {
                        world.playSound(center, Sound.ENTITY_BREEZE_INHALE, 0.8f, 1.0f + (float) progress * 0.5f);
                    }
                } else if (ticks == suctionDuration) {
                    // Phase 2: EXPLOSION!
                    world.playSound(center, Sound.ENTITY_BREEZE_SHOOT, 1.5f, 0.5f);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                    world.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1.5f, 0.8f);

                    // Particules d'explosion
                    Location explosionCenter = center.clone().add(0, 1, 0);
                    world.spawnParticle(Particle.EXPLOSION, explosionCenter, 5, 1, 1, 1, 0);
                    world.spawnParticle(Particle.CLOUD, explosionCenter, 100, 3, 2, 3, 0.5);
                    world.spawnParticle(Particle.SWEEP_ATTACK, explosionCenter, 30, 2, 1, 2, 0);

                    // Projeter et endommager tous les monstres piégés
                    for (Monster m : trappedMonsters) {
                        if (m.isValid() && !m.isDead()) {
                            // Dégâts
                            m.damage(adjustedDamage, player);

                            // Knockback massif vers l'extérieur
                            Vector expulsion = m.getLocation().toVector()
                                .subtract(center.toVector());
                            if (expulsion.lengthSquared() < 0.1) {
                                // Si trop proche du centre, direction aléatoire
                                expulsion = new Vector(
                                    Math.random() - 0.5,
                                    0,
                                    Math.random() - 0.5
                                );
                            }
                            expulsion.normalize().multiply(2.5);
                            expulsion.setY(0.8);
                            m.setVelocity(expulsion);

                            // Effet de désorientation prolongée
                            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, false, false));
                            m.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, false));

                            // Particules sur chaque monstre éjecté
                            world.spawnParticle(Particle.CLOUD, m.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                        }
                    }

                    player.sendMessage("§a[Pet] §7Ennemis éjectés : §e" + trappedMonsters.size());
                    cancel();
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }
}
