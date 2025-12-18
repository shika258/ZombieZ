package com.rinaorc.zombiez.pets.display;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.PetManager;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.PlayerPetData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère l'affichage visuel des pets en jeu
 * Système de suivi intelligent avec gestion complète des cas limites
 */
public class PetDisplayManager {

    private final ZombieZPlugin plugin;
    private final PetManager petManager;

    // Map des entités de pet actives: Player UUID -> Entity UUID
    private final Map<UUID, UUID> activePetEntities = new ConcurrentHashMap<>();

    // Map des positions cibles pour le suivi fluide
    private final Map<UUID, Location> targetLocations = new ConcurrentHashMap<>();

    // Map des mondes des joueurs pour détecter les changements
    private final Map<UUID, UUID> playerWorlds = new ConcurrentHashMap<>();

    // Task de mise à jour des positions
    private BukkitTask updateTask;

    // Constantes de comportement
    private static final double TELEPORT_DISTANCE = 15.0;
    private static final double FOLLOW_START_DISTANCE = 3.5;
    private static final double IDEAL_DISTANCE = 2.5;
    private static final double MAX_SPEED = 0.4;
    private static final double ACCELERATION = 0.15;

    public PetDisplayManager(ZombieZPlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;

        // Tâche de suivi toutes les 2 ticks pour plus de fluidité
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPets, 2L, 2L);
    }

    /**
     * Spawn l'entité visuelle du pet pour un joueur
     */
    public void spawnPetDisplay(Player player, PetType type) {
        // Vérifier que le joueur est en ligne et dans un monde valide
        if (!player.isOnline() || player.getWorld() == null) return;

        // Vérifier l'option d'affichage du pet
        PlayerPetData data = petManager.getPlayerData(player.getUniqueId());
        if (data != null && !data.isShowPetEntity()) return;

        // Retirer l'ancien pet s'il existe
        removePetDisplay(player);

        Location spawnLoc = getFollowLocation(player);

        // Créer l'entité basée sur le type de pet
        Entity petEntity = spawnPetEntity(spawnLoc, type, player);
        if (petEntity == null) return;

        // Configurer l'entité
        configurePetEntity(petEntity, type, player);

        // Enregistrer l'entité et le monde actuel
        activePetEntities.put(player.getUniqueId(), petEntity.getUniqueId());
        playerWorlds.put(player.getUniqueId(), player.getWorld().getUID());
        targetLocations.put(player.getUniqueId(), spawnLoc.clone());
    }

    /**
     * Retire l'entité visuelle du pet d'un joueur
     */
    public void removePetDisplay(Player player) {
        removePetDisplayByUUID(player.getUniqueId());
    }

    /**
     * Retire l'entité visuelle par UUID du joueur (pour déconnexion)
     */
    public void removePetDisplayByUUID(UUID playerUuid) {
        UUID entityUuid = activePetEntities.remove(playerUuid);
        playerWorlds.remove(playerUuid);
        targetLocations.remove(playerUuid);

        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null && entity.isValid()) {
                // Effet de disparition
                Location loc = entity.getLocation();
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
                }
                entity.remove();
            }
        }
    }

    /**
     * Vérifie si un joueur a un pet actif
     */
    public boolean hasPetDisplay(UUID playerUuid) {
        return activePetEntities.containsKey(playerUuid);
    }

    /**
     * Met à jour l'affichage du pet (position, nom, etc.)
     */
    public void updatePetDisplay(Player player) {
        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = activePetEntities.get(playerUuid);
        if (entityUuid == null) return;

        // Vérifier si le joueur a changé de monde
        UUID currentWorldUuid = player.getWorld().getUID();
        UUID storedWorldUuid = playerWorlds.get(playerUuid);
        if (storedWorldUuid != null && !storedWorldUuid.equals(currentWorldUuid)) {
            // Changement de monde détecté - recréer le pet
            PlayerPetData data = petManager.getPlayerData(playerUuid);
            if (data != null && data.getEquippedPet() != null && data.isShowPetEntity()) {
                removePetDisplayByUUID(playerUuid);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        spawnPetDisplay(player, data.getEquippedPet());
                    }
                }, 10L); // Délai pour laisser le temps au joueur d'arriver
            }
            return;
        }

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null || !entity.isValid() || entity.isDead()) {
            // L'entité n'existe plus, en recréer une
            PlayerPetData data = petManager.getPlayerData(playerUuid);
            if (data != null && data.getEquippedPet() != null && data.isShowPetEntity()) {
                activePetEntities.remove(playerUuid);
                targetLocations.remove(playerUuid);
                spawnPetDisplay(player, data.getEquippedPet());
            }
            return;
        }

        // Vérifier que le pet est dans le même monde que le joueur
        if (!entity.getWorld().getUID().equals(player.getWorld().getUID())) {
            // Téléporter le pet vers le joueur
            entity.teleport(getFollowLocation(player));
            return;
        }

        // Calculer la position cible idéale
        Location targetLoc = getFollowLocation(player);
        Location entityLoc = entity.getLocation();

        // Vérifier si le joueur est dans un véhicule
        if (player.isInsideVehicle()) {
            // Suivre à plus grande distance et ne pas bloquer
            targetLoc = player.getLocation().clone().add(
                player.getLocation().getDirection().multiply(-3).setY(0)
            ).add(0, 1, 0);
        }

        double distance = entityLoc.distance(player.getLocation());

        // Téléportation si trop loin ou dans un mauvais état
        if (distance > TELEPORT_DISTANCE || entity.getLocation().getY() < player.getLocation().getY() - 10) {
            teleportPetWithEffect(entity, targetLoc);
            return;
        }

        // Suivi intelligent
        if (distance > FOLLOW_START_DISTANCE) {
            moveTowardsTarget(entity, targetLoc, entityLoc, distance);
        } else {
            // Arrêter le mouvement si proche
            if (entity.getVelocity().lengthSquared() > 0.01) {
                entity.setVelocity(entity.getVelocity().multiply(0.5));
            }

            // Faire regarder le pet vers le joueur si c'est un LivingEntity
            if (entity instanceof LivingEntity living && !(entity instanceof Tameable)) {
                Location lookAt = player.getLocation();
                Vector direction = lookAt.toVector().subtract(entityLoc.toVector()).normalize();
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                living.setRotation(yaw, 0);
            }
        }

        // Mettre à jour la position cible
        targetLocations.put(playerUuid, targetLoc);
    }

    /**
     * Déplace le pet vers la cible de manière fluide
     */
    private void moveTowardsTarget(Entity entity, Location target, Location current, double distance) {
        Vector direction = target.toVector().subtract(current.toVector());

        // Calculer la vitesse en fonction de la distance
        double speed = Math.min(MAX_SPEED, (distance - IDEAL_DISTANCE) * ACCELERATION);
        speed = Math.max(0.1, speed);

        // Appliquer le mouvement
        Vector velocity = direction.normalize().multiply(speed);

        // Ajuster pour les entités volantes
        if (entity.getType() == EntityType.BAT || entity.getType() == EntityType.VEX ||
            entity.getType() == EntityType.ALLAY || entity.getType() == EntityType.BEE ||
            entity.getType() == EntityType.PARROT) {
            // Les entités volantes ont besoin d'un Y pour voler
            velocity.setY(velocity.getY() + 0.05); // Légère poussée vers le haut
        } else {
            // Entités au sol - ne pas modifier Y trop agressivement
            velocity.setY(Math.max(-0.5, Math.min(0.3, velocity.getY())));
        }

        entity.setVelocity(velocity);
    }

    /**
     * Téléporte le pet avec un effet visuel
     */
    private void teleportPetWithEffect(Entity entity, Location target) {
        // Effet de départ
        Location oldLoc = entity.getLocation();
        if (oldLoc.getWorld() != null) {
            oldLoc.getWorld().spawnParticle(Particle.PORTAL, oldLoc.add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.1);
        }

        // Téléportation
        entity.teleport(target);

        // Effet d'arrivée
        if (target.getWorld() != null) {
            target.getWorld().spawnParticle(Particle.PORTAL, target.add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.5f);
        }
    }

    /**
     * Met à jour tous les pets
     */
    private void updateAllPets() {
        for (Map.Entry<UUID, UUID> entry : activePetEntities.entrySet()) {
            UUID playerUuid = entry.getKey();
            Player player = Bukkit.getPlayer(playerUuid);

            if (player == null || !player.isOnline()) {
                // Joueur déconnecté - nettoyer immédiatement
                cleanupDisconnectedPlayer(playerUuid, entry.getValue());
                continue;
            }

            // Vérifier si le joueur est mort
            if (player.isDead()) {
                // Cacher temporairement le pet
                Entity entity = Bukkit.getEntity(entry.getValue());
                if (entity != null && entity.isValid()) {
                    entity.setCustomNameVisible(false);
                    entity.teleport(player.getLocation().add(0, -5, 0)); // Cacher sous le sol
                }
                continue;
            }

            try {
                updatePetDisplay(player);
            } catch (Exception e) {
                // En cas d'erreur, nettoyer et recréer au prochain tick
                plugin.getLogger().warning("Erreur mise à jour pet pour " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Nettoie les données d'un joueur déconnecté
     */
    private void cleanupDisconnectedPlayer(UUID playerUuid, UUID entityUuid) {
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        activePetEntities.remove(playerUuid);
        playerWorlds.remove(playerUuid);
        targetLocations.remove(playerUuid);
    }

    /**
     * Calcule la position où le pet doit suivre
     */
    private Location getFollowLocation(Player player) {
        Location playerLoc = player.getLocation();

        // Position à 2.5 blocs derrière et 0.5 bloc à droite du joueur
        Vector behind = playerLoc.getDirection().multiply(-2.5);
        Vector right = new Vector(-playerLoc.getDirection().getZ(), 0, playerLoc.getDirection().getX())
            .normalize().multiply(0.5);

        return playerLoc.add(behind).add(right).add(0, 0.5, 0);
    }

    /**
     * Spawn l'entité appropriée selon le type de pet
     */
    private Entity spawnPetEntity(Location loc, PetType type, Player player) {
        World world = loc.getWorld();
        if (world == null) return null;

        Entity entity;

        // Utiliser un ArmorStand invisible avec un custom model comme base
        // Pour simplifier, on utilise des entités existantes configurées
        switch (type.getEntityType()) {
            case BAT -> {
                Bat bat = (Bat) world.spawnEntity(loc, EntityType.BAT);
                bat.setAwake(true);
                entity = bat;
            }
            case PARROT -> {
                Parrot parrot = (Parrot) world.spawnEntity(loc, EntityType.PARROT);
                parrot.setVariant(getParrotVariant(type));
                entity = parrot;
            }
            case WOLF -> {
                Wolf wolf = (Wolf) world.spawnEntity(loc, EntityType.WOLF);
                wolf.setTamed(true);
                wolf.setOwner(player);
                wolf.setSitting(false);
                wolf.setCollarColor(getDyeColor(type));
                entity = wolf;
            }
            case CAT -> {
                Cat cat = (Cat) world.spawnEntity(loc, EntityType.CAT);
                cat.setTamed(true);
                cat.setOwner(player);
                cat.setSitting(false);
                cat.setCatType(Cat.Type.ALL_BLACK);
                entity = cat;
            }
            case BEE -> {
                Bee bee = (Bee) world.spawnEntity(loc, EntityType.BEE);
                bee.setHasNectar(true);
                entity = bee;
            }
            case ALLAY -> {
                Allay allay = (Allay) world.spawnEntity(loc, EntityType.ALLAY);
                entity = allay;
            }
            case VEX -> {
                Vex vex = (Vex) world.spawnEntity(loc, EntityType.VEX);
                vex.setCharging(false);
                entity = vex;
            }
            case CHICKEN -> {
                // Pour les phénix
                Chicken chicken = (Chicken) world.spawnEntity(loc, EntityType.CHICKEN);
                chicken.setBaby();
                entity = chicken;
            }
            default -> {
                // Fallback: ArmorStand invisible
                ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
                stand.setVisible(false);
                stand.setSmall(true);
                stand.setMarker(true);
                entity = stand;
            }
        }

        return entity;
    }

    /**
     * Configure l'entité du pet (nom, tags, etc.)
     */
    private void configurePetEntity(Entity entity, PetType type, Player player) {
        PlayerPetData playerData = petManager.getPlayerData(player.getUniqueId());
        PetData petData = playerData != null ? playerData.getPet(type) : null;

        // Nom affiché
        String displayName = type.getColoredName();
        if (petData != null) {
            displayName += " §7[Lv." + petData.getLevel() + "]";
            if (petData.getStarPower() > 0) {
                displayName += " §e" + "★".repeat(petData.getStarPower());
            }
        }
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);

        // Tags pour identification
        entity.addScoreboardTag("zombiez_pet");
        entity.addScoreboardTag("pet_owner_" + player.getUniqueId());
        entity.addScoreboardTag("pet_type_" + type.name());

        // Configuration commune
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setPersistent(false);
        entity.setGravity(entity.getType() != EntityType.VEX && entity.getType() != EntityType.BAT && entity.getType() != EntityType.ALLAY);

        // Désactiver l'AI pour les mobs
        if (entity instanceof Mob mob) {
            mob.setAware(false);
            mob.setTarget(null);
            // Pour les loups et chats, ils suivront naturellement leur propriétaire
            if (entity instanceof Tameable) {
                mob.setAware(true);
            }
        }

        // Effets de particules selon la rareté
        spawnRarityParticles(entity, type, player);
    }

    /**
     * Spawn des particules selon la rareté du pet
     */
    private void spawnRarityParticles(Entity entity, PetType type, Player owner) {
        UUID ownerUuid = owner.getUniqueId();

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            // Arrêter si l'entité n'est plus valide
            if (!entity.isValid()) {
                task.cancel();
                return;
            }

            // Vérifier l'option de particules du joueur
            PlayerPetData data = petManager.getPlayerData(ownerUuid);
            if (data == null || !data.isShowPetParticles()) {
                return; // Ne pas afficher les particules mais garder la tâche
            }

            Location loc = entity.getLocation().add(0, 0.5, 0);
            if (loc.getWorld() == null) return;

            switch (type.getRarity()) {
                case EPIC -> loc.getWorld().spawnParticle(Particle.WITCH, loc, 2, 0.2, 0.2, 0.2, 0);
                case LEGENDARY -> {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 3, 0.2, 0.2, 0.2, 0.02);
                    loc.getWorld().spawnParticle(Particle.GLOW, loc, 1, 0.1, 0.1, 0.1, 0);
                }
                case MYTHIC -> {
                    loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.5);
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.2, 0.2, 0.2, 0);
                }
                default -> {} // Pas de particules pour common/uncommon/rare
            }
        }, 10L, 10L);
    }

    /**
     * Obtient la variante de perroquet selon le type de pet
     */
    private Parrot.Variant getParrotVariant(PetType type) {
        return switch (type) {
            case CORBEAU_MESSAGER -> Parrot.Variant.GRAY;
            case HIBOU_ARCANIQUE -> Parrot.Variant.BLUE;
            default -> Parrot.Variant.RED;
        };
    }

    /**
     * Obtient la couleur de collier selon le type de pet
     */
    private DyeColor getDyeColor(PetType type) {
        return switch (type.getRarity()) {
            case COMMON -> DyeColor.GRAY;
            case UNCOMMON -> DyeColor.LIME;
            case RARE -> DyeColor.CYAN;
            case EPIC -> DyeColor.PURPLE;
            case LEGENDARY -> DyeColor.ORANGE;
            case MYTHIC -> DyeColor.RED;
        };
    }

    /**
     * Retire tous les affichages (pour shutdown)
     */
    public void removeAllDisplays() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        for (UUID entityUuid : activePetEntities.values()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activePetEntities.clear();
    }

    /**
     * Vérifie si une entité est un pet
     */
    public boolean isPetEntity(Entity entity) {
        return entity.getScoreboardTags().contains("zombiez_pet");
    }

    /**
     * Obtient le propriétaire d'un pet
     */
    public UUID getPetOwner(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("pet_owner_")) {
                try {
                    return UUID.fromString(tag.substring("pet_owner_".length()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }
}
