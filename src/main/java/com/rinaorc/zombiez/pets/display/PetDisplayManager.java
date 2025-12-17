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
 * Utilise des entités avec AI personnalisée pour suivre le joueur
 */
public class PetDisplayManager {

    private final ZombieZPlugin plugin;
    private final PetManager petManager;

    // Map des entités de pet actives: Player UUID -> Entity UUID
    private final Map<UUID, UUID> activePetEntities = new ConcurrentHashMap<>();

    // Task de mise à jour des positions
    private BukkitTask updateTask;

    public PetDisplayManager(ZombieZPlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;

        // Tâche de suivi toutes les 5 ticks
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPets, 5L, 5L);
    }

    /**
     * Spawn l'entité visuelle du pet pour un joueur
     */
    public void spawnPetDisplay(Player player, PetType type) {
        // Retirer l'ancien pet s'il existe
        removePetDisplay(player);

        Location spawnLoc = getFollowLocation(player);

        // Créer l'entité basée sur le type de pet
        Entity petEntity = spawnPetEntity(spawnLoc, type, player);
        if (petEntity == null) return;

        // Configurer l'entité
        configurePetEntity(petEntity, type, player);

        // Enregistrer
        activePetEntities.put(player.getUniqueId(), petEntity.getUniqueId());
    }

    /**
     * Retire l'entité visuelle du pet d'un joueur
     */
    public void removePetDisplay(Player player) {
        UUID entityUuid = activePetEntities.remove(player.getUniqueId());
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    /**
     * Met à jour l'affichage du pet (position, nom, etc.)
     */
    public void updatePetDisplay(Player player) {
        UUID entityUuid = activePetEntities.get(player.getUniqueId());
        if (entityUuid == null) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null || !entity.isValid()) {
            // L'entité n'existe plus, en recréer une
            PlayerPetData data = petManager.getPlayerData(player.getUniqueId());
            if (data != null && data.getEquippedPet() != null) {
                activePetEntities.remove(player.getUniqueId());
                spawnPetDisplay(player, data.getEquippedPet());
            }
            return;
        }

        // Mettre à jour la position si trop loin
        double distance = entity.getLocation().distance(player.getLocation());

        if (distance > 10) {
            // Téléporter si trop loin
            entity.teleport(getFollowLocation(player));
        } else if (distance > 3) {
            // Se déplacer vers le joueur
            Location targetLoc = getFollowLocation(player);
            Vector direction = targetLoc.toVector().subtract(entity.getLocation().toVector()).normalize();
            entity.setVelocity(direction.multiply(0.3));
        }
    }

    /**
     * Met à jour tous les pets
     */
    private void updateAllPets() {
        for (Map.Entry<UUID, UUID> entry : activePetEntities.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                // Nettoyer les pets de joueurs déconnectés
                Entity entity = Bukkit.getEntity(entry.getValue());
                if (entity != null) entity.remove();
                activePetEntities.remove(entry.getKey());
                continue;
            }

            updatePetDisplay(player);
        }
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
        spawnRarityParticles(entity, type);
    }

    /**
     * Spawn des particules selon la rareté du pet
     */
    private void spawnRarityParticles(Entity entity, PetType type) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!entity.isValid()) {
                task.cancel();
                return;
            }

            Location loc = entity.getLocation().add(0, 0.5, 0);

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
