package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.ArmorTrimGenerator;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.worldboss.procedural.BossModifiers;
import com.rinaorc.zombiez.worldboss.procedural.BossTrait;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
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

    // Modificateurs procéduraux (rendent chaque boss unique)
    protected BossModifiers modifiers;

    // Entité du boss
    protected Zombie entity;
    protected BossBar bossBar;

    // État
    protected boolean active = false;
    protected boolean dead = false;
    protected long spawnTime;
    protected long lastPlayerNearby;

    // État du trait PHASING (intangibilité temporaire)
    protected boolean isPhasing = false;
    protected long phasingEndTime = 0;

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
        spawn(location, null);
    }

    /**
     * Spawn le boss avec des modificateurs procéduraux
     */
    public void spawn(Location location, BossModifiers modifiers) {
        if (active) return;

        World world = location.getWorld();
        if (world == null) return;

        // Générer des modificateurs procéduraux si non fournis
        if (modifiers == null) {
            modifiers = BossModifiers.generate(type);
        }
        this.modifiers = modifiers;

        // Créer le zombie
        entity = world.spawn(location, Zombie.class, zombie -> {
            zombie.setAdult();
            zombie.setCanPickupItems(false);
            zombie.setRemoveWhenFarAway(false); // Ne despawn pas naturellement
            zombie.setPersistent(true);

            // Nom procédural unique
            String bossName = this.modifiers.getName().titleName() + " §c[WORLD BOSS]";
            zombie.setCustomName(bossName);
            zombie.setCustomNameVisible(true);

            // Stats avec modificateurs procéduraux
            var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double health = type.calculateHealth(zoneId) * this.modifiers.getHealthMultiplier();
                maxHealth.setBaseValue(health);
                zombie.setHealth(health);
            }

            var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(type.calculateDamage(zoneId) * this.modifiers.getDamageMultiplier());
            }

            var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.25 * this.modifiers.getSpeedMultiplier());
            }

            var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(this.modifiers.getKnockbackResistance());
            }

            // Scale (taille) avec variation procédurale
            var scale = zombie.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(type.getScale() * this.modifiers.getScaleMultiplier());
            }

            // Armor bonus procédural
            var armor = zombie.getAttribute(Attribute.ARMOR);
            if (armor != null) {
                armor.setBaseValue(this.modifiers.getArmorBonus());
            }

            // Résistances
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

            // Trait spécial: Implacable = pas de knockback
            if (this.modifiers.hasTrait(BossTrait.RELENTLESS)) {
                var kb = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (kb != null) kb.setBaseValue(1.0);
            }

            // Tags pour identification
            zombie.addScoreboardTag("world_boss");
            zombie.addScoreboardTag("boss_" + bossId.toString());
            zombie.addScoreboardTag("boss_type_" + type.name());
            zombie.addScoreboardTag("boss_seed_" + this.modifiers.getSeed());

            // Glowing permanent
            zombie.setGlowing(true);

            // Équiper une armure colorée selon les traits procéduraux
            equipColoredArmor(zombie);
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
     * Équipe le boss avec une armure en cuir colorée et des trims selon ses traits procéduraux
     * Rend chaque boss visuellement unique
     */
    protected void equipColoredArmor(Zombie zombie) {
        if (modifiers == null) return;

        EntityEquipment equipment = zombie.getEquipment();
        if (equipment == null) return;

        Color primary = modifiers.getPrimaryColor();
        Color secondary = modifiers.getSecondaryColor();

        // Générer un trim pour le boss (basé sur zone et difficulté)
        ArmorTrim trim = generateBossTrim();

        // Casque - couleur primaire + trim
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setColor(primary);
            if (trim != null) {
                helmetMeta.setTrim(trim);
            }
            helmet.setItemMeta(helmetMeta);
        }
        equipment.setHelmet(helmet);

        // Plastron - couleur primaire + trim
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(primary);
            if (trim != null) {
                chestMeta.setTrim(trim);
            }
            chestplate.setItemMeta(chestMeta);
        }
        equipment.setChestplate(chestplate);

        // Jambières - couleur secondaire + trim
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setColor(secondary);
            if (trim != null) {
                leggingsMeta.setTrim(trim);
            }
            leggings.setItemMeta(leggingsMeta);
        }
        equipment.setLeggings(leggings);

        // Bottes - couleur secondaire + trim
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setColor(secondary);
            if (trim != null) {
                bootsMeta.setTrim(trim);
            }
            boots.setItemMeta(bootsMeta);
        }
        equipment.setBoots(boots);

        // Empêcher le drop de l'équipement
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
    }

    /**
     * Génère un trim d'armure pour le boss basé sur sa zone et difficulté
     * Les boss plus difficiles ont des trims plus prestigieux
     */
    private ArmorTrim generateBossTrim() {
        try {
            // Déterminer la rareté du trim selon la zone et le nombre de traits
            Rarity trimRarity = determineTrimRarity();

            // Utiliser ArmorTrimGenerator pour générer un trim
            ArmorTrimGenerator.TrimResult trimResult = ArmorTrimGenerator.getInstance()
                .generateTrimForMob(trimRarity, zoneId);

            if (trimResult != null) {
                return new ArmorTrim(trimResult.material(), trimResult.pattern());
            }
        } catch (Exception e) {
            // En cas d'erreur (version incompatible, etc.), pas de trim
            plugin.getLogger().fine("[WorldBoss] Impossible de générer un trim: " + e.getMessage());
        }
        return null;
    }

    /**
     * Détermine la rareté du trim basée sur la zone et les traits
     */
    private Rarity determineTrimRarity() {
        // Base: zone détermine le tier minimum
        Rarity baseRarity;
        if (zoneId >= 40) {
            baseRarity = Rarity.LEGENDARY;
        } else if (zoneId >= 25) {
            baseRarity = Rarity.EPIC;
        } else if (zoneId >= 15) {
            baseRarity = Rarity.RARE;
        } else if (zoneId >= 5) {
            baseRarity = Rarity.UNCOMMON;
        } else {
            baseRarity = Rarity.COMMON;
        }

        // Bonus: plus de traits = rareté augmentée
        if (modifiers != null && modifiers.getTraits().length >= 3) {
            // 3 traits = upgrade d'un tier
            baseRarity = upgradeRarity(baseRarity);
        }

        // Bonus: difficulté élevée = chance d'upgrade supplémentaire
        if (modifiers != null && modifiers.getDifficultyMultiplier() > 1.3) {
            if (Math.random() < 0.5) {
                baseRarity = upgradeRarity(baseRarity);
            }
        }

        return baseRarity;
    }

    /**
     * Augmente la rareté d'un niveau
     */
    private Rarity upgradeRarity(Rarity current) {
        return switch (current) {
            case COMMON -> Rarity.UNCOMMON;
            case UNCOMMON -> Rarity.RARE;
            case RARE -> Rarity.EPIC;
            case EPIC -> Rarity.LEGENDARY;
            default -> current; // Déjà au max
        };
    }

    /**
     * Crée la boss bar avec nom procédural et indicateurs de traits
     */
    protected void createBossBar() {
        // Couleur basée sur le trait principal (ou type si pas de traits)
        BarColor color = getBarColorFromTraits();

        // Construire le titre avec traits
        String bossName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();
        String traitsIndicator = getTraitsIndicator();

        bossBar = Bukkit.createBossBar(
            bossName + traitsIndicator + " §7- §c" + getFormattedHealth(),
            color,
            BarStyle.SEGMENTED_10
        );
        bossBar.setVisible(true);
    }

    /**
     * Détermine la couleur de la boss bar selon les traits
     */
    private BarColor getBarColorFromTraits() {
        if (modifiers == null || modifiers.getTraits().length == 0) {
            return switch (type) {
                case THE_BUTCHER -> BarColor.RED;
                case SHADOW_UNSTABLE -> BarColor.PURPLE;
                case PYROMANCER -> BarColor.YELLOW;
                case HORDE_QUEEN -> BarColor.PINK;
                case ICE_BREAKER -> BarColor.BLUE;
            };
        }

        // Couleur basée sur le premier trait
        BossTrait firstTrait = modifiers.getTraits()[0];
        return switch (firstTrait) {
            case ENRAGED, BERSERKER, BURNING -> BarColor.RED;
            case VENOMOUS, REGENERATING -> BarColor.GREEN;
            case VAMPIRIC, CURSED -> BarColor.PURPLE;
            case FROZEN, ICE_BREAKER -> BarColor.BLUE;
            case SWIFT, EMPOWERED -> BarColor.YELLOW;
            case EXPLOSIVE, STORMY -> BarColor.YELLOW;
            case ARMORED, THORNS, RELENTLESS -> BarColor.WHITE;
            case TELEPORTER, PHASING -> BarColor.PINK;
        };
    }

    /**
     * Génère les indicateurs visuels de traits pour la boss bar
     * Utilise des emojis/symboles pour représenter chaque catégorie de trait
     */
    private String getTraitsIndicator() {
        if (modifiers == null || modifiers.getTraits().length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(" §8[");
        for (int i = 0; i < modifiers.getTraits().length; i++) {
            if (i > 0) sb.append(" ");
            BossTrait trait = modifiers.getTraits()[i];
            sb.append(getTraitSymbol(trait));
        }
        sb.append("§8]");
        return sb.toString();
    }

    /**
     * Retourne un symbole coloré pour chaque trait
     */
    private String getTraitSymbol(BossTrait trait) {
        return switch (trait) {
            case ENRAGED -> "§c⚔";      // Épée rouge
            case BERSERKER -> "§4💀";   // Crâne rouge foncé
            case VENOMOUS -> "§2☠";     // Poison vert
            case EXPLOSIVE -> "§6💥";   // Explosion orange
            case ARMORED -> "§7🛡";     // Bouclier gris
            case REGENERATING -> "§a❤"; // Cœur vert
            case VAMPIRIC -> "§5🦇";    // Chauve-souris violette
            case THORNS -> "§8⚡";      // Épines grises
            case SWIFT -> "§e💨";       // Vent jaune
            case TELEPORTER -> "§d✦";   // Étoile rose
            case PHASING -> "§f👻";     // Fantôme blanc
            case EMPOWERED -> "§b✧";    // Étoile cyan
            case RELENTLESS -> "§4🔥";  // Flamme rouge
            case CURSED -> "§5☽";       // Lune violette
            case BURNING -> "§6🔥";     // Flamme orange
            case FROZEN -> "§b❄";       // Flocon cyan
            case STORMY -> "§9⚡";       // Éclair bleu
        };
    }

    /**
     * Génère une description lisible des multiplicateurs de stats
     * Format: "§c+50% dégâts §7| §a+30% vie §7| §e+20% vitesse"
     */
    private String getStatsDescription() {
        if (modifiers == null) return "§7Normal";

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        // Dégâts
        double damageMult = modifiers.getDamageMultiplier();
        if (Math.abs(damageMult - 1.0) > 0.05) {
            if (!first) sb.append(" §7| ");
            first = false;
            int percent = (int) Math.round((damageMult - 1.0) * 100);
            String color = percent > 0 ? "§c" : "§a";
            sb.append(color).append(percent > 0 ? "+" : "").append(percent).append("% dégâts");
        }

        // Vie
        double healthMult = modifiers.getHealthMultiplier();
        if (Math.abs(healthMult - 1.0) > 0.05) {
            if (!first) sb.append(" §7| ");
            first = false;
            int percent = (int) Math.round((healthMult - 1.0) * 100);
            String color = percent > 0 ? "§c" : "§a";
            sb.append(color).append(percent > 0 ? "+" : "").append(percent).append("% vie");
        }

        // Vitesse
        double speedMult = modifiers.getSpeedMultiplier();
        if (Math.abs(speedMult - 1.0) > 0.05) {
            if (!first) sb.append(" §7| ");
            first = false;
            int percent = (int) Math.round((speedMult - 1.0) * 100);
            String color = percent > 0 ? "§e" : "§a";
            sb.append(color).append(percent > 0 ? "+" : "").append(percent).append("% vitesse");
        }

        // Cooldown capacité (inversé - plus bas = plus rapide)
        double abilityMult = modifiers.getAbilityCooldownMultiplier();
        if (Math.abs(abilityMult - 1.0) > 0.05) {
            if (!first) sb.append(" §7| ");
            first = false;
            // Inverser la logique: 0.7x cooldown = 30% plus rapide
            int percent = (int) Math.round((1.0 - abilityMult) * 100);
            String color = percent > 0 ? "§b" : "§7";
            sb.append(color).append(percent > 0 ? "+" : "").append(percent).append("% capacité");
        }

        // Si aucun modificateur significatif
        if (first) {
            return "§7Équilibré";
        }

        return sb.toString();
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

        String bossDisplayName = modifiers != null ? modifiers.getName().displayName() : type.getDisplayName();
        String bossTitleName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();

        for (Entity e : world.getNearbyEntities(location, ALERT_RADIUS, ALERT_RADIUS, ALERT_RADIUS)) {
            if (e instanceof Player player) {
                // Title
                player.sendTitle(
                    "§c§lWORLD BOSS!",
                    "§7" + bossDisplayName + " §7est proche de vous!",
                    10, 60, 20
                );

                // Son d'alerte
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
                player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2f, 0.5f);

                // Message chat avec traits procéduraux
                player.sendMessage("");
                player.sendMessage("§c§l⚠ ALERTE WORLD BOSS ⚠");
                player.sendMessage("§7" + bossTitleName + " §7vient d'apparaître!");
                player.sendMessage("§7Capacité: §e" + type.getAbilityDescription());

                // Afficher les stats procédurales
                if (modifiers != null) {
                    player.sendMessage("§7Traits: " + modifiers.getTraitsDescription());
                    player.sendMessage("§7Stats: " + getStatsDescription());
                }

                player.sendMessage("§7Le boss brille pour être visible!");
                player.sendMessage("");
            }
        }
    }

    /**
     * Démarre les tâches récurrentes
     */
    protected void startTasks() {
        // Tâche d'utilisation des capacités (avec cooldown procédural)
        if (type.getAbilityCooldownSeconds() > 0) {
            // Calculer le cooldown avec le modificateur procédural
            double cooldownMult = modifiers != null ? modifiers.getAbilityCooldownMultiplier() : 1.0;
            long cooldownTicks = (long) (type.getAbilityCooldownSeconds() * 20L * cooldownMult);

            abilityTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active || entity == null || !entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }
                    useAbility();
                }
            }.runTaskTimer(plugin, cooldownTicks, cooldownTicks);
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

        // Nom procédural + traits + santé
        String bossName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();
        String traitsIndicator = getTraitsIndicator();
        bossBar.setTitle(bossName + traitsIndicator + " §7- §c" + getFormattedHealth());
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
     * Particules ambiantes procédurales
     */
    protected void ambientParticles() {
        if (entity == null) return;

        double scale = modifiers != null ? type.getScale() * modifiers.getScaleMultiplier() : type.getScale();
        Location loc = entity.getLocation().add(0, 1.5 * scale, 0);
        World world = loc.getWorld();
        if (world == null) return;

        // Particules procédurales basées sur les modificateurs
        if (modifiers != null) {
            // Particule ambiante procédurale
            Particle ambient = modifiers.getAmbientParticle();
            Color primary = modifiers.getPrimaryColor();
            float size = modifiers.getParticleSize();

            if (ambient == Particle.DUST) {
                world.spawnParticle(Particle.DUST, loc, 8, 0.6, 0.6, 0.6,
                    new Particle.DustOptions(primary, size));
            } else {
                world.spawnParticle(ambient, loc, 8, 0.5, 0.5, 0.5, 0.02);
            }

            // Particule secondaire basée sur les traits
            if (modifiers.hasTrait(BossTrait.BURNING)) {
                world.spawnParticle(Particle.FLAME, loc, 5, 0.4, 0.4, 0.4, 0.02);
            }
            if (modifiers.hasTrait(BossTrait.FROZEN)) {
                world.spawnParticle(Particle.SNOWFLAKE, loc, 8, 0.4, 0.4, 0.4, 0.01);
            }
            if (modifiers.hasTrait(BossTrait.VENOMOUS)) {
                world.spawnParticle(Particle.DUST, loc, 4, 0.3, 0.3, 0.3,
                    new Particle.DustOptions(Color.GREEN, 1.2f));
            }
            if (modifiers.hasTrait(BossTrait.CURSED)) {
                world.spawnParticle(Particle.SOUL, loc, 3, 0.4, 0.4, 0.4, 0.02);
            }
        } else {
            // Fallback aux particules par type
            switch (type) {
                case THE_BUTCHER -> world.spawnParticle(Particle.DUST, loc, 5,
                    0.5, 0.5, 0.5, new Particle.DustOptions(Color.RED, 2f));
                case SHADOW_UNSTABLE -> world.spawnParticle(Particle.SMOKE, loc, 10, 0.5, 0.5, 0.5, 0.02);
                case PYROMANCER -> world.spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.5, 0.5, 0.02);
                case HORDE_QUEEN -> world.spawnParticle(Particle.WITCH, loc, 5, 0.5, 0.5, 0.5, 0);
                case ICE_BREAKER -> world.spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.5, 0.5, 0.5, 0.02);
            }
        }
    }

    /**
     * Tick appelé chaque seconde - applique les effets procéduraux des traits
     */
    protected void tick() {
        if (entity == null || !entity.isValid() || modifiers == null) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Trait: Régénération
        if (modifiers.getRegenerationRate() > 0) {
            var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null && entity.getHealth() < maxHealth.getValue()) {
                double newHealth = Math.min(maxHealth.getValue(),
                    entity.getHealth() + modifiers.getRegenerationRate());
                entity.setHealth(newHealth);

                // Effet visuel de régénération
                world.spawnParticle(Particle.HEART, bossLoc.clone().add(0, 2, 0), 2, 0.3, 0.3, 0.3, 0);
            }
        }

        // Trait: Ardent (brûle les joueurs proches - dégâts scalent avec zone)
        if (modifiers.hasTrait(BossTrait.BURNING)) {
            // Rayon d'effet scale avec la zone (4 base + 0.1 par zone, max 6)
            double burnRadius = Math.min(6, 4 + zoneId * 0.1);
            // Durée du feu scale avec la zone (40 base + 2 par zone)
            int fireTicks = 40 + zoneId * 2;

            for (Entity e : world.getNearbyEntities(bossLoc, burnRadius, burnRadius, burnRadius)) {
                if (e instanceof Player player) {
                    if (player.getFireTicks() < 20) {
                        player.setFireTicks(fireTicks);
                    }
                }
            }
        }

        // Trait: Glacial (ralentit les joueurs proches - intensité scale avec zone)
        if (modifiers.hasTrait(BossTrait.FROZEN)) {
            // Amplificateur scale avec la zone (1 base, +1 tous les 20 niveaux, max 3)
            int slowAmplifier = Math.min(3, 1 + zoneId / 20);
            // Durée scale avec zone (40 base + 1 par zone)
            int slowDuration = 40 + zoneId;

            for (Entity e : world.getNearbyEntities(bossLoc, 5, 5, 5)) {
                if (e instanceof Player player) {
                    if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, true, false));
                    }
                }
            }
        }

        // Trait: Maudit (applique Darkness aux joueurs proches - durée scale avec zone)
        if (modifiers.hasTrait(BossTrait.CURSED)) {
            // Rayon d'effet scale avec la zone (6 base + 0.1 par zone, max 8)
            double curseRadius = Math.min(8, 6 + zoneId * 0.1);
            // Durée scale avec zone (60 base + 2 par zone)
            int darknessDuration = 60 + zoneId * 2;
            // Amplificateur scale tous les 30 niveaux (0 → 1)
            int darknessAmplifier = Math.min(1, zoneId / 30);

            for (Entity e : world.getNearbyEntities(bossLoc, curseRadius, curseRadius, curseRadius)) {
                if (e instanceof Player player) {
                    if (!player.hasPotionEffect(PotionEffectType.DARKNESS)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessDuration, darknessAmplifier, true, false));
                    }
                }
            }
        }

        // Trait: Orageux (éclairs périodiques - dégâts scalent avec la zone)
        if (modifiers.hasTrait(BossTrait.STORMY) && Math.random() < 0.1) {
            List<Player> nearby = getNearbyPlayers(15);
            if (!nearby.isEmpty()) {
                Player target = nearby.get((int) (Math.random() * nearby.size()));
                world.strikeLightningEffect(target.getLocation());
                double damage = 5 + zoneId * 0.3; // 5 base + 0.3 par niveau de zone
                target.damage(damage);
                target.sendMessage("§9§l⚡ §7La foudre vous frappe!");
            }
        }

        // Trait: Téléporteur (téléportation aléatoire)
        if (modifiers.hasTrait(BossTrait.TELEPORTER) && Math.random() < 0.05) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 5 + Math.random() * 10;
            Location teleportLoc = bossLoc.clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance
            );
            teleportLoc.setY(world.getHighestBlockYAt(teleportLoc) + 1);

            // Effets de téléportation
            world.playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
            world.spawnParticle(Particle.PORTAL, bossLoc, 30, 0.5, 1, 0.5, 0.3);

            entity.teleport(teleportLoc);

            world.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);
            world.spawnParticle(Particle.PORTAL, teleportLoc, 30, 0.5, 1, 0.5, 0.3);
        }

        // Trait: Spectral (devient intangible brièvement - 10% chance, dure 2s)
        if (modifiers.hasTrait(BossTrait.PHASING)) {
            long now = System.currentTimeMillis();

            // Fin de l'intangibilité
            if (isPhasing && now >= phasingEndTime) {
                isPhasing = false;
                entity.setGlowing(true);
                world.playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                world.spawnParticle(Particle.REVERSE_PORTAL, bossLoc, 20, 0.5, 1, 0.5, 0.1);

                for (Player player : getNearbyPlayers(30)) {
                    player.sendMessage("§f" + modifiers.getName().displayName() + " §7redevient tangible!");
                }
            }

            // Déclenchement de l'intangibilité (8% chance par seconde si pas déjà intangible)
            if (!isPhasing && Math.random() < 0.08) {
                isPhasing = true;
                phasingEndTime = now + 2000; // 2 secondes
                entity.setGlowing(false);
                world.playSound(bossLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.2f);
                world.spawnParticle(Particle.REVERSE_PORTAL, bossLoc, 30, 0.5, 1, 0.5, 0.1);

                for (Player player : getNearbyPlayers(30)) {
                    player.sendMessage("§f§l⚠ " + modifiers.getName().displayName() + " §7devient §fintangible§7!");
                }
            }

            // Effet visuel pendant l'intangibilité
            if (isPhasing) {
                world.spawnParticle(Particle.REVERSE_PORTAL, bossLoc.clone().add(0, 1, 0), 5, 0.5, 1, 0.5, 0.05);
            }
        }

        // ==================== SYNERGIES DE TRAITS ====================
        applyTraitSynergies(bossLoc, world);
    }

    /**
     * Applique les effets de synergie entre traits
     * Les synergies sont des bonus spéciaux quand certains traits sont combinés
     */
    protected void applyTraitSynergies(Location bossLoc, World world) {
        if (modifiers == null) return;

        // SYNERGIE: Choc Thermique (FROZEN + BURNING)
        // Les joueurs affectés par le feu ET le ralentissement subissent des dégâts de choc
        if (modifiers.hasTrait(BossTrait.FROZEN) && modifiers.hasTrait(BossTrait.BURNING)) {
            for (Entity e : world.getNearbyEntities(bossLoc, 5, 5, 5)) {
                if (e instanceof Player player) {
                    // Si le joueur est à la fois en feu et ralenti
                    if (player.getFireTicks() > 0 && player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                        // Choc thermique: dégâts + effets visuels
                        double shockDamage = 3 + zoneId * 0.2;
                        player.damage(shockDamage);
                        player.sendMessage("§b§l❄ §6§l🔥 §7Choc Thermique! §c-" + String.format("%.1f", shockDamage) + " PV");

                        // Effets visuels spectaculaires
                        world.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 10, 0.5, 1, 0.5, 0.1);
                        world.spawnParticle(Particle.FLAME, player.getLocation(), 10, 0.5, 1, 0.5, 0.05);
                        world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);

                        // Retire le feu (le choc "consomme" les effets)
                        player.setFireTicks(0);
                    }
                }
            }
        }

        // SYNERGIE: Frénésie Sanguine (BERSERKER + VAMPIRIC)
        // Le lifesteal augmente drastiquement quand le boss est low HP
        // Géré dans WorldBossListener.onBossAttack() - ici on ajoute l'effet visuel
        if (modifiers.hasTrait(BossTrait.BERSERKER) && modifiers.hasLifesteal()) {
            var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double healthPercent = entity.getHealth() / maxHealth.getValue();
                if (healthPercent < 0.3) {
                    // Effet visuel de frénésie
                    world.spawnParticle(Particle.DUST, bossLoc.clone().add(0, 1.5, 0), 8, 0.5, 0.5, 0.5,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f)); // Rouge sang foncé
                }
            }
        }

        // SYNERGIE: Tempête Arcanique (STORMY + CURSED)
        // La foudre a une chance de maudire les joueurs touchés
        // Note: L'effet principal est dans le trait STORMY, ici on ajoute l'effet bonus
        // Implémenté dans le bloc STORMY existant via un check supplémentaire

        // SYNERGIE: Forteresse (ARMORED + THORNS)
        // Réduit les dégâts reçus et augmente le retour de dégâts quand stationnaire
        if (modifiers.hasTrait(BossTrait.ARMORED) && modifiers.hasThorns()) {
            // Effet visuel de forteresse (particules de bouclier)
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.WAX_ON, bossLoc.clone().add(0, 1.2, 0), 5, 0.5, 0.5, 0.5, 0);
            }
        }

        // SYNERGIE: Prédateur Fantôme (PHASING + SWIFT)
        // Après être redevenu tangible, gagne un boost de vitesse temporaire
        if (modifiers.hasTrait(BossTrait.PHASING) && modifiers.hasTrait(BossTrait.SWIFT)) {
            // Quand le boss vient de sortir de phase (dans les dernières 2 secondes)
            long timeSincePhaseEnd = System.currentTimeMillis() - phasingEndTime;
            if (!isPhasing && timeSincePhaseEnd >= 0 && timeSincePhaseEnd < 2000) {
                // Effet visuel de vitesse
                world.spawnParticle(Particle.CLOUD, bossLoc, 3, 0.3, 0.1, 0.3, 0.05);
            }
        }

        // SYNERGIE: Peste Venimeuse (VENOMOUS + CURSED)
        // Le poison devient plus puissant et dure plus longtemps
        // Les joueurs empoisonnés ET maudits ont leur régénération réduite
        if (modifiers.hasTrait(BossTrait.VENOMOUS) && modifiers.hasTrait(BossTrait.CURSED)) {
            for (Entity e : world.getNearbyEntities(bossLoc, 6, 6, 6)) {
                if (e instanceof Player player) {
                    if (player.hasPotionEffect(PotionEffectType.POISON) && player.hasPotionEffect(PotionEffectType.DARKNESS)) {
                        // Appliquer Wither (empêche la régénération naturelle)
                        if (!player.hasPotionEffect(PotionEffectType.WITHER)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, true, false));
                            player.sendMessage("§5§l☠ §7La peste venimeuse vous consume!");
                            world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5,
                                new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.2f)); // Indigo
                        }
                    }
                }
            }
        }
    }

    /**
     * Vérifie si le boss peut recevoir des dégâts
     * Surcharger pour implémenter l'invincibilité (ex: HordeQueen)
     * @return true si le boss peut être endommagé
     */
    public boolean canReceiveDamage() {
        // Trait PHASING: intangible = pas de dégâts
        if (isPhasing) {
            return false;
        }
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

        // Trait: Épineux - renvoie une partie des dégâts
        if (modifiers != null && modifiers.hasThorns()) {
            double thornsDamage = damage * modifiers.getThornsPercent();
            attacker.damage(thornsDamage);
            attacker.sendMessage("§8§l⚔ §7Les épines vous renvoient §c" + String.format("%.1f", thornsDamage) + " §7dégâts!");

            World world = entity.getWorld();
            world.spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
            world.playSound(attacker.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.8f, 1f);
        }

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
     * Effets visuels lors de la mort (avec effets procéduraux)
     */
    protected void deathEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Sons
        world.playSound(location, Sound.ENTITY_WITHER_DEATH, 2f, 1f);
        world.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);

        // Particules standards
        world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 10, 3, 3, 3, 0);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 200, 3, 3, 3, 0.5);
        world.spawnParticle(Particle.DRAGON_BREATH, location, 100, 2, 2, 2, 0.1);

        // Trait: Explosif - explosion à la mort
        if (modifiers != null && modifiers.hasExplosiveDeaths()) {
            world.createExplosion(location, 4f, false, false);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 20, 4, 4, 4, 0);

            // Dégâts aux joueurs proches
            for (Entity e : world.getNearbyEntities(location, 6, 6, 6)) {
                if (e instanceof Player player) {
                    double distance = player.getLocation().distance(location);
                    double damage = 15 * (1 - distance / 6);
                    if (damage > 0) {
                        player.damage(damage);
                        player.sendMessage("§6§l💥 §7L'explosion du boss vous inflige §c" +
                            String.format("%.1f", damage) + " §7dégâts!");
                    }
                }
            }
        }

        // Particules procédurales de couleur
        if (modifiers != null) {
            Color color = modifiers.getPrimaryColor();
            world.spawnParticle(Particle.DUST, location, 100, 4, 4, 4,
                new Particle.DustOptions(color, 2.5f));
        }
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

        // Points avec scaling de difficulté procédurale
        double difficultyMult = modifiers != null ? modifiers.getDifficultyMultiplier() : 1.0;
        long basePoints = (long) ((1000 + (zoneId * 50L)) * difficultyMult);

        if (topPlayer != null) {
            plugin.getEconomyManager().addPoints(topPlayer, basePoints, "World Boss - Top Damager");
            topPlayer.sendMessage("§a§l+++ BONUS TOP DAMAGER +++");
            topPlayer.sendMessage("§7Vous avez infligé le plus de dégâts!");
            if (difficultyMult > 1.1) {
                topPlayer.sendMessage("§6§l⚡ Bonus difficulté: §e+" + String.format("%.0f", (difficultyMult - 1) * 100) + "%");
            }
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
     * Le loot scale avec la difficulté procédurale du boss
     */
    protected void dropLoot(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        var itemManager = plugin.getItemManager();
        if (itemManager == null) return;

        // Calculer le bonus de difficulté procédurale
        // getDifficultyMultiplier() retourne ~1.0 pour un boss normal, plus pour un boss difficile
        double difficultyMult = modifiers != null ? modifiers.getDifficultyMultiplier() : 1.0;

        // Luck bonus scale avec la difficulté: 0.5 base + 0.3 par difficulté excédentaire
        // Un boss avec 1.5x difficulté aura 0.5 + 0.15 = 0.65 luckBonus
        double luckBonus = 0.5 + Math.max(0, (difficultyMult - 1.0) * 0.3);

        // Drop 1-3 items selon la zone, +1 si difficulté > 1.3
        int itemCount = 1 + Math.min(2, zoneId / 15);
        if (difficultyMult > 1.3) {
            itemCount++;
        }

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

        // Boss très difficile (>1.5x) = chance d'un second LEGENDARY
        if (difficultyMult > 1.5 && Math.random() < 0.5) {
            ItemStack bonusLegendary = itemManager.generateItem(zoneId, com.rinaorc.zombiez.items.types.Rarity.LEGENDARY, luckBonus);
            if (bonusLegendary != null) {
                world.dropItemNaturally(location, bonusLegendary);
            }
        }

        // Consommables bonus (3-5 selon difficulté)
        int consumableCount = 3 + (difficultyMult > 1.2 ? 1 : 0) + (difficultyMult > 1.4 ? 1 : 0);
        if (plugin.getConsumableManager() != null) {
            for (int i = 0; i < consumableCount; i++) {
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
