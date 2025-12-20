package com.rinaorc.zombiez.classes.beasts;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestionnaire des bêtes de la Voie des Bêtes du Chasseur.
 * Gère l'invocation, le suivi, les capacités et la persistance des bêtes.
 */
public class BeastManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // Metadata keys
    public static final String BEAST_OWNER_KEY = "beast_owner";
    public static final String BEAST_TYPE_KEY = "beast_type";

    // NamespacedKeys for PDC
    private final NamespacedKey ownerKey;
    private final NamespacedKey typeKey;

    // Stockage des bêtes par joueur
    @Getter
    private final Map<UUID, Map<BeastType, UUID>> playerBeasts = new ConcurrentHashMap<>();

    // Entités en attente de respawn (type -> timestamp de respawn)
    private final Map<UUID, Map<BeastType, Long>> pendingRespawn = new ConcurrentHashMap<>();

    // Cible focus par joueur
    private final Map<UUID, UUID> playerFocusTarget = new ConcurrentHashMap<>();

    // Cooldowns des capacités
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new ConcurrentHashMap<>();

    // Frénésie de la Ruche - actif jusqu'à timestamp
    private final Map<UUID, Long> frenzyActiveUntil = new ConcurrentHashMap<>();

    // Double-sneak tracking pour Frénésie
    private final Map<UUID, Long> lastSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW = 400; // ms

    // Mines explosives actives (Vache) - thread-safe
    private final Map<UUID, List<Entity>> activeMines = new ConcurrentHashMap<>();

    // Tracking des bleeds actifs pour éviter le stacking
    private final Set<UUID> activeBleedTargets = ConcurrentHashMap.newKeySet();

    // Respawn delay en ms pour l'Ours
    private static final long BEAR_RESPAWN_DELAY = 10000; // 10 secondes

    public BeastManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        this.ownerKey = new NamespacedKey(plugin, "beast_owner");
        this.typeKey = new NamespacedKey(plugin, "beast_type");

        startBeastTasks();
    }

    /**
     * Calcule les dégâts d'une bête basés sur les stats du joueur propriétaire.
     * Fonctionne comme les minions de l'Occultiste - % des dégâts du joueur.
     *
     * @param owner Le joueur propriétaire de la bête
     * @param type  Le type de bête
     * @return Les dégâts calculés
     */
    public double calculateBeastDamage(Player owner, BeastType type) {
        double baseDamage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        return baseDamage * type.getDamagePercent();
    }

    /**
     * Calcule les dégâts avec multiplicateur de frénésie
     */
    public double calculateBeastDamage(Player owner, BeastType type, double frenzyMultiplier) {
        return calculateBeastDamage(owner, type) * frenzyMultiplier;
    }

    /**
     * Démarre les tâches périodiques pour les bêtes
     */
    private void startBeastTasks() {
        // Tâche principale de mise à jour (20 ticks = 1 seconde)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllBeasts();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Tâche de suivi de formation (10 ticks = 0.5 seconde - équilibre fluidité/perf)
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePackFormation();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // Tâche de vérification de respawn (20 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPendingRespawns();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Invoque toutes les bêtes pour un joueur basé sur ses talents actifs
     */
    public void summonBeastsForPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) {
            return;
        }

        // Vérifier si le joueur a la branche Bêtes sélectionnée (slotIndex 1)
        String branchId = data.getSelectedBranchId();
        if (branchId == null || !branchId.contains("betes")) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Pour chaque tier débloqué, vérifier si le talent bête correspondant est actif
        for (TalentTier tier : TalentTier.values()) {
            if (!tier.isUnlocked(data.getClassLevel().get())) continue;

            Talent talent = talentManager.getActiveTalentForTier(player, tier);
            if (talent == null) continue;

            // Vérifier si c'est un talent de bête
            BeastType beastType = getBeastTypeFromTalent(talent);
            if (beastType == null) continue;

            // Vérifier si la bête existe déjà
            Map<BeastType, UUID> beasts = playerBeasts.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            if (beasts.containsKey(beastType)) {
                UUID beastUuid = beasts.get(beastType);
                Entity existing = Bukkit.getEntity(beastUuid);
                if (existing != null && existing.isValid() && !existing.isDead()) {
                    continue; // La bête existe déjà
                }
            }

            // Vérifier si en attente de respawn
            Map<BeastType, Long> respawns = pendingRespawn.get(uuid);
            if (respawns != null && respawns.containsKey(beastType)) {
                continue; // En attente de respawn
            }

            // Invoquer la bête
            spawnBeast(player, beastType);
        }
    }

    /**
     * Invoque une bête spécifique pour un joueur
     */
    public void spawnBeast(Player player, BeastType type) {
        UUID playerUuid = player.getUniqueId();
        Location spawnLoc = calculatePackPosition(player, type);

        LivingEntity beast = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, type.getEntityType());

        // Configuration de base
        beast.customName(Component.text("Bête de " + player.getName(), NamedTextColor.GOLD));
        beast.setCustomNameVisible(true);

        // Stocker les métadonnées
        beast.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, playerUuid.toString());
        beast.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        beast.setMetadata(BEAST_OWNER_KEY, new FixedMetadataValue(plugin, playerUuid.toString()));
        beast.setMetadata(BEAST_TYPE_KEY, new FixedMetadataValue(plugin, type.name()));

        // Configurer selon le type
        configureBeast(beast, type, player);

        // Stocker la référence
        Map<BeastType, UUID> beasts = playerBeasts.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        beasts.put(type, beast.getUniqueId());

        // Effets visuels d'invocation
        spawnBeastEffects(beast.getLocation(), type);

        // Son d'invocation (pas de message spam - ActionBar serait mieux)
        player.playSound(player.getLocation(), type.getAmbientSound(), 1.0f, 1.0f);
    }

    /**
     * Configure une bête selon son type
     */
    private void configureBeast(LivingEntity beast, BeastType type, Player owner) {
        // Invincibilité si applicable
        if (type.isInvincible()) {
            beast.setInvulnerable(true);
        }

        // Désactiver l'IA par défaut pour contrôle manuel
        if (beast instanceof Mob mob) {
            mob.setAware(true);
            mob.setAggressive(false);
        }

        // Configuration spécifique par type
        switch (type) {
            case BAT -> {
                // Utilise un Vex pour le pathfinding/targeting, stylisé comme chauve-souris
                if (beast instanceof org.bukkit.entity.Vex vex) {
                    vex.setCharging(false);
                    vex.setSilent(true); // Sons custom
                    // Petite taille visuelle via attribut
                    if (vex.getAttribute(Attribute.SCALE) != null) {
                        vex.getAttribute(Attribute.SCALE).setBaseValue(0.6);
                    }
                }
            }
            case BEAR -> {
                // L'ours partage la vie du joueur - on le synchronisera dans updateAllBeasts
                double maxHealth = owner.getAttribute(Attribute.MAX_HEALTH).getValue();
                beast.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
                beast.setHealth(owner.getHealth());
            }
            case WOLF -> {
                if (beast instanceof Wolf wolf) {
                    wolf.setTamed(true);
                    wolf.setOwner(owner);
                    wolf.setSitting(false);
                    wolf.setCollarColor(DyeColor.RED);
                }
            }
            case AXOLOTL -> {
                if (beast instanceof Axolotl axolotl) {
                    axolotl.setVariant(Axolotl.Variant.WILD);
                }
            }
            case COW -> {
                // Pas de config spéciale
            }
            case LLAMA -> {
                if (beast instanceof Llama llama) {
                    llama.setStrength(5);
                    llama.setColor(Llama.Color.BROWN);
                }
            }
            case FOX -> {
                if (beast instanceof Fox fox) {
                    fox.setFoxType(Fox.Type.RED);
                    fox.setSitting(false);
                }
            }
            case BEE -> {
                if (beast instanceof Bee bee) {
                    bee.setAnger(0);
                    bee.setHasStung(false);
                }
            }
            case IRON_GOLEM -> {
                if (beast instanceof IronGolem golem) {
                    golem.setPlayerCreated(true);
                }
            }
        }

        // Désactiver les collisions avec le propriétaire
        beast.setCollidable(false);
    }

    /**
     * Calcule la position dans la formation de meute
     */
    private Location calculatePackPosition(Player player, BeastType type) {
        Location playerLoc = player.getLocation();
        double angle = Math.toRadians(playerLoc.getYaw() + type.getOffsetAngle() + 180);
        double distance = type.getDistanceFromPlayer();

        double x = playerLoc.getX() + Math.sin(angle) * distance;
        double z = playerLoc.getZ() + Math.cos(angle) * distance;

        Location targetLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY(), z);

        // Trouver le sol
        targetLoc = findSafeLocation(targetLoc);

        return targetLoc;
    }

    /**
     * Trouve une position sûre (sur le sol)
     */
    private Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY();

        // Chercher vers le bas puis vers le haut
        for (int dy = 0; dy < 10; dy++) {
            Location checkLoc = new Location(world, x + 0.5, y - dy, z + 0.5);
            if (isSafeSpawn(checkLoc)) {
                return checkLoc;
            }
            checkLoc = new Location(world, x + 0.5, y + dy, z + 0.5);
            if (isSafeSpawn(checkLoc)) {
                return checkLoc;
            }
        }

        return loc;
    }

    private boolean isSafeSpawn(Location loc) {
        return loc.getBlock().isPassable() &&
               loc.clone().add(0, 1, 0).getBlock().isPassable() &&
               !loc.clone().subtract(0, 1, 0).getBlock().isPassable();
    }

    /**
     * Met à jour la formation de la meute
     */
    private void updatePackFormation() {
        for (Map.Entry<UUID, Map<BeastType, UUID>> entry : playerBeasts.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            for (Map.Entry<BeastType, UUID> beastEntry : entry.getValue().entrySet()) {
                Entity entity = Bukkit.getEntity(beastEntry.getValue());
                if (entity == null || entity.isDead() || !(entity instanceof LivingEntity beast)) {
                    continue;
                }

                // Calculer la position cible
                Location targetLoc = calculatePackPosition(player, beastEntry.getKey());
                double distanceToTarget = beast.getLocation().distance(targetLoc);

                // Si trop loin, téléporter (mais pas bêtement sur le joueur)
                if (distanceToTarget > 20) {
                    beast.teleport(targetLoc);
                    continue;
                }

                // Sinon, déplacer avec le pathfinding
                if (distanceToTarget > 2 && beast instanceof Mob mob) {
                    mob.getPathfinder().moveTo(targetLoc, 1.0 + (distanceToTarget / 10.0));
                }
            }
        }
    }

    /**
     * Met à jour toutes les bêtes (capacités, états, etc.)
     */
    private void updateAllBeasts() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<BeastType, UUID>> entry : playerBeasts.entrySet()) {
            UUID playerUuid = entry.getKey();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) continue;

            for (Map.Entry<BeastType, UUID> beastEntry : entry.getValue().entrySet()) {
                BeastType type = beastEntry.getKey();
                Entity entity = Bukkit.getEntity(beastEntry.getValue());
                if (entity == null || entity.isDead() || !(entity instanceof LivingEntity beast)) {
                    continue;
                }

                // Exécuter la capacité de chaque bête
                executeBeastAbility(player, beast, type, now);
            }
        }
    }

    /**
     * Exécute la capacité spécifique d'une bête
     */
    private void executeBeastAbility(Player owner, LivingEntity beast, BeastType type, long now) {
        UUID ownerUuid = owner.getUniqueId();
        String cooldownKey = type.name() + "_ability";
        boolean hasFrenzy = frenzyActiveUntil.containsKey(ownerUuid) && now < frenzyActiveUntil.get(ownerUuid);
        double frenzyMultiplier = hasFrenzy ? 1.5 : 1.0;

        switch (type) {
            case BAT -> executeBatAbility(owner, beast, frenzyMultiplier);
            case BEAR -> executeBearAbility(owner, beast, now, cooldownKey, frenzyMultiplier);
            case WOLF -> executeWolfAbility(owner, beast, frenzyMultiplier);
            case AXOLOTL -> executeAxolotlAbility(owner, beast, now, cooldownKey, frenzyMultiplier);
            case COW -> executeCowAbility(owner, beast, now, cooldownKey);
            case LLAMA -> executeLlamaAbility(owner, beast, now, cooldownKey, frenzyMultiplier);
            case FOX -> {} // Passif - géré dans le listener de mort d'entité
            case BEE -> {} // Actif - géré par double-sneak
            case IRON_GOLEM -> executeIronGolemAbility(owner, beast, now, cooldownKey, frenzyMultiplier);
        }

        // Synchroniser la vie de l'ours avec le joueur
        if (type == BeastType.BEAR) {
            double healthPercent = owner.getHealth() / owner.getAttribute(Attribute.MAX_HEALTH).getValue();
            double bearMaxHealth = beast.getAttribute(Attribute.MAX_HEALTH).getValue();
            beast.setHealth(Math.max(1, healthPercent * bearMaxHealth));
        }
    }

    // === CAPACITÉS SPÉCIFIQUES ===

    private void executeBatAbility(Player owner, LivingEntity bat, double frenzyMultiplier) {
        UUID focusTarget = playerFocusTarget.get(owner.getUniqueId());
        if (focusTarget == null) return;

        Entity target = Bukkit.getEntity(focusTarget);
        if (target == null || target.isDead() || !(target instanceof LivingEntity living)) {
            playerFocusTarget.remove(owner.getUniqueId());
            return;
        }

        // Vérifier la distance
        if (bat.getLocation().distance(target.getLocation()) > 15) return;

        // Attaquer la cible
        if (bat instanceof Mob mob) {
            mob.setTarget(living);
        }

        // Appliquer des dégâts directs (% des dégâts du joueur)
        if (bat.getLocation().distance(target.getLocation()) < 2) {
            living.damage(calculateBeastDamage(owner, BeastType.BAT, frenzyMultiplier), owner);
            bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.5f);
        }
    }

    private void executeBearAbility(Player owner, LivingEntity bear, long now, String cooldownKey, double frenzyMultiplier) {
        // Rugissement toutes les 8 secondes
        long rugissementCooldown = (long) (8000 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            Location loc = bear.getLocation();

            // Effet de rugissement
            bear.getWorld().playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, 2.0f, 0.8f);
            bear.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.add(0, 1, 0), 1, 0, 0, 0, 0);

            // Aggroer les monstres dans 5 blocs (limité à 10 cibles max)
            int aggroCount = 0;
            for (Entity nearby : bear.getNearbyEntities(5, 5, 5)) {
                if (aggroCount >= 10) break; // Limite pour la perf
                if (nearby instanceof Monster monster && !isBeast(nearby)) {
                    if (monster instanceof Mob mob) {
                        mob.setTarget(bear);
                    }
                    // Effet visuel
                    nearby.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                        nearby.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0);
                    aggroCount++;
                }
            }

            // Pas de message spam - juste effet visuel/sonore
            setCooldown(owner.getUniqueId(), cooldownKey, now + rugissementCooldown);
        }
    }

    private void executeWolfAbility(Player owner, LivingEntity wolf, double frenzyMultiplier) {
        if (!(wolf instanceof Wolf w)) return;

        // Le loup cible automatiquement l'ennemi le plus proche
        LivingEntity currentTarget = w.getTarget();

        // Ne recalcule que si pas de cible ou cible morte
        if (currentTarget != null && !currentTarget.isDead() && currentTarget.getLocation().distance(wolf.getLocation()) < 10) {
            return;
        }

        // Chercher le plus proche (early exit dès qu'on trouve)
        Entity nearestEnemy = null;
        double nearestDistance = 8.0;

        for (Entity nearby : wolf.getNearbyEntities(8, 4, 8)) {
            if (nearby instanceof Monster && !isBeast(nearby)) {
                double dist = wolf.getLocation().distanceSquared(nearby.getLocation()); // Squared = plus rapide
                if (dist < nearestDistance * nearestDistance) {
                    nearestDistance = Math.sqrt(dist);
                    nearestEnemy = nearby;
                    if (nearestDistance < 3) break; // Assez proche, on prend
                }
            }
        }

        if (nearestEnemy instanceof LivingEntity target) {
            w.setTarget(target);
        }
    }

    /**
     * Applique le saignement du loup (appelé quand le loup attaque)
     * Empêche le stacking de plusieurs bleeds sur la même cible
     */
    public void applyWolfBleed(Player owner, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();

        // Éviter le stacking de bleeds
        if (activeBleedTargets.contains(targetUuid)) {
            return;
        }
        activeBleedTargets.add(targetUuid);

        // Appliquer un DoT pendant 5 secondes
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 5 || target.isDead()) {
                    activeBleedTargets.remove(targetUuid);
                    cancel();
                    return;
                }

                // DoT = 15% des dégâts du joueur par tick (5 ticks = 75% total)
                double bleedDamage = calculateBeastDamage(owner, BeastType.WOLF) * 0.5; // 15% = 30% * 0.5
                target.damage(bleedDamage, owner);
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                    5, 0.2, 0.3, 0.2, Material.REDSTONE_BLOCK.createBlockData());
                // Son uniquement toutes les 2 ticks pour réduire le spam
                if (ticks % 2 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.3f, 1.2f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Chaque seconde

        // Message uniquement si c'est une cible importante (pas de spam)
    }

    private void executeAxolotlAbility(Player owner, LivingEntity axolotl, long now, String cooldownKey, double frenzyMultiplier) {
        // Tirer des bulles toutes les 1.5 secondes
        long shootCooldown = (long) (1500 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver la cible la plus proche (avec early-exit)
            LivingEntity nearestEnemy = null;
            double nearestDistSq = 64.0; // 8^2

            for (Entity nearby : axolotl.getNearbyEntities(8, 4, 8)) {
                if (nearby instanceof Monster monster && !isBeast(nearby)) {
                    double distSq = axolotl.getLocation().distanceSquared(nearby.getLocation());
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestEnemy = monster;
                        if (distSq < 9) break; // <3 blocs, on prend direct
                    }
                }
            }

            if (nearestEnemy != null) {
                // Tirer une bulle d'eau
                shootWaterBubble(owner, axolotl, nearestEnemy);
                setCooldown(owner.getUniqueId(), cooldownKey, now + shootCooldown);
            }
        }
    }

    private void shootWaterBubble(Player owner, LivingEntity axolotl, LivingEntity target) {
        Location start = axolotl.getLocation().add(0, 0.5, 0);
        Vector direction = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        // Créer un projectile visuel avec des particules
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20) {
                    cancel();
                    return;
                }

                // Déplacer la bulle
                current.add(direction.clone().multiply(0.8));

                // Particules de bulle
                current.getWorld().spawnParticle(Particle.BUBBLE_POP, current, 5, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.DRIPPING_WATER, current, 3, 0.1, 0.1, 0.1, 0);

                // Vérifier l'impact
                for (Entity entity : current.getWorld().getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                    if (entity instanceof LivingEntity living && entity != axolotl && !isBeast(entity) && entity != owner) {
                        living.damage(calculateBeastDamage(owner, BeastType.AXOLOTL), owner);
                        current.getWorld().playSound(current, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.5f);
                        current.getWorld().spawnParticle(Particle.SPLASH, current, 20, 0.3, 0.3, 0.3, 0);
                        cancel();
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        axolotl.getWorld().playSound(axolotl.getLocation(), Sound.ENTITY_AXOLOTL_SPLASH, 1.0f, 1.2f);
    }

    private void executeCowAbility(Player owner, LivingEntity cow, long now, String cooldownKey) {
        // Poser une mine toutes les 15 secondes
        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            spawnExplosiveMine(owner, cow);
            setCooldown(owner.getUniqueId(), cooldownKey, now + 15000);
        }
    }

    private void spawnExplosiveMine(Player owner, LivingEntity cow) {
        Location mineLoc = cow.getLocation();

        // Créer un item au sol comme "mine"
        Item mineItem = cow.getWorld().dropItem(mineLoc, new org.bukkit.inventory.ItemStack(Material.BROWN_DYE));
        mineItem.setPickupDelay(Integer.MAX_VALUE);
        mineItem.setCustomNameVisible(true);
        mineItem.customName(Component.text("💣 Mine", NamedTextColor.DARK_RED));
        mineItem.setGlowing(true);
        mineItem.setVelocity(new Vector(0, 0, 0));

        UUID ownerUuid = owner.getUniqueId();
        List<Entity> mines = activeMines.computeIfAbsent(ownerUuid, k -> new CopyOnWriteArrayList<>());
        mines.add(mineItem);

        // Son de placement (pas de message pour réduire le spam)
        cow.getWorld().playSound(mineLoc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 0.5f);

        // Vérifier les contacts
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (mineItem.isDead() || ticks > 600) { // 30 secondes max
                    mineItem.remove();
                    mines.remove(mineItem);
                    cancel();
                    return;
                }

                // Vérifier si un ennemi marche dessus
                for (Entity nearby : mineItem.getNearbyEntities(1.5, 1, 1.5)) {
                    if (nearby instanceof Monster && !isBeast(nearby)) {
                        // EXPLOSION!
                        Location explosionLoc = mineItem.getLocation();
                        explosionLoc.getWorld().createExplosion(explosionLoc, 0, false, false); // Effet visuel
                        explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 1);
                        explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

                        // Dégâts et knockback (80% des dégâts du joueur)
                        double mineDamage = calculateBeastDamage(owner, BeastType.COW);
                        for (Entity damaged : explosionLoc.getWorld().getNearbyEntities(explosionLoc, 3, 3, 3)) {
                            if (damaged instanceof LivingEntity living && !isBeast(damaged) && damaged != owner) {
                                living.damage(mineDamage, owner);
                                Vector knockback = living.getLocation().subtract(explosionLoc).toVector().normalize().multiply(1.5);
                                knockback.setY(0.5);
                                living.setVelocity(knockback);
                            }
                        }

                        mineItem.remove();
                        mines.remove(mineItem);
                        // Pas de message - l'effet visuel suffit
                        cancel();
                        return;
                    }
                }

                // Particules d'avertissement
                if (ticks % 10 == 0) {
                    mineItem.getWorld().spawnParticle(Particle.SMOKE, mineItem.getLocation().add(0, 0.3, 0), 3, 0.1, 0.1, 0.1, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private void executeLlamaAbility(Player owner, LivingEntity llama, long now, String cooldownKey, double frenzyMultiplier) {
        // Cracher toutes les 3 secondes
        long spitCooldown = (long) (3000 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver jusqu'à 3 cibles (déjà limité naturellement)
            List<LivingEntity> targets = new ArrayList<>(3);
            for (Entity nearby : llama.getNearbyEntities(6, 4, 6)) {
                if (nearby instanceof Monster monster && !isBeast(nearby)) {
                    targets.add(monster);
                    if (targets.size() >= 3) break; // Limite atteinte
                }
            }

            if (!targets.isEmpty()) {
                for (LivingEntity target : targets) {
                    spitAtTarget(owner, llama, target);
                }
                setCooldown(owner.getUniqueId(), cooldownKey, now + spitCooldown);
            }
        }
    }

    private void spitAtTarget(Player owner, LivingEntity llama, LivingEntity target) {
        // Spawn un crachat de lama
        LlamaSpit spit = (LlamaSpit) llama.getWorld().spawnEntity(
            llama.getLocation().add(0, 1, 0), EntityType.LLAMA_SPIT);
        spit.setShooter(llama);

        Vector direction = target.getLocation().add(0, 1, 0).subtract(llama.getLocation().add(0, 1, 0)).toVector().normalize();
        spit.setVelocity(direction.multiply(1.5));

        // Effet sur impact (géré dans le listener)
        spit.setMetadata("llama_owner", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

        llama.getWorld().playSound(llama.getLocation(), Sound.ENTITY_LLAMA_SPIT, 1.0f, 1.0f);
    }

    /**
     * Applique l'effet du crachat du lama (appelé par le listener)
     */
    public void applyLlamaSpit(Player owner, LivingEntity target) {
        target.damage(calculateBeastDamage(owner, BeastType.LLAMA), owner);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1)); // Lenteur II, 3s
        target.getWorld().spawnParticle(Particle.SPIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
    }

    private void executeIronGolemAbility(Player owner, LivingEntity golem, long now, String cooldownKey, double frenzyMultiplier) {
        // Onde de choc toutes les 12 secondes
        long shockwaveCooldown = (long) (12000 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            executeShockwave(owner, golem);
            setCooldown(owner.getUniqueId(), cooldownKey, now + shockwaveCooldown);
        }
    }

    private void executeShockwave(Player owner, LivingEntity golem) {
        Location center = golem.getLocation();
        // Pas de message - effet visuel/sonore suffit

        // Animation de préparation
        golem.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);

        // Phase 1: Vortex (1.5 secondes)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 30) { // 1.5 secondes
                    // Phase 2: EXPLOSION!
                    executeShockwaveExplosion(owner, golem, center);
                    cancel();
                    return;
                }

                // Attirer les ennemis (limité à 15 pour perf)
                int pulled = 0;
                for (Entity nearby : center.getWorld().getNearbyEntities(center, 6, 3, 6)) {
                    if (pulled >= 15) break;
                    if (nearby instanceof LivingEntity living && nearby != golem && !isBeast(nearby) && nearby != owner) {
                        Vector pull = center.toVector().subtract(nearby.getLocation().toVector()).normalize().multiply(0.15);
                        nearby.setVelocity(nearby.getVelocity().add(pull));
                        pulled++;
                    }
                }

                // Particules de vortex
                double angle = ticks * 0.3;
                for (int i = 0; i < 8; i++) {
                    double a = angle + (i * Math.PI / 4);
                    double r = 4.0 * (1.0 - ticks / 30.0);
                    double x = Math.cos(a) * r;
                    double z = Math.sin(a) * r;
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.5, z), 2, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 150, 150), 1.5f));
                }

                // Son du vortex
                if (ticks % 5 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeShockwaveExplosion(Player owner, LivingEntity golem, Location center) {
        // Son d'explosion
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        center.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);

        // Particules d'explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 30, 3, 1, 3, 0);
        center.getWorld().spawnParticle(Particle.DUST, center, 100, 4, 2, 4, 0,
            new Particle.DustOptions(Color.fromRGB(100, 100, 100), 2.0f));

        // Dégâts et projection (limité à 20 cibles pour perf)
        int damaged = 0;
        for (Entity nearby : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
            if (damaged >= 20) break;
            if (nearby instanceof LivingEntity living && nearby != golem && !isBeast(nearby) && nearby != owner) {
                // Dégâts massifs (50% des dégâts du joueur)
                living.damage(calculateBeastDamage(owner, BeastType.IRON_GOLEM), owner);

                // Projection en l'air
                Vector knockback = living.getLocation().subtract(center).toVector().normalize().multiply(2.0);
                knockback.setY(1.2);
                living.setVelocity(knockback);

                // Effets visuels sur la cible
                living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.2);
                damaged++;
            }
        }
        // Pas de message - effets visuels suffisent
    }

    // === FRÉNÉSIE DE LA RUCHE (Abeille) ===

    /**
     * Gère le double-sneak pour activer la Frénésie
     */
    public void handleSneak(Player player, boolean isSneaking) {
        if (!isSneaking) return;

        UUID uuid = player.getUniqueId();

        // Vérifier si le joueur a l'abeille
        Map<BeastType, UUID> beasts = playerBeasts.get(uuid);
        if (beasts == null || !beasts.containsKey(BeastType.BEE)) return;

        long now = System.currentTimeMillis();
        Long lastSneak = lastSneakTime.get(uuid);

        if (lastSneak != null && now - lastSneak <= DOUBLE_SNEAK_WINDOW) {
            // Double-sneak détecté!
            activateFrenzy(player);
            lastSneakTime.remove(uuid);
        } else {
            lastSneakTime.put(uuid, now);
        }
    }

    private void activateFrenzy(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cooldown
        if (isOnCooldown(uuid, "frenzy", System.currentTimeMillis())) {
            player.sendMessage("§c✦ Frénésie en cooldown!");
            return;
        }

        // Activer la frénésie
        frenzyActiveUntil.put(uuid, System.currentTimeMillis() + 10000); // 10 secondes
        setCooldown(uuid, "frenzy", System.currentTimeMillis() + 20000); // 20 secondes de cooldown

        // Effets
        player.sendMessage("§e§l✦ FRÉNÉSIE DE LA RUCHE ACTIVÉE! +50% vitesse d'attaque pour 10s!");
        player.playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 2.0f, 1.5f);

        // Particules sur toutes les bêtes
        Map<BeastType, UUID> beasts = playerBeasts.get(uuid);
        if (beasts != null) {
            for (UUID beastUuid : beasts.values()) {
                Entity beast = Bukkit.getEntity(beastUuid);
                if (beast != null) {
                    beast.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, beast.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                }
            }
        }
    }

    // === RENARD - CHASSEUR DE TRÉSORS ===

    /**
     * Vérifie si le renard trouve un trésor (appelé à la mort d'un mob)
     */
    public void checkFoxTreasure(Player owner, Location deathLoc) {
        UUID uuid = owner.getUniqueId();
        Map<BeastType, UUID> beasts = playerBeasts.get(uuid);
        if (beasts == null || !beasts.containsKey(BeastType.FOX)) return;

        Entity fox = Bukkit.getEntity(beasts.get(BeastType.FOX));
        if (fox == null || fox.getLocation().distance(deathLoc) > 10) return;

        // 20% de chance
        if (Math.random() > 0.20) return;

        // Choisir Force ou Vitesse
        boolean isStrength = Math.random() > 0.5;

        if (isStrength) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0)); // Force I, 10s
            owner.sendMessage("§6✦ Le Renard déterre: §c+Force§6 pour 10s!");
        } else {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0)); // Vitesse I, 10s
            owner.sendMessage("§6✦ Le Renard déterre: §b+Vitesse§6 pour 10s!");
        }

        // Effets
        fox.getWorld().playSound(fox.getLocation(), Sound.ENTITY_FOX_SCREECH, 1.0f, 1.2f);
        fox.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, fox.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0);
    }

    // === GESTION DE LA MORT ET RESPAWN ===

    /**
     * Gère la mort d'une bête
     */
    public void handleBeastDeath(LivingEntity beast) {
        String ownerUuidStr = beast.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        String typeStr = beast.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        if (ownerUuidStr == null || typeStr == null) return;

        UUID ownerUuid = UUID.fromString(ownerUuidStr);
        BeastType type = BeastType.valueOf(typeStr);

        // Retirer des bêtes actives
        Map<BeastType, UUID> beasts = playerBeasts.get(ownerUuid);
        if (beasts != null) {
            beasts.remove(type);
        }

        Player owner = Bukkit.getPlayer(ownerUuid);

        // L'Ours respawn après 10 secondes
        if (type == BeastType.BEAR && owner != null) {
            owner.sendMessage("§c✦ L'Ours est tombé! Respawn dans 10 secondes...");
            Map<BeastType, Long> respawns = pendingRespawn.computeIfAbsent(ownerUuid, k -> new ConcurrentHashMap<>());
            respawns.put(type, System.currentTimeMillis() + BEAR_RESPAWN_DELAY);
        }
    }

    /**
     * Vérifie les respawns en attente
     */
    private void checkPendingRespawns() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<BeastType, Long>> entry : pendingRespawn.entrySet()) {
            UUID playerUuid = entry.getKey();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) continue;

            Iterator<Map.Entry<BeastType, Long>> it = entry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BeastType, Long> respawnEntry = it.next();
                if (now >= respawnEntry.getValue()) {
                    spawnBeast(player, respawnEntry.getKey());
                    it.remove();
                }
            }
        }
    }

    // === PERSISTANCE ===

    /**
     * Retire toutes les bêtes d'un joueur
     */
    public void despawnAllBeasts(Player player) {
        UUID uuid = player.getUniqueId();
        Map<BeastType, UUID> beasts = playerBeasts.remove(uuid);
        if (beasts == null) return;

        for (UUID beastUuid : beasts.values()) {
            Entity entity = Bukkit.getEntity(beastUuid);
            if (entity != null) {
                entity.remove();
            }
        }

        pendingRespawn.remove(uuid);
        activeMines.remove(uuid);
    }

    /**
     * Définit la cible focus du joueur (pour la chauve-souris)
     */
    public void setFocusTarget(Player player, LivingEntity target) {
        if (target == null) {
            playerFocusTarget.remove(player.getUniqueId());
        } else {
            playerFocusTarget.put(player.getUniqueId(), target.getUniqueId());
        }
    }

    // === UTILITAIRES ===

    public boolean isBeast(Entity entity) {
        return entity.hasMetadata(BEAST_OWNER_KEY);
    }

    public UUID getBeastOwner(Entity entity) {
        if (!entity.hasMetadata(BEAST_OWNER_KEY)) return null;
        String uuidStr = entity.getMetadata(BEAST_OWNER_KEY).get(0).asString();
        return UUID.fromString(uuidStr);
    }

    private boolean isOnCooldown(UUID uuid, String ability, long now) {
        Map<String, Long> cooldowns = abilityCooldowns.get(uuid);
        if (cooldowns == null) return false;
        Long until = cooldowns.get(ability);
        return until != null && now < until;
    }

    private void setCooldown(UUID uuid, String ability, long until) {
        Map<String, Long> cooldowns = abilityCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        cooldowns.put(ability, until);
    }

    private BeastType getBeastTypeFromTalent(Talent talent) {
        if (talent == null) return null;
        String id = talent.getId();

        if (id.contains("beast_bat")) return BeastType.BAT;
        if (id.contains("beast_bear")) return BeastType.BEAR;
        if (id.contains("beast_wolf")) return BeastType.WOLF;
        if (id.contains("beast_axolotl")) return BeastType.AXOLOTL;
        if (id.contains("beast_cow")) return BeastType.COW;
        if (id.contains("beast_llama")) return BeastType.LLAMA;
        if (id.contains("beast_fox")) return BeastType.FOX;
        if (id.contains("beast_bee")) return BeastType.BEE;
        if (id.contains("beast_iron_golem")) return BeastType.IRON_GOLEM;

        return null;
    }

    private void spawnBeastEffects(Location loc, BeastType type) {
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 1, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 20, 0.5, 1, 0.5, 0,
            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }

    /**
     * Vérifie si un joueur a une bête spécifique
     */
    public boolean hasBeast(Player player, BeastType type) {
        Map<BeastType, UUID> beasts = playerBeasts.get(player.getUniqueId());
        if (beasts == null) return false;
        UUID beastUuid = beasts.get(type);
        if (beastUuid == null) return false;
        Entity entity = Bukkit.getEntity(beastUuid);
        return entity != null && !entity.isDead();
    }

    /**
     * Obtient toutes les bêtes actives d'un joueur
     */
    public List<LivingEntity> getPlayerBeasts(Player player) {
        List<LivingEntity> result = new ArrayList<>();
        Map<BeastType, UUID> beasts = playerBeasts.get(player.getUniqueId());
        if (beasts == null) return result;

        for (UUID beastUuid : beasts.values()) {
            Entity entity = Bukkit.getEntity(beastUuid);
            if (entity instanceof LivingEntity living && !entity.isDead()) {
                result.add(living);
            }
        }
        return result;
    }
}
