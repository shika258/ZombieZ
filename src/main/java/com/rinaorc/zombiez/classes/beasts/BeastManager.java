package com.rinaorc.zombiez.classes.beasts;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
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

    // Mines explosives actives (Vache) - thread-safe
    private final Map<UUID, List<Entity>> activeMines = new ConcurrentHashMap<>();

    // Tracking des bleeds actifs pour éviter le stacking
    private final Set<UUID> activeBleedTargets = ConcurrentHashMap.newKeySet();

    // Entités marquées par le renard (UUID entité -> timestamp expiration)
    private final Map<UUID, Long> foxMarkedEntities = new ConcurrentHashMap<>();
    private static final double FOX_MARK_DAMAGE_BONUS = 0.30; // +30% dégâts

    // Stacks de piqûres d'abeille (UUID entité -> nombre de stacks)
    private final Map<UUID, Integer> beeStingStacks = new ConcurrentHashMap<>();
    private static final int BEE_MAX_STACKS = 5;
    private static final double BEE_VENOM_EXPLOSION_DAMAGE = 1.5; // 150% des dégâts de base

    // Endermite - Système d'infestation et corruption du Vide
    private static final long ENDERMITE_INFESTATION_DURATION = 3000; // 3 secondes
    private static final double ENDERMITE_CORRUPTION_BONUS = 0.25;   // +25% dégâts reçus
    private static final double ENDERMITE_EXPLOSION_DAMAGE = 0.50;   // 50% dégâts du joueur
    private static final double ENDERMITE_DOT_DAMAGE = 0.15;         // 15% dégâts/tick
    private static final double ENDERMITE_AOE_RADIUS = 4.0;          // Rayon explosion
    private final Map<UUID, Long> voidCorruptedEntities = new ConcurrentHashMap<>(); // entityUUID -> expiry timestamp
    private final Map<UUID, UUID> endermiteCurrentTarget = new ConcurrentHashMap<>(); // ownerUUID -> currentTargetUUID
    private final Map<UUID, Long> endermiteInfestationStart = new ConcurrentHashMap<>(); // ownerUUID -> start timestamp

    // Axolotl - Système de vitesse d'attaque progressive
    private final Map<UUID, Double> axolotlAttackSpeedBonus = new ConcurrentHashMap<>(); // Bonus en % (0.0 à 1.5)
    private final Map<UUID, Long> axolotlLastAttackTime = new ConcurrentHashMap<>();
    private static final double AXOLOTL_SPEED_INCREMENT = 0.10;    // +10% par attaque
    private static final double AXOLOTL_SPEED_MAX_BONUS = 1.50;    // +150% max
    private static final long AXOLOTL_SPEED_DECAY_DELAY = 10000;   // 10 secondes avant décroissance
    private static final double AXOLOTL_SPEED_DECAY_RATE = 0.10;   // -10% par seconde
    private static final long AXOLOTL_TICK_RATE = 500;              // Tick rate des bêtes (10 ticks = 500ms)
    private final Map<UUID, Long> axolotlLastShotTime = new ConcurrentHashMap<>(); // Dernier tir effectif

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
     * Applique les dégâts d'une attaque native de bête avec le système complet.
     * Utilisé par le listener quand une bête attaque via son IA.
     * Inclut critiques, lifesteal, et dégâts élémentaires.
     */
    public void applyNativeBeastDamage(Player owner, LivingEntity target, BeastType type) {
        // Utiliser le système de dégâts complet (comme le Loup)
        applyBeastDamageWithFullStats(owner, target, type, 1.0);
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

        // Tâche de décroissance de la vitesse d'attaque de l'Axolotl (20 ticks = 1 seconde)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAxolotlSpeedDecay();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Met à jour la décroissance de la vitesse d'attaque de l'Axolotl.
     * Si le joueur n'a pas attaqué depuis 10 secondes, réduit le bonus de 10% par seconde.
     */
    private void updateAxolotlSpeedDecay() {
        long now = System.currentTimeMillis();

        // Itérer sur tous les joueurs avec un bonus d'axolotl
        axolotlAttackSpeedBonus.entrySet().removeIf(entry -> {
            UUID playerUuid = entry.getKey();
            double currentBonus = entry.getValue();

            // Si le bonus est déjà à 0, nettoyer
            if (currentBonus <= 0) return true;

            // Vérifier si le joueur a un axolotl actif
            Map<BeastType, UUID> beasts = playerBeasts.get(playerUuid);
            if (beasts == null || !beasts.containsKey(BeastType.AXOLOTL)) {
                axolotlLastShotTime.remove(playerUuid); // Nettoyer le timestamp de tir aussi
                return true; // Nettoyer si pas d'axolotl
            }

            // Vérifier le temps depuis la dernière attaque
            Long lastAttack = axolotlLastAttackTime.get(playerUuid);
            if (lastAttack == null || now - lastAttack >= AXOLOTL_SPEED_DECAY_DELAY) {
                // Décroître le bonus
                double newBonus = currentBonus - AXOLOTL_SPEED_DECAY_RATE;

                if (newBonus <= 0) {
                    axolotlLastAttackTime.remove(playerUuid);
                    axolotlLastShotTime.remove(playerUuid); // Nettoyer aussi
                    return true; // Supprimer l'entrée
                } else {
                    entry.setValue(newBonus);

                    // Feedback visuel si le bonus diminue significativement
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && newBonus < currentBonus) {
                        // Son subtil de décroissance (seulement toutes les 0.5 de perte)
                        if ((int)(currentBonus * 10) != (int)(newBonus * 10) && (int)(newBonus * 10) % 5 == 0) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f);
                        }
                    }
                }
            }
            return false;
        });
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
     * Synchronise les bêtes d'un joueur avec ses talents actifs.
     * Despawn les bêtes dont le talent n'est plus actif.
     * Invoque les bêtes manquantes dont le talent est actif.
     */
    public void syncBeastsWithTalents(Player player) {
        UUID uuid = player.getUniqueId();
        ClassData data = plugin.getClassManager().getClassData(player);

        // Si le joueur n'est pas Chasseur ou n'a pas la branche Bêtes, despawn toutes les bêtes
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) {
            despawnAllBeasts(player);
            return;
        }

        String branchId = data.getSelectedBranchId();
        if (branchId == null || !branchId.contains("betes")) {
            despawnAllBeasts(player);
            return;
        }

        // Vérifier chaque bête existante
        Map<BeastType, UUID> beasts = playerBeasts.get(uuid);
        if (beasts != null) {
            // Copie pour éviter ConcurrentModificationException
            Map<BeastType, UUID> beastsCopy = new HashMap<>(beasts);
            for (Map.Entry<BeastType, UUID> entry : beastsCopy.entrySet()) {
                BeastType type = entry.getKey();
                UUID beastUuid = entry.getValue();

                // Vérifier si le talent correspondant est toujours actif
                boolean talentActive = false;
                for (TalentTier tier : TalentTier.values()) {
                    Talent talent = talentManager.getActiveTalentForTier(player, tier);
                    if (talent != null && getBeastTypeFromTalent(talent) == type) {
                        talentActive = true;
                        break;
                    }
                }

                // Si le talent n'est plus actif, despawn la bête
                if (!talentActive) {
                    Entity entity = Bukkit.getEntity(beastUuid);
                    if (entity != null) {
                        entity.remove();
                    }
                    beasts.remove(type);
                    // Retirer aussi du pending respawn
                    Map<BeastType, Long> respawns = pendingRespawn.get(uuid);
                    if (respawns != null) {
                        respawns.remove(type);
                    }
                }
            }
        }

        // Invoquer les bêtes manquantes
        summonBeastsForPlayer(player);
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
                // Chauve-souris avec ultrasons - pas de pathfinding nécessaire
                if (beast instanceof Bat bat) {
                    bat.setAwake(true);
                    bat.setSilent(true); // Sons custom pour l'ultrason
                }
            }
            case ENDERMITE -> {
                // L'Endermite est un petit parasite du Vide - invincible et discret
                if (beast instanceof org.bukkit.entity.Endermite endermite) {
                    // Empêcher le despawn naturel
                    endermite.setPersistent(true);
                }
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

        // Comportement spécial pour les bêtes volantes
        if (type == BeastType.BAT || type == BeastType.BEE) {
            // Flotter au-dessus du joueur (entre 1.5 et 2.5 blocs)
            targetLoc.setY(playerLoc.getY() + 1.5 + Math.sin(System.currentTimeMillis() / 500.0) * 0.5);
            return targetLoc;
        }

        // Trouver le sol pour les autres bêtes
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

    // Constantes pour le comportement des bêtes
    private static final double BEAST_COMBAT_RANGE = 20.0;      // Rayon max de combat autour du joueur
    private static final double BEAST_LEASH_RANGE = 40.0;       // Distance max avant téléportation (augmenté de 25)
    private static final double BEAST_FOLLOW_RANGE = 8.0;       // Distance pour commencer à suivre (pas de combat)

    /**
     * Met à jour la formation de la meute.
     * Système de priorité:
     * 1. Cible focus du joueur (celle qu'il attaque)
     * 2. Ennemi le plus proche du joueur (dans 20 blocs)
     * 3. Suivre le joueur si aucun ennemi
     */
    private void updatePackFormation() {
        for (Map.Entry<UUID, Map<BeastType, UUID>> entry : playerBeasts.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            // PRIORITÉ 1: Récupérer la cible focus du joueur (celle qu'il attaque)
            LivingEntity focusTarget = getPlayerFocusTarget(player);

            // PRIORITÉ 2: Si pas de focus, chercher l'ennemi le plus proche du joueur
            if (focusTarget == null) {
                focusTarget = findNearestEnemy(player, BEAST_COMBAT_RANGE);
            }

            for (Map.Entry<BeastType, UUID> beastEntry : entry.getValue().entrySet()) {
                Entity entity = Bukkit.getEntity(beastEntry.getValue());
                if (entity == null || entity.isDead() || !(entity instanceof LivingEntity beast)) {
                    continue;
                }

                BeastType type = beastEntry.getKey();
                Location beastLoc = beast.getLocation();
                Location playerLoc = player.getLocation();
                double distanceToPlayer = beastLoc.distance(playerLoc);

                // Si trop loin du joueur (>25 blocs), téléporter près du joueur
                if (distanceToPlayer > BEAST_LEASH_RANGE) {
                    Location safeLoc = calculatePackPosition(player, type);
                    beast.teleport(safeLoc);
                    continue;
                }

                // Comportement spécifique selon le type de bête
                if (type == BeastType.BAT || type == BeastType.BEE) {
                    // Bêtes volantes: comportement spécial
                    updateFlyingBeastCombat(player, beast, focusTarget, type);
                } else if (type == BeastType.LLAMA || type == BeastType.AXOLOTL || type == BeastType.COW || type == BeastType.ENDERMITE) {
                    // Bêtes à distance: restent près du joueur et attaquent de loin
                    updateRangedBeastBehavior(player, beast, focusTarget, type);
                } else {
                    // Bêtes terrestres de mêlée: mode combat ou suivi
                    updateGroundBeastCombat(player, beast, focusTarget, type);
                }
            }
        }
    }

    /**
     * Récupère la cible focus du joueur (celle qu'il attaque)
     */
    private LivingEntity getPlayerFocusTarget(Player player) {
        UUID focusUuid = playerFocusTarget.get(player.getUniqueId());
        if (focusUuid == null) return null;

        Entity focusEntity = Bukkit.getEntity(focusUuid);
        if (focusEntity instanceof LivingEntity living && !living.isDead()) {
            // Vérifier que la cible est dans le rayon de combat
            if (living.getLocation().distanceSquared(player.getLocation()) <= BEAST_COMBAT_RANGE * BEAST_COMBAT_RANGE) {
                return living;
            }
        }

        // Focus invalide, nettoyer
        playerFocusTarget.remove(player.getUniqueId());
        return null;
    }

    /**
     * Met à jour une bête volante en mode combat
     */
    private void updateFlyingBeastCombat(Player player, LivingEntity beast, LivingEntity combatTarget, BeastType type) {
        Location beastLoc = beast.getLocation();
        Location playerLoc = player.getLocation();

        // Déterminer la destination
        Location targetLoc;

        if (combatTarget != null) {
            // MODE COMBAT: Voler près de la cible pour attaquer
            Location combatLoc = combatTarget.getLocation().add(0, combatTarget.getHeight() + 1.5, 0);
            double distToCombat = beastLoc.distance(combatLoc);

            // Rester à portée d'attaque (2-4 blocs au-dessus de la cible)
            if (distToCombat > 5) {
                targetLoc = combatLoc;
            } else {
                // Orbiter légèrement autour de la cible
                double angle = System.currentTimeMillis() / 1000.0 * 2;
                double orbitRadius = 2.0;
                targetLoc = combatLoc.clone().add(
                    Math.cos(angle) * orbitRadius,
                    Math.sin(System.currentTimeMillis() / 500.0) * 0.5,
                    Math.sin(angle) * orbitRadius
                );
            }
        } else {
            // MODE SUIVI: Suivre le joueur
            targetLoc = calculatePackPosition(player, type);
        }

        // Mouvement fluide
        double distanceToTarget = beastLoc.distance(targetLoc);
        if (distanceToTarget > 0.5) {
            Vector direction = targetLoc.toVector().subtract(beastLoc.toVector());
            double speed = Math.min(distanceToTarget / 4.0, 0.8);
            direction.normalize().multiply(speed);
            direction.setY(Math.max(-0.4, Math.min(0.4, direction.getY())));
            beast.setVelocity(direction);
        }

        // Orienter vers la cible de combat ou le joueur
        if (combatTarget != null) {
            orientBeastTowards(beast, combatTarget.getLocation().add(0, combatTarget.getHeight() / 2, 0));
        }
    }

    /**
     * Met à jour une bête terrestre en mode combat
     */
    private void updateGroundBeastCombat(Player player, LivingEntity beast, LivingEntity combatTarget, BeastType type) {
        if (!(beast instanceof Mob mob)) return;

        Location beastLoc = beast.getLocation();
        Location playerLoc = player.getLocation();
        double distanceToPlayer = beastLoc.distance(playerLoc);

        // MODE COMBAT: Une cible existe
        if (combatTarget != null) {
            double distToCombatTarget = beastLoc.distance(combatTarget.getLocation());

            // Définir la cible de la bête
            if (mob.getTarget() != combatTarget) {
                mob.setTarget(combatTarget);
            }

            // Si la bête est loin de la cible, la faire se déplacer vers elle
            if (distToCombatTarget > 2) {
                double speed = 1.2 + Math.min(distToCombatTarget / 10.0, 0.8);
                mob.getPathfinder().moveTo(combatTarget.getLocation(), speed);
            }

            // Orienter vers la cible
            orientBeastTowards(beast, combatTarget.getLocation());
            return;
        }

        // MODE SUIVI: Pas de cible, suivre le joueur
        Location targetLoc = calculatePackPosition(player, type);
        double distanceToTarget = beastLoc.distance(targetLoc);

        // Téléporter si bloqué et trop loin
        if (distanceToTarget > 15) {
            if (!mob.getPathfinder().hasPath()) {
                beast.teleport(targetLoc);
                return;
            }
        }

        // Se déplacer vers la position de formation
        if (distanceToTarget > 3) {
            double speed = 1.0 + Math.min(distanceToTarget / 8.0, 1.0);
            mob.getPathfinder().moveTo(targetLoc, speed);
        }

        // Nettoyer la cible quand on suit le joueur
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
    }

    /**
     * Met à jour une bête à distance (Llama, Axolotl) - reste près du joueur et attaque de loin
     */
    private void updateRangedBeastBehavior(Player player, LivingEntity beast, LivingEntity combatTarget, BeastType type) {
        if (!(beast instanceof Mob mob)) return;

        Location beastLoc = beast.getLocation();
        Location playerLoc = player.getLocation();
        double distanceToPlayer = beastLoc.distance(playerLoc);

        // Distance idéale: rester proche du joueur (2-4 blocs)
        double idealDistance = type.getDistanceFromPlayer();
        Location targetLoc = calculatePackPosition(player, type);
        double distanceToTarget = beastLoc.distance(targetLoc);

        // Si en combat, orienter vers la cible mais rester près du joueur
        if (combatTarget != null) {
            // Orienter vers la cible pour attaquer
            orientBeastTowards(beast, combatTarget.getLocation());

            // Si trop loin du joueur, revenir vers lui
            if (distanceToPlayer > idealDistance + 3) {
                double speed = 1.0 + Math.min(distanceToPlayer / 8.0, 1.0);
                mob.getPathfinder().moveTo(targetLoc, speed);
            } else if (distanceToTarget > 2) {
                // Ajuster légèrement la position pour rester en formation
                mob.getPathfinder().moveTo(targetLoc, 0.8);
            }
            return;
        }

        // MODE SUIVI: Pas de cible, suivre le joueur
        if (distanceToTarget > 3) {
            double speed = 1.0 + Math.min(distanceToTarget / 8.0, 1.0);
            mob.getPathfinder().moveTo(targetLoc, speed);
        }

        // Nettoyer la cible quand on suit le joueur
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
    }

    /**
     * Oriente une bête vers une position (fait regarder)
     */
    private void orientBeastTowards(LivingEntity beast, Location target) {
        Location beastLoc = beast.getLocation();
        Vector direction = target.toVector().subtract(beastLoc.toVector());

        if (direction.lengthSquared() < 0.01) return;

        // Calculer le yaw et pitch
        direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.getY()));

        // Appliquer la rotation de manière fluide
        beastLoc.setYaw(yaw);
        beastLoc.setPitch(pitch);

        // Téléportation légère pour appliquer la rotation (sans déplacer)
        beast.teleport(beastLoc);
    }

    /**
     * Trouve l'ennemi le plus proche du joueur
     */
    private LivingEntity findNearestEnemy(Player player, double range) {
        LivingEntity nearest = null;
        double nearestDistSq = range * range;

        for (Entity nearby : player.getNearbyEntities(range, range / 2, range)) {
            if (nearby instanceof Monster monster && !isBeast(nearby)) {
                double distSq = player.getLocation().distanceSquared(nearby.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = monster;
                }
            }
        }
        return nearest;
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
        String cooldownKey = type.name() + "_ability";

        switch (type) {
            case BAT -> executeBatAbility(owner, beast, 1.0);
            case ENDERMITE -> executeEndermiteAbility(owner, beast, now, 1.0);
            case WOLF -> executeWolfAbility(owner, beast, 1.0);
            case AXOLOTL -> executeAxolotlAbility(owner, beast, now, cooldownKey, 1.0);
            case COW -> executeCowAbility(owner, beast, now, cooldownKey);
            case LLAMA -> executeLlamaAbility(owner, beast, now, cooldownKey, 1.0);
            case FOX -> executeFoxAbility(owner, beast, now, cooldownKey, 1.0);
            case BEE -> executeBeeAbility(owner, beast, now, cooldownKey, 1.0);
            case IRON_GOLEM -> executeIronGolemAbility(owner, beast, now, cooldownKey, 1.0);
        }
    }

    // === CAPACITÉS SPÉCIFIQUES ===

    private void executeBatAbility(Player owner, LivingEntity bat, double frenzyMultiplier) {
        // L'ultrason est géré par un cooldown séparé dans updateAllBeasts
        // Cette méthode est appelée chaque seconde, on utilise un cooldown pour la cadence
        long now = System.currentTimeMillis();
        String cooldownKey = "bat_ultrasound";
        long shootCooldown = (long) (1500 / frenzyMultiplier); // 1.5s base

        if (isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            return;
        }

        // PRIORITÉ 1: Cible focus du joueur (celle qu'il attaque)
        LivingEntity target = null;
        UUID focusUuid = playerFocusTarget.get(owner.getUniqueId());
        if (focusUuid != null) {
            Entity focusEntity = Bukkit.getEntity(focusUuid);
            if (focusEntity instanceof LivingEntity living && !living.isDead() &&
                living.getLocation().distanceSquared(bat.getLocation()) < 1024) { // 32 blocs
                target = living;
            } else {
                // Focus invalide, nettoyer
                playerFocusTarget.remove(owner.getUniqueId());
            }
        }

        // PRIORITÉ 2: Ennemi le plus proche du joueur (pas de la chauve-souris)
        if (target == null) {
            double nearestDistSq = 1024.0; // 32^2

            for (Entity nearby : owner.getNearbyEntities(32, 16, 32)) {
                if (nearby instanceof Monster monster && !isBeast(nearby) && !monster.isDead()) {
                    double distSq = owner.getLocation().distanceSquared(nearby.getLocation());
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        target = monster;
                    }
                }
            }
        }

        // PRIORITÉ 3: Ennemi le plus proche de la chauve-souris
        if (target == null) {
            double nearestDistSq = 1024.0; // 32^2

            for (Entity nearby : bat.getNearbyEntities(32, 16, 32)) {
                if (nearby instanceof Monster monster && !isBeast(nearby) && !monster.isDead()) {
                    double distSq = bat.getLocation().distanceSquared(nearby.getLocation());
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        target = monster;
                    }
                }
            }
        }

        if (target != null) {
            shootUltrasound(owner, bat, target, frenzyMultiplier);
            setCooldown(owner.getUniqueId(), cooldownKey, now + shootCooldown);
        }
    }

    /**
     * Tire un ultrason en ligne droite qui transperce tous les ennemis.
     * Particules optimisées pour réduire le lag.
     */
    private void shootUltrasound(Player owner, LivingEntity bat, LivingEntity target, double frenzyMultiplier) {
        Location start = bat.getLocation().add(0, 0.3, 0);
        Location targetLoc = target.getLocation().add(0, target.getHeight() * 0.5, 0);
        Vector direction = targetLoc.subtract(start).toVector().normalize();
        double damage = calculateBeastDamage(owner, BeastType.BAT, frenzyMultiplier);

        // Son d'ultrason
        bat.getWorld().playSound(start, Sound.ENTITY_BAT_TAKEOFF, 1.5f, 2.0f);
        bat.getWorld().playSound(start, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 2.0f);

        // Set pour tracker les entités déjà touchées
        Set<UUID> hitEntities = new HashSet<>();

        // Tracer le rayon sur 32 blocs (portée complète)
        new BukkitRunnable() {
            Location current = start.clone();
            int steps = 0;
            final int maxSteps = 32; // 32 blocs (1.0 par step pour plus de vitesse)

            @Override
            public void run() {
                if (steps >= maxSteps) {
                    cancel();
                    return;
                }

                // Avancer de 1 bloc par tick (projectile rapide)
                current.add(direction.clone().multiply(1.0));

                // Particules réduites: SONIC_BOOM toutes les 4 blocs
                // + petites particules de note entre-temps pour le traçage visuel
                if (steps % 4 == 0) {
                    current.getWorld().spawnParticle(Particle.SONIC_BOOM, current, 1, 0, 0, 0, 0);
                } else {
                    // Particules légères entre les SONIC_BOOM
                    current.getWorld().spawnParticle(Particle.NOTE, current, 1, 0.1, 0.1, 0.1, 0);
                }

                // Vérifier les collisions avec les ennemis
                for (Entity entity : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                    if (entity instanceof LivingEntity living &&
                        entity instanceof Monster &&
                        !isBeast(entity) &&
                        entity != owner &&
                        !hitEntities.contains(entity.getUniqueId())) {

                        // Touché! Appliquer les dégâts avec metadata
                        hitEntities.add(entity.getUniqueId());
                        applyBeastDamageWithMetadata(owner, living, damage);

                        // Effet d'impact sonore
                        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_BAT_HURT, 1.0f, 1.5f);
                    }
                }

                // Arrêter si on touche un bloc solide
                if (current.getBlock().getType().isSolid()) {
                    cancel();
                    return;
                }

                steps++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // === ENDERMITE - PARASITE DU VIDE ===

    /**
     * Capacité de l'Endermite: cycle d'infestation du Vide
     * 1. Se téléporte sur un ennemi
     * 2. S'accroche et inflige des DoT pendant 3s
     * 3. Applique "Corruption du Vide" (+25% dégâts reçus)
     * 4. Explose et se téléporte vers une nouvelle cible
     */
    private void executeEndermiteAbility(Player owner, LivingEntity endermite, long now, double frenzyMultiplier) {
        UUID ownerUuid = owner.getUniqueId();

        // Vérifier si l'Endermite est en train d'infester une cible
        UUID currentTargetUuid = endermiteCurrentTarget.get(ownerUuid);
        Long infestationStart = endermiteInfestationStart.get(ownerUuid);

        if (currentTargetUuid != null && infestationStart != null) {
            // Récupérer la cible actuelle
            Entity targetEntity = Bukkit.getEntity(currentTargetUuid);

            if (targetEntity instanceof LivingEntity target && !target.isDead()) {
                // Vérifier si la cible est trop loin du joueur - si oui, annuler l'infestation
                double distanceTargetToPlayer = target.getLocation().distance(owner.getLocation());
                if (distanceTargetToPlayer > BEAST_LEASH_RANGE) {
                    // Annuler l'infestation et retourner près du joueur
                    endermiteCurrentTarget.remove(ownerUuid);
                    endermiteInfestationStart.remove(ownerUuid);

                    // Téléporter l'endermite près du joueur
                    Location returnLoc = owner.getLocation().clone().add(
                        Math.cos(Math.toRadians(BeastType.ENDERMITE.getOffsetAngle())) * 2,
                        0.5,
                        Math.sin(Math.toRadians(BeastType.ENDERMITE.getOffsetAngle())) * 2
                    );
                    endermite.teleport(returnLoc);
                    endermite.getWorld().playSound(returnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
                    return;
                }

                long elapsed = now - infestationStart;
                long duration = (long) (ENDERMITE_INFESTATION_DURATION / frenzyMultiplier);

                // Téléporter l'endermite SUR la cible (effet parasite)
                Location targetLoc = target.getLocation().add(0, target.getHeight(), 0);
                endermite.teleport(targetLoc);

                // Appliquer la Corruption du Vide
                applyVoidCorruption(target);

                // Appliquer DoT chaque seconde (tick rate ~500ms, donc 2 ticks = 1s)
                if (elapsed % 1000 < 600) { // Approximativement toutes les secondes
                    double dotDamage = calculateBeastDamage(owner, BeastType.ENDERMITE) * frenzyMultiplier;
                    applyBeastDamageWithMetadata(owner, target, dotDamage);

                    // Particules de corruption (réduites)
                    target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1, 0),
                        4, 0.3, 0.5, 0.3, 0.02);
                }

                // Fin d'infestation -> EXPLOSION DU VIDE
                if (elapsed >= duration) {
                    executeVoidExplosion(owner, endermite, target, frenzyMultiplier);
                    endermiteCurrentTarget.remove(ownerUuid);
                    endermiteInfestationStart.remove(ownerUuid);
                }
                return;
            } else {
                // Cible morte ou invalide - chercher nouvelle cible immédiatement
                endermiteCurrentTarget.remove(ownerUuid);
                endermiteInfestationStart.remove(ownerUuid);
            }
        }

        // Pas de cible actuelle - chercher une nouvelle cible et téléporter
        LivingEntity newTarget = findEndermiteTarget(owner, endermite);
        if (newTarget != null) {
            teleportToTarget(owner, endermite, newTarget);
            endermiteCurrentTarget.put(ownerUuid, newTarget.getUniqueId());
            endermiteInfestationStart.put(ownerUuid, now);
        }
    }

    /**
     * Trouve la meilleure cible pour l'Endermite
     * Priorité: cibles marquées (renard) > cibles saignantes (loup) > cibles avec stacks abeille > plus proche
     */
    private LivingEntity findEndermiteTarget(Player owner, LivingEntity endermite) {
        LivingEntity bestTarget = null;
        double bestScore = 0;

        // Portée limitée au BEAST_LEASH_RANGE pour éviter de cibler trop loin
        for (Entity nearby : owner.getNearbyEntities(BEAST_LEASH_RANGE, 16, BEAST_LEASH_RANGE)) {
            if (!(nearby instanceof Monster monster) || isBeast(nearby) || monster.isDead()) continue;

            // Vérifier la distance exacte (getNearbyEntities utilise une boîte, pas un rayon)
            double distToOwner = nearby.getLocation().distance(owner.getLocation());
            if (distToOwner > BEAST_LEASH_RANGE) continue;

            double score = 1.0;

            // Bonus si marqué par le renard (+50 - haute priorité)
            if (isMarkedByFox(nearby.getUniqueId())) {
                score += 50;
            }

            // Bonus si saignant (loup) (+30)
            if (activeBleedTargets.contains(nearby.getUniqueId())) {
                score += 30;
            }

            // Bonus selon les stacks d'abeille (+10 par stack)
            int beeStacks = beeStingStacks.getOrDefault(nearby.getUniqueId(), 0);
            score += beeStacks * 10;

            // Bonus proximité du joueur (préférer les menaces proches)
            double dist = owner.getLocation().distanceSquared(nearby.getLocation());
            score += Math.max(0, (1024 - dist) / 50); // Bonus jusqu'à 32 blocs

            if (score > bestScore) {
                bestScore = score;
                bestTarget = monster;
            }
        }

        return bestTarget;
    }

    /**
     * Téléporte l'Endermite vers sa cible avec effets satisfaisants
     */
    private void teleportToTarget(Player owner, LivingEntity endermite, LivingEntity target) {
        Location from = endermite.getLocation();
        Location to = target.getLocation().add(0, target.getHeight(), 0);

        // SON DE TÉLÉPORTATION (très satisfaisant!)
        endermite.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.8f);
        endermite.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 2.0f);

        // Particules de départ (réduites)
        endermite.getWorld().spawnParticle(Particle.REVERSE_PORTAL, from.add(0, 0.5, 0),
            10, 0.3, 0.3, 0.3, 0.1);

        // Téléportation
        endermite.teleport(to);

        // Particules d'arrivée (réduites)
        endermite.getWorld().spawnParticle(Particle.REVERSE_PORTAL, to,
            15, 0.5, 0.5, 0.5, 0.15);
        endermite.getWorld().spawnParticle(Particle.PORTAL, to,
            10, 0.3, 0.3, 0.3, 2.0);

        // Son d'accrochage
        endermite.getWorld().playSound(to, Sound.ENTITY_ENDERMITE_AMBIENT, 2.0f, 1.5f);
    }

    /**
     * Explosion du Vide - fin de cycle d'infestation
     */
    private void executeVoidExplosion(Player owner, LivingEntity endermite, LivingEntity target, double frenzyMultiplier) {
        Location explosionLoc = target.getLocation().add(0, 1, 0);

        // SONS D'EXPLOSION (satisfaisant!)
        endermite.getWorld().playSound(explosionLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
        endermite.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        endermite.getWorld().playSound(explosionLoc, Sound.ENTITY_ENDERMITE_DEATH, 1.5f, 0.8f);
        endermite.getWorld().playSound(explosionLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.2f);

        // EXPLOSION DE PARTICULES (réduites pour performance)
        endermite.getWorld().spawnParticle(Particle.REVERSE_PORTAL, explosionLoc,
            25, 1.5, 1.5, 1.5, 0.3);
        endermite.getWorld().spawnParticle(Particle.PORTAL, explosionLoc,
            20, 2.0, 2.0, 2.0, 3.0);
        endermite.getWorld().spawnParticle(Particle.DRAGON_BREATH, explosionLoc,
            8, 1.0, 0.5, 1.0, 0.05);
        endermite.getWorld().spawnParticle(Particle.FLASH, explosionLoc, 1);

        // Dégâts AoE
        double explosionDamage = calculateBeastDamage(owner, BeastType.ENDERMITE) * (ENDERMITE_EXPLOSION_DAMAGE / ENDERMITE_DOT_DAMAGE) * frenzyMultiplier;

        for (Entity nearby : explosionLoc.getWorld().getNearbyEntities(explosionLoc, ENDERMITE_AOE_RADIUS, ENDERMITE_AOE_RADIUS, ENDERMITE_AOE_RADIUS)) {
            if (nearby instanceof LivingEntity living && nearby instanceof Monster && !isBeast(nearby)) {
                // Dégâts réduits selon la distance
                double distance = nearby.getLocation().distance(explosionLoc);
                double damageMult = Math.max(0.5, 1.0 - (distance / ENDERMITE_AOE_RADIUS));

                applyBeastDamageWithMetadata(owner, living, explosionDamage * damageMult);

                // Appliquer la corruption aux cibles touchées par l'explosion
                applyVoidCorruption(living);

                // SYNERGIE: Si la cible a 3+ stacks d'abeille, déclencher l'explosion de venin!
                int beeStacks = beeStingStacks.getOrDefault(nearby.getUniqueId(), 0);
                if (beeStacks >= 3) {
                    triggerBeeVenomExplosion(owner, living);
                }
            }
        }

        // Retour de l'endermite près du joueur avant de chercher une nouvelle cible
        Location ownerLoc = owner.getLocation();
        Location returnLoc = ownerLoc.clone().add(
            Math.cos(Math.toRadians(BeastType.ENDERMITE.getOffsetAngle())) * 2,
            0.5,
            Math.sin(Math.toRadians(BeastType.ENDERMITE.getOffsetAngle())) * 2
        );
        endermite.teleport(returnLoc);
    }

    /**
     * Applique la Corruption du Vide à une cible
     * +25% de dégâts reçus pendant 5 secondes
     */
    private void applyVoidCorruption(LivingEntity target) {
        voidCorruptedEntities.put(target.getUniqueId(), System.currentTimeMillis() + 5000);

        // Effet visuel de corruption
        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1, 0),
            10, 0.3, 0.5, 0.3, 0.02);
    }

    /**
     * Vérifie si une entité est corrompue par le Vide
     */
    public boolean isVoidCorrupted(UUID entityUuid) {
        Long expiry = voidCorruptedEntities.get(entityUuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            voidCorruptedEntities.remove(entityUuid);
            return false;
        }
        return true;
    }

    /**
     * Obtient le bonus de dégâts de la Corruption du Vide
     */
    public double getVoidCorruptionBonus(UUID entityUuid) {
        return isVoidCorrupted(entityUuid) ? ENDERMITE_CORRUPTION_BONUS : 0.0;
    }

    /**
     * Déclenche l'explosion de venin d'abeille (synergie avec Endermite)
     */
    private void triggerBeeVenomExplosion(Player owner, LivingEntity target) {
        // Reset les stacks
        beeStingStacks.remove(target.getUniqueId());

        // Dégâts d'explosion
        double explosionDamage = calculateBeastDamage(owner, BeastType.BEE) * BEE_VENOM_EXPLOSION_DAMAGE * 10;
        applyBeastDamageWithMetadata(owner, target, explosionDamage);

        // Effets visuels
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.DUST, loc, 30, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.5f));
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc, 5, 0.3, 0.3, 0.3, 0);

        // Sons
        target.getWorld().playSound(loc, Sound.ENTITY_BEE_DEATH, 1.5f, 0.5f);
        target.getWorld().playSound(loc, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.0f, 0.8f);

        // Poison II pendant 5 secondes
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, false, true, true));
    }

    private void executeWolfAbility(Player owner, LivingEntity wolf, double frenzyMultiplier) {
        if (!(wolf instanceof Wolf w)) return;

        long now = System.currentTimeMillis();
        String cooldownKey = "wolf_bite";
        long biteCooldown = (long) (1000 / frenzyMultiplier); // 1s entre chaque morsure

        // Chercher la cible la plus proche (priorité aux cibles à portée de mêlée)
        LivingEntity nearestEnemy = null;
        double nearestDistSq = 64.0; // 8^2

        for (Entity nearby : wolf.getNearbyEntities(8, 4, 8)) {
            if (nearby instanceof Monster monster && !isBeast(nearby)) {
                double distSq = wolf.getLocation().distanceSquared(nearby.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestEnemy = monster;
                    if (distSq < 4) break; // <2 blocs, on prend direct (portée mêlée)
                }
            }
        }

        if (nearestEnemy == null) return;

        // Définir la cible pour le pathfinding
        w.setTarget(nearestEnemy);

        // Si à portée de mêlée (< 2.5 blocs) et pas en cooldown, infliger les dégâts
        double distance = Math.sqrt(nearestDistSq);
        if (distance <= 2.5 && !isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Infliger les dégâts manuellement (comme les serviteurs Occultiste)
            applyWolfBite(owner, nearestEnemy, frenzyMultiplier);
            setCooldown(owner.getUniqueId(), cooldownKey, now + biteCooldown);
        }
    }

    /**
     * Applique une morsure du loup avec dégâts et saignement.
     * Utilise le système de dégâts complet (comme serviteurs Occultiste).
     */
    private void applyWolfBite(Player owner, LivingEntity target, double frenzyMultiplier) {
        // Appliquer les dégâts avec le calcul complet des stats du joueur
        applyBeastDamageWithFullStats(owner, target, BeastType.WOLF, frenzyMultiplier);

        // Appliquer le saignement
        applyWolfBleed(owner, target);

        // Effet sonore de morsure
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.8f, 1.2f);
    }

    /**
     * Applique des dégâts d'une bête avec les metadata nécessaires pour l'indicateur et le loot.
     * Méthode helper réutilisable par toutes les bêtes.
     *
     * @param owner  Le joueur propriétaire de la bête
     * @param target La cible des dégâts
     * @param damage Les dégâts à infliger
     */
    private void applyBeastDamageWithMetadata(Player owner, LivingEntity target, double damage) {
        // Configurer les metadata pour l'indicateur de dégâts (comme les serviteurs Occultiste)
        target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
        target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, false));
        target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

        // Attribution du loot au propriétaire
        if (plugin.getZombieManager().isZombieZMob(target)) {
            target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));
        }

        // Marquer comme dégâts de bête (pour bypass la restriction arc/arbalète dans CombatListener)
        target.setMetadata("zombiez_beast_damage", new FixedMetadataValue(plugin, true));

        // Appliquer les dégâts
        target.damage(damage, owner);

        // Retirer le marqueur
        target.removeMetadata("zombiez_beast_damage", plugin);

        // Mettre à jour l'affichage de vie du zombie
        if (plugin.getZombieManager().isZombieZMob(target)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(target);
                }
            });
        }
    }

    // ==================== SYSTÈME DE DÉGÂTS COMPLET (COMME SERVITEURS OCCULTISTE) ====================

    /**
     * Résultat du calcul de dégâts d'une bête.
     * Inclut les dégâts finaux, critique, lifesteal et dégâts élémentaires.
     */
    private static class BeastDamageResult {
        final double damage;
        final boolean isCritical;
        final double lifestealAmount;
        final double fireDamage;
        final double iceDamage;
        final double lightningDamage;
        final boolean lightningProc;

        BeastDamageResult(double damage, boolean isCritical, double lifestealAmount,
                double fireDamage, double iceDamage, double lightningDamage, boolean lightningProc) {
            this.damage = damage;
            this.isCritical = isCritical;
            this.lifestealAmount = lifestealAmount;
            this.fireDamage = fireDamage;
            this.iceDamage = iceDamage;
            this.lightningDamage = lightningDamage;
            this.lightningProc = lightningProc;
        }
    }

    /**
     * Calcule les dégâts complets d'une bête avec TOUTES les stats du joueur.
     * Réplique exactement la logique des serviteurs Occultiste.
     *
     * @param player Le propriétaire de la bête
     * @param target La cible de l'attaque
     * @param beastType Le type de bête (pour le multiplicateur de base)
     * @param damageMultiplier Multiplicateur additionnel (ex: synergies, frénésie)
     * @return Les dégâts calculés avec infos critique/lifesteal
     */
    private BeastDamageResult calculateBeastDamageWithFullStats(Player player, LivingEntity target,
            BeastType beastType, double damageMultiplier) {

        // ============ 1. DÉGÂTS DE BASE DU JOUEUR ============
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        // Appliquer le multiplicateur de la bête (ex: WOLF = 30%, FOX = 20%)
        baseDamage *= beastType.getDamagePercent();
        double finalDamage = baseDamage;
        boolean isCritical = false;

        // ============ 2. STATS D'ÉQUIPEMENT ZOMBIEZ ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus de dégâts flat (ex: +10 dégâts)
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage (ex: +15% dégâts)
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // ============ 3. SKILL TREE BONUSES ============
        var skillManager = plugin.getSkillTreeManager();

        // Bonus dégâts passifs (Puissance I/II/III)
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // ============ 4. SYSTÈME DE CRITIQUE ============
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (Math.random() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0; // 150% de base
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);

            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // ============ 5. MOMENTUM SYSTEM ============
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(player);
        finalDamage *= momentumMultiplier;

        // ============ 6. EXECUTE DAMAGE (<20% HP cible) ============
        double mobHealthPercent = target.getHealth() / target.getMaxHealth() * 100;
        double executeThreshold = playerStats.getOrDefault(StatType.EXECUTE_THRESHOLD, 20.0);

        if (mobHealthPercent <= executeThreshold) {
            double executeBonus = playerStats.getOrDefault(StatType.EXECUTE_DAMAGE, 0.0);
            double skillExecuteBonus = skillManager.getSkillBonus(player, SkillBonus.EXECUTE_DAMAGE);
            finalDamage *= (1 + (executeBonus + skillExecuteBonus) / 100.0);
        }

        // ============ 7. BERSERKER (<30% HP joueur) ============
        double playerHealthPercent = player.getHealth() / player.getMaxHealth() * 100;
        if (playerHealthPercent <= 30) {
            double berserkerBonus = skillManager.getSkillBonus(player, SkillBonus.BERSERKER);
            if (berserkerBonus > 0) {
                finalDamage *= (1 + berserkerBonus / 100.0);
            }
        }

        // ============ 8. DÉGÂTS ÉLÉMENTAIRES ============
        double fireDamage = playerStats.getOrDefault(StatType.FIRE_DAMAGE, 0.0);
        double iceDamage = playerStats.getOrDefault(StatType.ICE_DAMAGE, 0.0);
        double lightningDamage = playerStats.getOrDefault(StatType.LIGHTNING_DAMAGE, 0.0);
        boolean lightningProc = false;

        if (fireDamage > 0) {
            finalDamage += fireDamage * 0.5;
        }
        if (iceDamage > 0) {
            finalDamage += iceDamage * 0.5;
        }
        if (lightningDamage > 0 && Math.random() < 0.15) {
            lightningProc = true;
            finalDamage += lightningDamage * 2;
        }

        // ============ 9. MULTIPLICATEUR ADDITIONNEL (synergies, frénésie, etc.) ============
        finalDamage *= damageMultiplier;

        // ============ 10. CORRUPTION DU VIDE (Endermite synergy) ============
        // Les cibles corrompues par l'Endermite subissent +25% de dégâts de TOUTES les sources
        double voidCorruptionBonus = getVoidCorruptionBonus(target.getUniqueId());
        if (voidCorruptionBonus > 0) {
            finalDamage *= (1 + voidCorruptionBonus);
        }

        // ============ 11. MULTIPLICATEUR BÊTE (équilibrage: 80% comme serviteurs) ============
        finalDamage *= 0.8;

        // ============ 12. LIFESTEAL ============
        double lifestealPercent = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);
        double skillLifesteal = skillManager.getSkillBonus(player, SkillBonus.LIFESTEAL);
        double totalLifesteal = lifestealPercent + skillLifesteal;
        double lifestealAmount = 0;

        if (totalLifesteal > 0) {
            lifestealAmount = finalDamage * (totalLifesteal / 100.0);
        }

        return new BeastDamageResult(
                Math.max(1.0, finalDamage),
                isCritical,
                lifestealAmount,
                fireDamage,
                iceDamage,
                lightningDamage,
                lightningProc);
    }

    /**
     * Applique les dégâts d'une bête avec le calcul COMPLET des stats du joueur.
     * Utilisé par LOUP, RENARD et IRON_GOLEM.
     *
     * @param owner Le joueur propriétaire
     * @param target La cible des dégâts
     * @param beastType Le type de bête
     * @param damageMultiplier Multiplicateur additionnel
     */
    private void applyBeastDamageWithFullStats(Player owner, LivingEntity target,
            BeastType beastType, double damageMultiplier) {

        BeastDamageResult result = calculateBeastDamageWithFullStats(owner, target, beastType, damageMultiplier);

        // Configurer les metadata pour l'indicateur de dégâts
        target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
        target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, result.isCritical));
        target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

        // Attribution du loot au propriétaire
        if (plugin.getZombieManager().isZombieZMob(target)) {
            target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));
        }

        // Marquer comme dégâts de bête (pour bypass la restriction arc/arbalète dans CombatListener)
        target.setMetadata("zombiez_beast_damage", new FixedMetadataValue(plugin, true));

        // Appliquer les dégâts
        target.damage(result.damage, owner);

        // Retirer le marqueur
        target.removeMetadata("zombiez_beast_damage", plugin);

        // Mettre à jour l'affichage de vie du zombie
        if (plugin.getZombieManager().isZombieZMob(target)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(target);
                }
            });
        }

        // Effet visuel critique
        if (result.isCritical) {
            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
        }

        // Appliquer les effets élémentaires
        applyBeastElementalEffects(target, result);

        // Lifesteal pour le propriétaire
        if (result.lifestealAmount > 0) {
            double newHealth = Math.min(owner.getHealth() + result.lifestealAmount, owner.getMaxHealth());
            owner.setHealth(newHealth);
            if (result.lifestealAmount > 1) {
                owner.getWorld().spawnParticle(Particle.HEART, owner.getLocation().add(0, 1.5, 0), 1, 0.2, 0.2, 0.2);
            }
        }
    }

    /**
     * Applique les effets élémentaires du stuff du joueur via sa bête
     */
    private void applyBeastElementalEffects(LivingEntity target, BeastDamageResult result) {
        // Feu - Enflamme la cible
        if (result.fireDamage > 0) {
            target.setFireTicks((int) (result.fireDamage * 20));
        }

        // Glace - Gèle la cible
        if (result.iceDamage > 0) {
            target.setFreezeTicks((int) (result.iceDamage * 20));
        }

        // Foudre - Effet visuel et sonore (15% chance, déjà calculée)
        if (result.lightningProc) {
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);
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
                applyBeastDamageWithMetadata(owner, target, bleedDamage);
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
        UUID ownerUuid = owner.getUniqueId();

        // Calculer le bonus de vitesse d'attaque actuel (0.0 à 1.5 = 0% à 150%)
        double speedBonus = axolotlAttackSpeedBonus.getOrDefault(ownerUuid, 0.0);

        // Vitesse totale = (1 + bonus) * frenzy
        // Cooldown de base: 1.5s, réduit par la vitesse
        double totalSpeedMultiplier = (1.0 + speedBonus) * frenzyMultiplier;
        long shootCooldown = (long) (1500 / totalSpeedMultiplier);

        // Minimum cooldown de 200ms pour éviter le spam extrême
        shootCooldown = Math.max(200, shootCooldown);

        // Système basé sur le temps réel pour gérer les hautes cadences
        // On calcule combien de tirs on peut faire depuis le dernier tir effectif
        long lastShot = axolotlLastShotTime.getOrDefault(ownerUuid, 0L);
        long timeSinceLastShot = now - lastShot;

        // Calculer le nombre de tirs possibles (max 3 par tick pour éviter le spam visuel)
        int shotsToFire = (int) Math.min(3, timeSinceLastShot / shootCooldown);

        if (shotsToFire <= 0) {
            return; // Pas encore le temps de tirer
        }

        // Trouver les cibles proches (on en a besoin pour les tirs multiples) - portée 24 blocs
        List<LivingEntity> nearbyEnemies = new ArrayList<>();
        for (Entity nearby : axolotl.getNearbyEntities(24, 16, 24)) {
            if (nearby instanceof Monster monster && !isBeast(nearby)) {
                nearbyEnemies.add(monster);
            }
        }

        if (nearbyEnemies.isEmpty()) {
            return; // Pas de cible
        }

        // Trier par distance
        Location axolotlLoc = axolotl.getLocation();
        nearbyEnemies.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(axolotlLoc)));

        // Tirer les bulles avec un léger délai entre chaque pour l'effet visuel
        for (int i = 0; i < shotsToFire; i++) {
            // Alterner entre les cibles si plusieurs tirs
            LivingEntity target = nearbyEnemies.get(i % nearbyEnemies.size());
            final int delay = i * 3; // 3 ticks = 150ms entre chaque bulle

            if (delay == 0) {
                shootWaterBubble(owner, axolotl, target);
            } else {
                LivingEntity finalTarget = target;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (axolotl.isValid() && !axolotl.isDead() && finalTarget.isValid() && !finalTarget.isDead()) {
                            shootWaterBubble(owner, axolotl, finalTarget);
                        }
                    }
                }.runTaskLater(plugin, delay);
            }
        }

        // Mettre à jour le timestamp du dernier tir
        // On avance le timestamp de (shotsToFire * shootCooldown) pour synchroniser correctement
        axolotlLastShotTime.put(ownerUuid, lastShot + (shotsToFire * shootCooldown));
    }

    /**
     * Incrémente le bonus de vitesse d'attaque de l'Axolotl après un hit.
     * +10% par attaque, max +150%
     */
    private void incrementAxolotlAttackSpeed(Player owner) {
        UUID ownerUuid = owner.getUniqueId();
        long now = System.currentTimeMillis();

        double currentBonus = axolotlAttackSpeedBonus.getOrDefault(ownerUuid, 0.0);
        double newBonus = Math.min(currentBonus + AXOLOTL_SPEED_INCREMENT, AXOLOTL_SPEED_MAX_BONUS);

        axolotlAttackSpeedBonus.put(ownerUuid, newBonus);
        axolotlLastAttackTime.put(ownerUuid, now);

        int newPercent = (int) Math.round(newBonus * 100);
        int oldPercent = (int) Math.round(currentBonus * 100);

        // Son de montée en puissance (pitch croissant) - feedback principal
        float pitch = 0.8f + (float) (newBonus / AXOLOTL_SPEED_MAX_BONUS) * 1.2f;
        owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, pitch);

        // Message chat aux paliers importants uniquement
        if (newPercent >= 50 && oldPercent < 50) {
            owner.sendMessage("§d✦ Axolotl: §e+50% §7vitesse d'attaque!");
        } else if (newPercent >= 100 && oldPercent < 100) {
            owner.sendMessage("§d✦ Axolotl: §6+100% §7vitesse d'attaque!");
        } else if (newPercent >= 150 && oldPercent < 150) {
            owner.sendMessage("§d✦ Axolotl: §c+150% §7vitesse MAX!");
            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
        }
    }

    private void shootWaterBubble(Player owner, LivingEntity axolotl, LivingEntity target) {
        Location start = axolotl.getLocation().add(0, 0.5, 0);
        Vector direction = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        // Créer un projectile visuel avec des particules
        // Vitesse: 1.6 blocs/tick, durée max: 30 ticks = 48 blocs de portée
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 30) {
                    cancel();
                    return;
                }

                // Déplacer la bulle (1.6 blocs/tick pour atteindre les cibles à 24 blocs)
                current.add(direction.clone().multiply(1.6));

                // Particules de bulle (réduites)
                current.getWorld().spawnParticle(Particle.BUBBLE_POP, current, 2, 0.1, 0.1, 0.1, 0);

                // Vérifier l'impact
                for (Entity entity : current.getWorld().getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                    if (entity instanceof LivingEntity living && entity != axolotl && !isBeast(entity) && entity != owner) {
                        living.damage(calculateBeastDamage(owner, BeastType.AXOLOTL), owner);
                        current.getWorld().playSound(current, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.5f);
                        current.getWorld().spawnParticle(Particle.SPLASH, current, 8, 0.3, 0.3, 0.3, 0);

                        // Incrémenter le bonus de vitesse d'attaque
                        incrementAxolotlAttackSpeed(owner);

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
        // Lancer une bouse explosive toutes les 5 secondes
        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver la meilleure cible (groupe d'ennemis ou ennemi proche)
            LivingEntity target = findBestCowTarget(cow);
            if (target != null) {
                launchExplosiveDung(owner, cow, target);
                setCooldown(owner.getUniqueId(), cooldownKey, now + 5000); // 5 secondes
            }
        }
    }

    /**
     * Trouve la meilleure cible pour la bouse (préfère les groupes d'ennemis)
     * Portée: 32 blocs
     */
    private LivingEntity findBestCowTarget(LivingEntity cow) {
        LivingEntity bestTarget = null;
        int bestScore = 0;

        // Portée de 32 blocs pour les bêtes à distance
        for (Entity nearby : cow.getNearbyEntities(32, 16, 32)) {
            if (!(nearby instanceof Monster monster) || isBeast(nearby)) continue;

            // Score basé sur le nombre d'ennemis autour de cette cible
            int nearbyEnemies = 0;
            for (Entity around : nearby.getNearbyEntities(3, 2, 3)) {
                if (around instanceof Monster && !isBeast(around)) {
                    nearbyEnemies++;
                }
            }

            // +1 pour la cible elle-même, bonus si groupe
            int score = 1 + nearbyEnemies * 2;

            // Préférer les cibles plus proches (bonus distance)
            double dist = cow.getLocation().distanceSquared(nearby.getLocation());
            if (dist < 36) score += 3; // < 6 blocs

            if (score > bestScore) {
                bestScore = score;
                bestTarget = monster;
            }
        }

        return bestTarget;
    }

    /**
     * Lance une bouse explosive vers la cible - trajectoire en arc
     * Utilise un calcul physique correct pour atteindre la cible
     */
    private void launchExplosiveDung(Player owner, LivingEntity cow, LivingEntity target) {
        Location start = cow.getLocation().add(0, 1.2, 0);
        Location targetGround = target.getLocation(); // Position au sol pour la détection d'impact
        Location targetLoc = targetGround.clone().add(0, 1.0, 0); // Viser le centre du mob

        // Calcul de trajectoire parabolique précise
        double dx = targetLoc.getX() - start.getX();
        double dy = targetLoc.getY() - start.getY();
        double dz = targetLoc.getZ() - start.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Gravité par tick (même que dans la boucle)
        double gravity = 0.08;

        // Calculer le temps de vol optimal basé sur la distance
        // Plus la distance est grande, plus le temps de vol est long
        double flightTime = Math.max(15, Math.min(45, horizontalDist * 0.8 + 10));

        // Vélocité horizontale pour couvrir la distance en flightTime ticks
        double horizontalSpeed = horizontalDist / flightTime;

        // Vélocité verticale pour atteindre dy avec la gravité
        // Formule: dy = vy*t - 0.5*g*t² => vy = (dy + 0.5*g*t²) / t
        double verticalSpeed = (dy + 0.5 * gravity * flightTime * flightTime) / flightTime;

        // Direction horizontale normalisée
        Vector horizontal = new Vector(dx, 0, dz).normalize().multiply(horizontalSpeed);
        Vector velocity = horizontal.setY(verticalSpeed);

        // Hauteur du sol pour la détection d'impact
        final double groundY = targetGround.getY();

        // Son de lancement
        cow.getWorld().playSound(start, Sound.ENTITY_COW_HURT, 1.5f, 0.6f);
        cow.getWorld().playSound(start, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.8f);

        // Projectile de bouse avec particules
        new BukkitRunnable() {
            Location current = start.clone();
            Vector vel = velocity.clone();
            int ticks = 0;
            boolean hasExploded = false;

            @Override
            public void run() {
                if (hasExploded || ticks > 60) { // Max 3 secondes
                    cancel();
                    return;
                }

                // Appliquer la gravité
                vel.setY(vel.getY() - 0.08);

                // Déplacer le projectile
                current.add(vel);

                // Particules de bouse volante (réduites)
                if (ticks % 2 == 0) {
                    current.getWorld().spawnParticle(Particle.BLOCK, current, 1, 0.1, 0.1, 0.1, 0,
                        Material.BROWN_TERRACOTTA.createBlockData());
                }

                // Vérifier impact au sol ou avec entité
                boolean shouldExplode = false;

                // Impact avec le sol (utilise groundY pour la hauteur de la cible au sol)
                if (current.getBlock().getType().isSolid() || current.getY() < groundY) {
                    shouldExplode = true;
                }

                // Impact avec une entité
                for (Entity entity : current.getWorld().getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                    if (entity instanceof Monster && !isBeast(entity)) {
                        shouldExplode = true;
                        break;
                    }
                }

                if (shouldExplode) {
                    explodeDung(owner, current);
                    hasExploded = true;
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Explosion de la bouse - dégâts AoE + knockback
     */
    private void explodeDung(Player owner, Location explosionLoc) {
        // Effets visuels (réduits)
        explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 1);
        explosionLoc.getWorld().spawnParticle(Particle.BLOCK, explosionLoc, 20, 2, 1, 2, 0.1,
            Material.BROWN_TERRACOTTA.createBlockData());
        explosionLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, explosionLoc, 6, 1.5, 0.5, 1.5, 0.02);

        // Sons
        explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
        explosionLoc.getWorld().playSound(explosionLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 2.0f, 0.5f);

        // Dégâts et knockback (80% des dégâts du joueur)
        double dungDamage = calculateBeastDamage(owner, BeastType.COW);
        int hitCount = 0;

        for (Entity entity : explosionLoc.getWorld().getNearbyEntities(explosionLoc, 4, 3, 4)) {
            if (entity instanceof LivingEntity living && !isBeast(entity) && entity != owner) {
                // Dégâts décroissants selon la distance
                double dist = living.getLocation().distance(explosionLoc);
                double damageMultiplier = Math.max(0.5, 1.0 - (dist / 6.0));

                applyBeastDamageWithMetadata(owner, living, dungDamage * damageMultiplier);

                // Knockback puissant
                Vector knockback = living.getLocation().subtract(explosionLoc).toVector();
                if (knockback.lengthSquared() > 0) {
                    knockback.normalize().multiply(1.8);
                }
                knockback.setY(0.7);
                living.setVelocity(knockback);

                hitCount++;
            }
        }
    }

    private void executeLlamaAbility(Player owner, LivingEntity llama, long now, String cooldownKey, double frenzyMultiplier) {
        // Cracher toutes les 2.5 secondes (amélioré pour Tier 6)
        long spitCooldown = (long) (2500 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver jusqu'à 5 cibles - portée 32 blocs
            List<LivingEntity> targets = new ArrayList<>(5);
            for (Entity nearby : llama.getNearbyEntities(32, 16, 32)) {
                if (nearby instanceof Monster monster && !isBeast(nearby)) {
                    targets.add(monster);
                    if (targets.size() >= 5) break; // Limite atteinte
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
     * Applique l'effet du crachat corrosif du lama (appelé par le listener)
     * Tier 6 - Dégâts augmentés + Lenteur III 5s + DoT Acide 15%/s pendant 4s
     */
    public void applyLlamaSpit(Player owner, LivingEntity target) {
        double damage = calculateBeastDamage(owner, BeastType.LLAMA);

        // Appliquer les dégâts d'impact avec metadata
        applyBeastDamageWithMetadata(owner, target, damage);

        // Lenteur III pendant 5 secondes (100 ticks)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2)); // Lenteur III, 5s

        // Particules d'acide à l'impact
        target.getWorld().spawnParticle(Particle.SPIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0);
        target.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, target.getLocation().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4, 0);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.5f);

        // DoT Acide: 15% des dégâts du joueur par seconde pendant 4 secondes
        double acidDamagePerTick = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.15;
        applyAcidDoT(owner, target, acidDamagePerTick, 4);
    }

    /**
     * Applique un DoT d'acide corrosif sur la cible
     * @param owner Le propriétaire du lama
     * @param target La cible
     * @param damagePerSecond Dégâts par seconde
     * @param durationSeconds Durée en secondes
     */
    private void applyAcidDoT(Player owner, LivingEntity target, double damagePerSecond, int durationSeconds) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20; // 4 secondes = 80 ticks

            @Override
            public void run() {
                if (ticks >= maxTicks || target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }

                // Appliquer les dégâts toutes les secondes (20 ticks)
                if (ticks % 20 == 0) {
                    applyBeastDamageWithMetadata(owner, target, damagePerSecond);

                    // Particules d'acide qui ronge
                    target.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM,
                        target.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0);
                    target.getWorld().spawnParticle(Particle.SMOKE,
                        target.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);

                    // Son d'acide qui grésille
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3f, 2.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeIronGolemAbility(Player owner, LivingEntity golem, long now, String cooldownKey, double frenzyMultiplier) {
        if (!(golem instanceof Mob golemMob)) return;

        // Frappe Titanesque toutes les 5 secondes
        long slamCooldown = (long) (5000 / frenzyMultiplier);

        // PRIORITÉ 1: Cible focus du joueur
        LivingEntity target = getPlayerFocusTarget(owner);

        // PRIORITÉ 2: Cible avec synergies (marquée ou stacks abeille)
        if (target == null) {
            target = findGolemSynergyTarget(golem);
        }

        // PRIORITÉ 3: Cible la plus proche du joueur
        if (target == null) {
            target = findNearestEnemy(owner, BEAST_COMBAT_RANGE);
        }

        // PRIORITÉ 4: Cible la plus proche du golem
        if (target == null) {
            target = findNearestEnemyFromBeast(golem, 15);
        }

        // Toujours définir la cible du Golem pour l'IA de base
        if (target != null && !target.isDead()) {
            if (golemMob.getTarget() != target) {
                golemMob.setTarget(target);
            }

            double distToTarget = golem.getLocation().distance(target.getLocation());

            // Si proche, attaquer en continu (l'IronGolem a une attaque de base)
            if (distToTarget > 3 && distToTarget < 20) {
                // Se déplacer vers la cible
                golemMob.getPathfinder().moveTo(target.getLocation(), 1.3);
            }

            // Frappe Titanesque si cooldown prêt et cible à portée (distance réduite pour plus de fiabilité)
            if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now) && distToTarget <= 20) {
                executeGolemCharge(owner, golem, target);
                setCooldown(owner.getUniqueId(), cooldownKey, now + slamCooldown);
            }
        }
    }

    /**
     * Trouve un ennemi proche d'une bête
     */
    private LivingEntity findNearestEnemyFromBeast(LivingEntity beast, double range) {
        LivingEntity nearest = null;
        double nearestDistSq = range * range;

        for (Entity nearby : beast.getNearbyEntities(range, range / 2, range)) {
            if (nearby instanceof Monster monster && !isBeast(nearby) && !monster.isDead()) {
                double distSq = beast.getLocation().distanceSquared(nearby.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = monster;
                }
            }
        }
        return nearest;
    }

    /**
     * Trouve une cible avec synergies pour le Golem (marquée par renard, stacks abeille, ou corruption du vide)
     */
    private LivingEntity findGolemSynergyTarget(LivingEntity golem) {
        LivingEntity bestTarget = null;
        double bestScore = 0;

        for (Entity nearby : golem.getNearbyEntities(15, 8, 15)) {
            if (!(nearby instanceof Monster monster) || isBeast(nearby) || monster.isDead()) continue;

            double score = 0;

            // Bonus si marqué par le renard (+50 - priorité haute)
            if (isMarkedByFox(nearby.getUniqueId())) {
                score += 50;
            }

            // Bonus selon les stacks d'abeille (+10 par stack, +30 bonus à 3+)
            int beeStacks = beeStingStacks.getOrDefault(nearby.getUniqueId(), 0);
            score += beeStacks * 10;
            if (beeStacks >= 3) {
                score += 30; // Bonus car dégâts x2
            }

            // Bonus si corrompu par l'Endermite (+40 - priorité haute)
            if (isVoidCorrupted(nearby.getUniqueId())) {
                score += 40;
            }

            // Seulement considérer si a une synergie
            if (score > 0) {
                // Bonus proximité (préférer les cibles plus proches)
                double dist = golem.getLocation().distanceSquared(nearby.getLocation());
                score += Math.max(0, (225 - dist) / 20); // Bonus jusqu'à 15 blocs

                // Bonus si groupe d'ennemis autour (pour l'onde de choc)
                int nearbyCount = 0;
                for (Entity around : nearby.getNearbyEntities(4, 2, 4)) {
                    if (around instanceof Monster && !isBeast(around)) nearbyCount++;
                }
                score += nearbyCount * 5;

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = monster;
                }
            }
        }

        return bestTarget;
    }

    /**
     * Le Golem charge vers la cible avec animation fluide
     */
    private void executeGolemCharge(Player owner, LivingEntity golem, LivingEntity target) {
        if (!(golem instanceof Mob golemMob)) return;

        Location startLoc = golem.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        // Phase 1: Préparation - le Golem se tourne vers sa cible
        orientBeastTowards(golem, targetLoc);

        // Désactiver temporairement l'IA du golem pendant la charge
        golemMob.setTarget(null);

        // Animation avec phase de préparation
        new BukkitRunnable() {
            int ticks = -8; // 8 ticks (0.4s) de préparation
            Set<UUID> hitDuringCharge = new HashSet<>();
            boolean hasCharged = false;

            @Override
            public void run() {
                if (golem.isDead()) {
                    cancel();
                    return;
                }

                // Phase de préparation: le Golem lève son bras
                if (ticks < 0) {
                    // Recalculer la direction vers la cible (qui peut bouger)
                    if (!target.isDead()) {
                        orientBeastTowards(golem, target.getLocation());
                    }

                    // Particules de préparation + son
                    if (ticks == -8) {
                        golem.getWorld().playSound(startLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.6f);
                    }
                    if (ticks == -4) {
                        golem.getWorld().playSound(startLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
                        golem.getWorld().spawnParticle(Particle.DUST, golem.getLocation().add(0, 2, 0),
                            5, 0.5, 0.3, 0.5, 0, new Particle.DustOptions(Color.fromRGB(200, 50, 50), 1.5f));
                    }
                    ticks++;
                    return;
                }

                // Calculer direction et distance au moment de la charge
                Location currentGolemLoc = golem.getLocation();
                Location currentTargetLoc = target.isDead() ? targetLoc : target.getLocation();
                Vector direction = currentTargetLoc.toVector().subtract(currentGolemLoc.toVector());
                double distance = direction.length();

                // Si la cible est très proche ou morte, slam directement
                if (distance < 2 || target.isDead()) {
                    executeGolemSlam(owner, golem, currentGolemLoc, direction.normalize(), hitDuringCharge);
                    cancel();
                    return;
                }

                direction.normalize();

                // Début de la charge
                if (!hasCharged) {
                    golem.getWorld().playSound(currentGolemLoc, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.5f);
                    hasCharged = true;
                }

                // Limite de temps de charge (max 1.5s = 30 ticks)
                if (ticks >= 30) {
                    executeGolemSlam(owner, golem, currentGolemLoc, direction, hitDuringCharge);
                    cancel();
                    return;
                }

                // Déplacer le golem avec vélocité (plus fluide que pathfinder)
                double chargeSpeed = 0.8;
                Vector velocity = direction.clone().multiply(chargeSpeed);
                velocity.setY(-0.1); // Légère gravité pour rester au sol
                golem.setVelocity(velocity);

                // Particules de charge (réduites)
                if (ticks % 2 == 0) {
                    golem.getWorld().spawnParticle(Particle.BLOCK, currentGolemLoc, 3, 0.5, 0.1, 0.5, 0,
                        Material.IRON_BLOCK.createBlockData());
                }

                // Dégâts aux ennemis sur le chemin
                for (Entity nearby : golem.getWorld().getNearbyEntities(currentGolemLoc, 1.8, 2, 1.8)) {
                    if (nearby instanceof LivingEntity living &&
                        nearby instanceof Monster &&
                        !isBeast(nearby) &&
                        !hitDuringCharge.contains(nearby.getUniqueId())) {

                        // Dégâts de charge (50% des dégâts) avec système complet
                        applyBeastDamageWithFullStats(owner, living, BeastType.IRON_GOLEM, 0.5);
                        hitDuringCharge.add(nearby.getUniqueId());

                        // Effet d'impact (réduit)
                        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.2f);
                    }
                }

                // Si on atteint la cible, slam!
                if (distance < 3) {
                    executeGolemSlam(owner, golem, currentGolemLoc, direction, hitDuringCharge);
                    cancel();
                    return;
                }

                // Son de pas lourds
                if (ticks % 3 == 0) {
                    golem.getWorld().playSound(currentGolemLoc, Sound.ENTITY_IRON_GOLEM_STEP, 1.5f, 0.7f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Impact de la Frappe Titanesque - onde de choc linéaire
     */
    private void executeGolemSlam(Player owner, LivingEntity golem, Location impactLoc, Vector direction, Set<UUID> alreadyHit) {
        // Sons d'impact
        golem.getWorld().playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 2.0f, 0.5f);
        golem.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        golem.getWorld().playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);

        // Particules d'impact central (réduites)
        golem.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 1);
        golem.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 30, 2, 0.5, 2, 0.1,
            Material.IRON_BLOCK.createBlockData());

        // Onde de choc linéaire (dans la direction de la charge)
        new BukkitRunnable() {
            int wave = 0;
            final int maxWaves = 8; // 8 blocs de portée
            Location wavePos = impactLoc.clone();

            @Override
            public void run() {
                if (wave >= maxWaves || golem.isDead()) {
                    cancel();
                    return;
                }

                // Avancer l'onde de choc
                wavePos.add(direction.clone().multiply(1.0));

                // Particules de l'onde (réduites - moins de positions)
                Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                for (double offset = -2.0; offset <= 2.0; offset += 1.0) {
                    Location particleLoc = wavePos.clone().add(perpendicular.clone().multiply(offset));
                    particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc.add(0, 0.2, 0), 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.5f));
                }

                // Effet de fissure au sol (réduit)
                wavePos.getWorld().spawnParticle(Particle.SWEEP_ATTACK, wavePos.clone().add(0, 0.5, 0), 1, 1, 0, 1, 0);

                // Dégâts aux ennemis dans l'onde (largeur 5 blocs, perpendiculaire)
                for (Entity nearby : wavePos.getWorld().getNearbyEntities(wavePos, 2.5, 2, 2.5)) {
                    if (!(nearby instanceof LivingEntity living) ||
                        !(nearby instanceof Monster) ||
                        isBeast(nearby) ||
                        alreadyHit.contains(nearby.getUniqueId())) {
                        continue;
                    }

                    alreadyHit.add(nearby.getUniqueId());

                    // Calcul du multiplicateur avec synergies
                    double damageMultiplier = 1.0;
                    boolean hasSynergyBonus = false;

                    // Bonus double dégâts si marqué par le renard
                    if (isMarkedByFox(nearby.getUniqueId())) {
                        damageMultiplier = 2.0;
                        hasSynergyBonus = true;
                    }

                    // Bonus si stacks d'abeille (double aussi à 3+ stacks)
                    int beeStacks = beeStingStacks.getOrDefault(nearby.getUniqueId(), 0);
                    if (beeStacks >= 3) {
                        damageMultiplier = Math.max(damageMultiplier, 2.0);
                        hasSynergyBonus = true;
                    }

                    // Appliquer les dégâts avec le système complet (comme serviteurs Occultiste)
                    applyBeastDamageWithFullStats(owner, living, BeastType.IRON_GOLEM, damageMultiplier);

                    // Stun (1.5s = 30 ticks de Slowness V + Jump Boost négatif)
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4, false, false, false)); // 1.5s
                    living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30, 128, false, false, false)); // Bloque les sauts

                    // Projection légère
                    Vector knockback = direction.clone().multiply(0.8);
                    knockback.setY(0.4);
                    living.setVelocity(knockback);

                    // Effets visuels (réduits)
                    if (hasSynergyBonus) {
                        // Effets spéciaux pour synergie
                        living.getWorld().spawnParticle(Particle.DUST, living.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0,
                            new Particle.DustOptions(Color.ORANGE, 1.5f));
                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);
                    } else {
                        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.2);
                    }
                }

                // Son de l'onde
                if (wave % 2 == 0) {
                    wavePos.getWorld().playSound(wavePos, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);
                }

                wave++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Une onde tous les 2 ticks
    }

    // === ABEILLE - ESSAIM VENIMEUX ===

    /**
     * Capacité de l'abeille: attaque en essaim avec piqûres empilables
     */
    private void executeBeeAbility(Player owner, LivingEntity bee, long now, String cooldownKey, double frenzyMultiplier) {
        // Attaque toutes les 2 secondes
        long stingCooldown = (long) (2000 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver jusqu'à 3 cibles proches
            List<LivingEntity> targets = findBeeTargets(bee, 3);
            if (!targets.isEmpty()) {
                launchSwarmAttack(owner, bee, targets);
                setCooldown(owner.getUniqueId(), cooldownKey, now + stingCooldown);
            }
        }
    }

    /**
     * Trouve les cibles pour l'essaim (priorité aux cibles déjà piquées)
     */
    private List<LivingEntity> findBeeTargets(LivingEntity bee, int maxTargets) {
        List<LivingEntity> targets = new ArrayList<>();
        List<LivingEntity> candidates = new ArrayList<>();

        // Portée de 24 blocs pour les bêtes à distance
        for (Entity nearby : bee.getNearbyEntities(24, 16, 24)) {
            if (nearby instanceof Monster monster && !isBeast(nearby)) {
                candidates.add(monster);
            }
        }

        // Trier: priorité aux cibles avec des stacks existants
        candidates.sort((a, b) -> {
            int stacksA = beeStingStacks.getOrDefault(a.getUniqueId(), 0);
            int stacksB = beeStingStacks.getOrDefault(b.getUniqueId(), 0);
            return Integer.compare(stacksB, stacksA); // Descending
        });

        for (int i = 0; i < Math.min(maxTargets, candidates.size()); i++) {
            targets.add(candidates.get(i));
        }

        return targets;
    }

    /**
     * Lance une attaque en essaim sur les cibles
     */
    private void launchSwarmAttack(Player owner, LivingEntity bee, List<LivingEntity> targets) {
        Location beeLocation = bee.getLocation().add(0, 0.5, 0);

        // Son d'essaim
        bee.getWorld().playSound(beeLocation, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.5f, 1.5f);

        for (LivingEntity target : targets) {
            // Créer un projectile visuel d'abeille
            launchStingProjectile(owner, bee, target);
        }
    }

    /**
     * Lance un projectile de piqûre vers la cible
     */
    private void launchStingProjectile(Player owner, LivingEntity bee, LivingEntity target) {
        Location start = bee.getLocation().add(0, 0.5, 0);

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20 || target.isDead()) {
                    cancel();
                    return;
                }

                // Direction vers la cible (homing)
                Vector direction = target.getLocation().add(0, 1, 0)
                    .subtract(current).toVector();
                if (direction.lengthSquared() < 1) {
                    // Impact!
                    applyBeeSting(owner, bee, target);
                    cancel();
                    return;
                }
                direction.normalize().multiply(1.2);

                // Déplacer le projectile
                current.add(direction);

                // Particules d'abeille (réduites)
                if (ticks % 2 == 0) {
                    current.getWorld().spawnParticle(Particle.DUST, current, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.YELLOW, 0.6f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Applique une piqûre d'abeille et gère les stacks
     */
    private void applyBeeSting(Player owner, LivingEntity bee, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();

        // Dégâts de base (10% des dégâts du joueur)
        double stingDamage = calculateBeastDamage(owner, BeastType.BEE);
        applyBeastDamageWithMetadata(owner, target, stingDamage);

        // Ajouter un stack
        int currentStacks = beeStingStacks.getOrDefault(targetUuid, 0);
        int newStacks = currentStacks + 1;

        // Effets visuels de piqûre (réduits)
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.2, 0),
            3, 0.2, 0.2, 0.2, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BEE_STING, 1.0f, 1.2f);

        if (newStacks >= BEE_MAX_STACKS) {
            // Explosion de venin!
            triggerVenomExplosion(owner, target);
            beeStingStacks.remove(targetUuid);
        } else {
            beeStingStacks.put(targetUuid, newStacks);

            // Particule de stack simple (réduit)
            target.getWorld().spawnParticle(Particle.DUST,
                target.getLocation().add(0, 2.0, 0),
                1, 0.2, 0.1, 0.2, 0,
                new Particle.DustOptions(Color.ORANGE, 0.5f));
        }
    }

    /**
     * Déclenche l'explosion de venin à 5 stacks
     */
    private void triggerVenomExplosion(Player owner, LivingEntity target) {
        Location loc = target.getLocation().add(0, 1, 0);

        // Dégâts massifs (150% des dégâts du joueur)
        double explosionDamage = calculateBeastDamage(owner, BeastType.BEE) * BEE_VENOM_EXPLOSION_DAMAGE * 10; // x10 car 10% de base
        applyBeastDamageWithMetadata(owner, target, explosionDamage);

        // Poison
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1)); // Poison II, 3s

        // Effets visuels (réduits)
        target.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.5, 0.5, 0.5, 0.1,
            new Particle.DustOptions(Color.YELLOW, 1.5f));
        target.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0.1,
            new Particle.DustOptions(Color.fromRGB(50, 200, 50), 1.2f));
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc, 2, 0.3, 0.3, 0.3, 0);

        // Sons
        target.getWorld().playSound(loc, Sound.ENTITY_BEE_DEATH, 2.0f, 0.5f);
        target.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.5f, 1.5f);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1.0f, 0.8f);
    }

    /**
     * Nettoie les stacks de piqûres pour une entité morte
     */
    public void cleanupBeeStings(UUID targetUuid) {
        beeStingStacks.remove(targetUuid);
    }

    // === RENARD - TRAQUE & BOND ===

    /**
     * Capacité du renard: bondit sur les ennemis et les marque
     */
    private void executeFoxAbility(Player owner, LivingEntity fox, long now, String cooldownKey, double frenzyMultiplier) {
        // Bond toutes les 3 secondes
        long pounceCooldown = (long) (3000 / frenzyMultiplier);

        if (!isOnCooldown(owner.getUniqueId(), cooldownKey, now)) {
            // Trouver la meilleure cible (priorité aux blessés)
            LivingEntity target = findFoxTarget(fox);
            if (target != null) {
                executeFoxPounce(owner, fox, target);
                setCooldown(owner.getUniqueId(), cooldownKey, now + pounceCooldown);
            }
        }

        // Nettoyer les marques expirées
        foxMarkedEntities.entrySet().removeIf(entry -> now > entry.getValue());
    }

    /**
     * Trouve la meilleure cible pour le renard (priorité aux blessés)
     */
    private LivingEntity findFoxTarget(LivingEntity fox) {
        LivingEntity bestTarget = null;
        double bestScore = 0;

        for (Entity nearby : fox.getNearbyEntities(10, 5, 10)) {
            if (!(nearby instanceof LivingEntity living) || !(nearby instanceof Monster) || isBeast(nearby)) continue;

            double maxHealth = living.getAttribute(Attribute.MAX_HEALTH).getValue();
            double currentHealth = living.getHealth();
            double healthPercent = currentHealth / maxHealth;

            // Score: préfère les cibles blessées et proches
            double score = (1.0 - healthPercent) * 3; // Bonus pour blessés
            double dist = fox.getLocation().distanceSquared(nearby.getLocation());
            score += Math.max(0, (100 - dist) / 20); // Bonus proximité

            // Bonus si pas déjà marqué
            if (!foxMarkedEntities.containsKey(nearby.getUniqueId())) {
                score += 2;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = living;
            }
        }

        return bestTarget;
    }

    /**
     * Le renard bondit sur la cible et la marque
     */
    private void executeFoxPounce(Player owner, LivingEntity fox, LivingEntity target) {
        Location foxLoc = fox.getLocation();
        Location targetLoc = target.getLocation();

        // Phase 1: Préparation - le renard regarde sa cible et se met en position
        orientBeastTowards(fox, targetLoc);

        // Son de préparation au bond
        fox.getWorld().playSound(foxLoc, Sound.ENTITY_FOX_SNIFF, 1.0f, 1.5f);

        // Animation de bond avec delay de préparation
        Vector direction = targetLoc.toVector().subtract(foxLoc.toVector()).normalize();
        direction.setY(0.5); // Arc de bond
        direction.multiply(1.5);

        // Phase 2: Bond après 5 ticks de préparation
        new BukkitRunnable() {
            int ticks = -5; // 5 ticks de préparation
            Location current = foxLoc.clone();
            boolean hasLeaped = false;

            @Override
            public void run() {
                if (fox.isDead() || target.isDead()) {
                    cancel();
                    return;
                }

                // Phase de préparation (accroupi)
                if (ticks < 0) {
                    // Regarder la cible
                    orientBeastTowards(fox, target.getLocation());
                    ticks++;
                    return;
                }

                // Saut initial
                if (!hasLeaped) {
                    fox.getWorld().playSound(fox.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.2f);
                    hasLeaped = true;
                }

                if (ticks >= 8) {
                    // Arrivée - infliger dégâts et marquer
                    applyFoxMark(owner, fox, target);
                    cancel();
                    return;
                }

                // Déplacer le renard
                current.add(direction.clone().multiply(0.3));
                current.setY(current.getY() + (ticks < 4 ? 0.15 : -0.15)); // Arc

                if (fox instanceof Mob mob) {
                    mob.getPathfinder().moveTo(current, 2.0);
                }

                // Particules de traînée (réduites)
                if (ticks % 3 == 0) {
                    fox.getWorld().spawnParticle(Particle.DUST, fox.getLocation().add(0, 0.5, 0),
                        1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.ORANGE, 0.8f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Son de bond
        fox.getWorld().playSound(foxLoc, Sound.ENTITY_FOX_AGGRO, 1.5f, 1.2f);
    }

    /**
     * Applique la marque du renard sur la cible.
     * La cible marquée devient visible à travers les murs (glowing) et subit +30% de dégâts.
     * Utilise le système de dégâts complet (comme serviteurs Occultiste).
     */
    private void applyFoxMark(Player owner, LivingEntity fox, LivingEntity target) {
        if (target.isDead()) return;

        // Dégâts initiaux avec le calcul complet des stats du joueur
        applyBeastDamageWithFullStats(owner, target, BeastType.FOX, 1.0);

        // Marquer la cible (5 secondes)
        foxMarkedEntities.put(target.getUniqueId(), System.currentTimeMillis() + 5000);

        // Appliquer l'effet GLOWING pour voir la cible à travers les murs
        target.setGlowing(true);

        // Effets visuels de marque (réduits)
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.5, 0),
            6, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 2, 0),
            4, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(Color.RED, 1.2f));

        // Son
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FOX_BITE, 1.5f, 1.0f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.5f);

        // Particules continues sur la cible marquée + gestion du glowing
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Vérifier si la marque a expiré
                if (ticks >= 100 || target.isDead() || !foxMarkedEntities.containsKey(target.getUniqueId())) {
                    // Retirer l'effet glowing quand la marque expire
                    if (!target.isDead()) {
                        target.setGlowing(false);
                    }
                    cancel();
                    return;
                }

                // Particule de marque toutes les 20 ticks (réduit)
                if (ticks % 20 == 0) {
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 2.2, 0),
                        1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.RED, 0.6f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    /**
     * Vérifie si une entité est marquée par le renard
     * @return Le bonus de dégâts (0.0 si non marquée, FOX_MARK_DAMAGE_BONUS si marquée)
     */
    public double getFoxMarkBonus(UUID targetUuid) {
        Long expiration = foxMarkedEntities.get(targetUuid);
        if (expiration == null || System.currentTimeMillis() > expiration) {
            return 0.0;
        }
        return FOX_MARK_DAMAGE_BONUS;
    }

    /**
     * Vérifie si une entité est marquée par le renard (boolean)
     */
    public boolean isMarkedByFox(UUID targetUuid) {
        return getFoxMarkBonus(targetUuid) > 0;
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
        // Note: Toutes les bêtes sont invincibles, pas de respawn nécessaire
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
        if (id.contains("beast_endermite")) return BeastType.ENDERMITE;
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
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.5, 1, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.5, 1, 0.5, 0,
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
