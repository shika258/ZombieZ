package com.rinaorc.zombiez.pets.display;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.PetManager;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.PlayerPetData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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

    // Maps des TextDisplay hologrammes: Player UUID -> TextDisplay Entity UUID
    private final Map<UUID, UUID> hologramLine1 = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> hologramLine2 = new ConcurrentHashMap<>();

    // Cache du dernier texte affiché pour optimisation (éviter updates inutiles)
    private final Map<UUID, String> lastLine1Text = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastLine2Text = new ConcurrentHashMap<>();

    // Task de mise à jour des positions
    private BukkitTask updateTask;

    // Task de mise à jour des noms (pour le timer ultime)
    private BukkitTask nameUpdateTask;

    // Constantes de comportement
    private static final double TELEPORT_DISTANCE = 15.0;
    private static final double FOLLOW_START_DISTANCE = 3.5;
    private static final double IDEAL_DISTANCE = 2.5;
    private static final double MAX_SPEED = 0.4;
    private static final double ACCELERATION = 0.15;

    // Constantes pour les hologrammes TextDisplay
    private static final double HOLOGRAM_Y_OFFSET = 1.85;
    private static final double HOLOGRAM_LINE_SPACING = 0.25;

    // Interpolation fluide pour les hologrammes
    private static final int HOLOGRAM_INTERPOLATION_TICKS = 3; // Durée d'interpolation client-side

    // Cache des positions de base des hologrammes pour interpolation fluide
    private final Map<UUID, Location> hologramBaseLocations = new ConcurrentHashMap<>();

    public PetDisplayManager(ZombieZPlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;

        // Tâche de suivi toutes les 2 ticks pour plus de fluidité
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPets, 2L, 2L);

        // Tâche de mise à jour des noms toutes les secondes (pour le timer ultime)
        nameUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPetNames, 20L, 20L);
    }

    /**
     * Spawn l'entité visuelle du pet pour un joueur
     */
    public void spawnPetDisplay(Player player, PetType type) {
        // Vérifier que le joueur est en ligne et dans un monde valide
        if (!player.isOnline() || player.getWorld() == null) return;

        // Vérifier l'option d'affichage du pet
        PlayerPetData playerData = petManager.getPlayerData(player.getUniqueId());
        if (playerData != null && !playerData.isShowPetEntity()) return;

        // Retirer l'ancien pet s'il existe
        removePetDisplay(player);

        Location spawnLoc = getFollowLocation(player);

        // Créer l'entité basée sur le type de pet
        Entity petEntity = spawnPetEntity(spawnLoc, type, player);
        if (petEntity == null) return;

        // Configurer l'entité
        configurePetEntity(petEntity, type, player);

        // Enregistrer l'entité et le monde actuel
        UUID playerUuid = player.getUniqueId();
        activePetEntities.put(playerUuid, petEntity.getUniqueId());
        playerWorlds.put(playerUuid, player.getWorld().getUID());
        targetLocations.put(playerUuid, spawnLoc.clone());

        // Créer les hologrammes TextDisplay
        PetData petData = playerData != null ? playerData.getPet(type) : null;
        if (petData != null) {
            createHologramDisplays(playerUuid, petEntity.getLocation(), type, petData);
        }
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

        // Supprimer les hologrammes TextDisplay
        removeHologramDisplays(playerUuid);

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

        // Mettre à jour la position des hologrammes (toutes les 2 ticks pour fluidité)
        updateHologramPositions(playerUuid, entity.getLocation());
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
        if (isFlyingEntity(entity.getType())) {
            // Les entités volantes ont besoin d'un Y pour voler
            velocity.setY(velocity.getY() + 0.05); // Légère poussée vers le haut
        } else {
            // Entités au sol - ne pas modifier Y trop agressivement
            velocity.setY(Math.max(-0.5, Math.min(0.3, velocity.getY())));
        }

        entity.setVelocity(velocity);
    }

    /**
     * Vérifie si un type d'entité peut voler
     */
    private boolean isFlyingEntity(EntityType type) {
        return type == EntityType.BAT || type == EntityType.VEX ||
               type == EntityType.ALLAY || type == EntityType.BEE ||
               type == EntityType.PARROT || type == EntityType.PHANTOM ||
               type == EntityType.BLAZE || type == EntityType.GHAST;
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
        // Supprimer les hologrammes TextDisplay
        removeHologramDisplays(playerUuid);

        // Supprimer l'entité du pet
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        activePetEntities.remove(playerUuid);
        playerWorlds.remove(playerUuid);
        targetLocations.remove(playerUuid);
    }

    /**
     * Met à jour les noms de tous les pets (pour le timer ultime)
     * Utilise les TextDisplay hologrammes au lieu de setCustomName()
     */
    private void updateAllPetNames() {
        for (Map.Entry<UUID, UUID> entry : activePetEntities.entrySet()) {
            UUID playerUuid = entry.getKey();
            UUID entityUuid = entry.getValue();

            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) continue;

            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity == null || !entity.isValid()) continue;

            // Optimisation: skip si le chunk n'est pas chargé
            if (!entity.getLocation().isChunkLoaded()) continue;

            PlayerPetData playerData = petManager.getPlayerData(playerUuid);
            if (playerData == null || playerData.getEquippedPet() == null) continue;

            PetType type = playerData.getEquippedPet();
            PetData petData = playerData.getPet(type);
            if (petData == null) continue;

            // Mettre à jour les hologrammes TextDisplay (optimisé - seulement si changement)
            updateHologramText(playerUuid, type, petData);
        }
    }

    /**
     * Construit la ligne 1 de l'hologramme : Nom du pet / niveau (+ étoiles)
     * Exemple : §bPanthère §7[Lv.12] §e★★
     */
    private String buildPetLine1(PetType type, PetData petData) {
        StringBuilder line = new StringBuilder();
        line.append(type.getColoredName());
        line.append(" §7[Lv.").append(petData.getLevel()).append("]");
        if (petData.getStarPower() > 0) {
            line.append(" §e").append("★".repeat(petData.getStarPower()));
        }
        return line.toString();
    }

    /**
     * Construit la ligne 2 de l'hologramme : Timer d'ultime
     * Si cooldown > 0 : §d⚡ Ultime: §f{remaining}s
     * Si prêt : §a⚡ Ultime: §lPRÊTE!
     * Si pas d'ultime : null (pas de ligne 2)
     */
    private String buildPetLine2(PetType type, UUID playerUuid) {
        // Pas de ligne 2 si le pet n'a pas d'ultime
        if (type.getUltimateCooldown() <= 0) {
            return null;
        }

        int remainingSeconds = petManager.getCooldownRemainingSeconds(playerUuid, type);
        if (remainingSeconds > 0) {
            return "§d⚡ Ultime: §f" + remainingSeconds + "s";
        } else {
            return "§a⚡ Ultime: §lPRÊTE!";
        }
    }

    /**
     * Crée les TextDisplay hologrammes pour un pet avec configuration d'interpolation fluide
     */
    private void createHologramDisplays(UUID playerUuid, Location petLoc, PetType type, PetData petData) {
        World world = petLoc.getWorld();
        if (world == null) return;

        // Supprimer les anciens hologrammes s'ils existent
        removeHologramDisplays(playerUuid);

        // Stocker la position de base pour l'interpolation fluide
        hologramBaseLocations.put(playerUuid, petLoc.clone());

        // Calculer les positions
        Location line1Loc = petLoc.clone().add(0, HOLOGRAM_Y_OFFSET, 0);
        Location line2Loc = petLoc.clone().add(0, HOLOGRAM_Y_OFFSET - HOLOGRAM_LINE_SPACING, 0);

        // Créer la ligne 1 (nom + niveau + étoiles)
        String line1Text = buildPetLine1(type, petData);
        TextDisplay line1 = spawnTextDisplay(world, line1Loc, line1Text);
        if (line1 != null) {
            hologramLine1.put(playerUuid, line1.getUniqueId());
            lastLine1Text.put(playerUuid, line1Text);
        }

        // Créer la ligne 2 (timer ultime) seulement si le pet a une ultime
        String line2Text = buildPetLine2(type, playerUuid);
        if (line2Text != null) {
            TextDisplay line2 = spawnTextDisplay(world, line2Loc, line2Text);
            if (line2 != null) {
                hologramLine2.put(playerUuid, line2.getUniqueId());
                lastLine2Text.put(playerUuid, line2Text);
            }
        }
    }

    /**
     * Spawn un TextDisplay configuré pour l'affichage hologramme avec interpolation fluide
     */
    private TextDisplay spawnTextDisplay(World world, Location loc, String text) {
        TextDisplay display = (TextDisplay) world.spawnEntity(loc, EntityType.TEXT_DISPLAY);

        // Configuration du texte
        display.setText(text);

        // Billboard centré (face toujours la caméra)
        display.setBillboard(Billboard.CENTER);

        // Pas de background (transparent)
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

        // Pas d'ombre
        display.setShadowed(false);

        // Taille raisonnable avec transformation initiale
        display.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),           // Translation (sera utilisé pour le mouvement fluide)
            new AxisAngle4f(0, 0, 0, 1),     // Rotation gauche
            new Vector3f(1f, 1f, 1f),        // Échelle
            new AxisAngle4f(0, 0, 0, 1)      // Rotation droite
        ));

        // Configuration de l'interpolation native pour mouvement fluide
        // Le client interpolera automatiquement entre les états pour 60Hz+
        display.setInterpolationDuration(HOLOGRAM_INTERPOLATION_TICKS);
        display.setInterpolationDelay(0);

        // Distance de vue standard
        display.setViewRange(0.5f);

        // Configuration supplémentaire
        display.setPersistent(false);
        display.setInvulnerable(true);

        // Tags pour identification
        display.addScoreboardTag("zombiez_pet_hologram");

        return display;
    }

    /**
     * Supprime les TextDisplay hologrammes d'un joueur
     */
    private void removeHologramDisplays(UUID playerUuid) {
        // Supprimer la ligne 1
        UUID line1Uuid = hologramLine1.remove(playerUuid);
        if (line1Uuid != null) {
            Entity entity = Bukkit.getEntity(line1Uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        // Supprimer la ligne 2
        UUID line2Uuid = hologramLine2.remove(playerUuid);
        if (line2Uuid != null) {
            Entity entity = Bukkit.getEntity(line2Uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        // Nettoyer les caches
        lastLine1Text.remove(playerUuid);
        lastLine2Text.remove(playerUuid);
        hologramBaseLocations.remove(playerUuid);
    }

    /**
     * Met à jour la position des hologrammes pour suivre le pet avec interpolation fluide
     * Utilise le système de Translation de Minecraft pour un mouvement smooth côté client
     */
    private void updateHologramPositions(UUID playerUuid, Location petLoc) {
        Location baseLoc = hologramBaseLocations.get(playerUuid);

        // Si pas de position de base ou trop loin (>8 blocs), reset via teleport
        if (baseLoc == null || !baseLoc.getWorld().equals(petLoc.getWorld()) ||
            baseLoc.distanceSquared(petLoc) > 64) { // 8² = 64
            // Reset: teleport et mettre à jour la position de base
            hologramBaseLocations.put(playerUuid, petLoc.clone());
            teleportHologramsToPosition(playerUuid, petLoc);
            return;
        }

        // Calculer le delta depuis la position de base
        // Note: l'offset Y (HOLOGRAM_Y_OFFSET) est déjà appliqué lors du spawn,
        // donc on applique seulement le delta de mouvement
        double deltaX = petLoc.getX() - baseLoc.getX();
        double deltaY = petLoc.getY() - baseLoc.getY();
        double deltaZ = petLoc.getZ() - baseLoc.getZ();

        // Mise à jour ligne 1 avec interpolation
        UUID line1Uuid = hologramLine1.get(playerUuid);
        if (line1Uuid != null) {
            Entity entity = Bukkit.getEntity(line1Uuid);
            if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
                // Appliquer la translation fluide (seulement le delta, l'offset Y est déjà dans la position spawn)
                textDisplay.setInterpolationDuration(HOLOGRAM_INTERPOLATION_TICKS);
                textDisplay.setTransformation(new Transformation(
                    new Vector3f((float) deltaX, (float) deltaY, (float) deltaZ),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }

        // Mise à jour ligne 2 avec interpolation
        UUID line2Uuid = hologramLine2.get(playerUuid);
        if (line2Uuid != null) {
            Entity entity = Bukkit.getEntity(line2Uuid);
            if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
                // Appliquer la translation fluide (seulement le delta)
                textDisplay.setInterpolationDuration(HOLOGRAM_INTERPOLATION_TICKS);
                textDisplay.setTransformation(new Transformation(
                    new Vector3f((float) deltaX, (float) deltaY, (float) deltaZ),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }
    }

    /**
     * Teleporte les hologrammes à une position (utilisé pour reset lors de grandes distances)
     */
    private void teleportHologramsToPosition(UUID playerUuid, Location petLoc) {
        // Teleport ligne 1
        UUID line1Uuid = hologramLine1.get(playerUuid);
        if (line1Uuid != null) {
            Entity entity = Bukkit.getEntity(line1Uuid);
            if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
                Location newLoc = petLoc.clone().add(0, HOLOGRAM_Y_OFFSET, 0);
                textDisplay.teleport(newLoc);
                // Reset la transformation après teleport
                textDisplay.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }

        // Teleport ligne 2
        UUID line2Uuid = hologramLine2.get(playerUuid);
        if (line2Uuid != null) {
            Entity entity = Bukkit.getEntity(line2Uuid);
            if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
                Location newLoc = petLoc.clone().add(0, HOLOGRAM_Y_OFFSET - HOLOGRAM_LINE_SPACING, 0);
                textDisplay.teleport(newLoc);
                // Reset la transformation après teleport
                textDisplay.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }
    }

    /**
     * Met à jour le texte des hologrammes (optimisé - seulement si changement)
     */
    private void updateHologramText(UUID playerUuid, PetType type, PetData petData) {
        // Mise à jour ligne 1 (nom + niveau + étoiles)
        String newLine1 = buildPetLine1(type, petData);
        String oldLine1 = lastLine1Text.get(playerUuid);

        if (!newLine1.equals(oldLine1)) {
            UUID line1Uuid = hologramLine1.get(playerUuid);
            if (line1Uuid != null) {
                Entity entity = Bukkit.getEntity(line1Uuid);
                if (entity instanceof TextDisplay textDisplay) {
                    textDisplay.setText(newLine1);
                    lastLine1Text.put(playerUuid, newLine1);
                }
            }
        }

        // Mise à jour ligne 2 (timer ultime)
        String newLine2 = buildPetLine2(type, playerUuid);
        String oldLine2 = lastLine2Text.get(playerUuid);

        // Gérer le cas où la ligne 2 doit apparaître/disparaître
        UUID line2Uuid = hologramLine2.get(playerUuid);

        if (newLine2 == null) {
            // Pas d'ultime - supprimer la ligne 2 si elle existe
            if (line2Uuid != null) {
                Entity entity = Bukkit.getEntity(line2Uuid);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
                hologramLine2.remove(playerUuid);
                lastLine2Text.remove(playerUuid);
            }
        } else if (newLine2.equals(oldLine2)) {
            // Pas de changement - ne rien faire
        } else {
            // Texte changé - mettre à jour
            if (line2Uuid != null) {
                Entity entity = Bukkit.getEntity(line2Uuid);
                if (entity instanceof TextDisplay textDisplay) {
                    textDisplay.setText(newLine2);
                    lastLine2Text.put(playerUuid, newLine2);
                }
            } else {
                // La ligne 2 n'existe pas mais devrait - la créer
                UUID petEntityUuid = activePetEntities.get(playerUuid);
                if (petEntityUuid != null) {
                    Entity petEntity = Bukkit.getEntity(petEntityUuid);
                    if (petEntity != null && petEntity.isValid()) {
                        Location line2Loc = petEntity.getLocation().add(0, HOLOGRAM_Y_OFFSET - HOLOGRAM_LINE_SPACING, 0);
                        TextDisplay line2 = spawnTextDisplay(petEntity.getWorld(), line2Loc, newLine2);
                        if (line2 != null) {
                            hologramLine2.put(playerUuid, line2.getUniqueId());
                            lastLine2Text.put(playerUuid, newLine2);
                        }
                    }
                }
            }
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

        // Spawn l'entité selon le type configuré dans PetType
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
                cat.setCatType(getCatType(type));
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
                Chicken chicken = (Chicken) world.spawnEntity(loc, EntityType.CHICKEN);
                chicken.setBaby();
                entity = chicken;
            }
            case RABBIT -> {
                Rabbit rabbit = (Rabbit) world.spawnEntity(loc, EntityType.RABBIT);
                rabbit.setRabbitType(Rabbit.Type.BLACK);
                rabbit.setBaby();
                entity = rabbit;
            }
            case SLIME -> {
                Slime slime = (Slime) world.spawnEntity(loc, EntityType.SLIME);
                slime.setSize(1); // Taille minimale
                entity = slime;
            }
            case BLAZE -> {
                Blaze blaze = (Blaze) world.spawnEntity(loc, EntityType.BLAZE);
                entity = blaze;
            }
            case SPIDER -> {
                Spider spider = (Spider) world.spawnEntity(loc, EntityType.SPIDER);
                entity = spider;
            }
            case PHANTOM -> {
                Phantom phantom = (Phantom) world.spawnEntity(loc, EntityType.PHANTOM);
                phantom.setSize(0); // Taille minimale
                entity = phantom;
            }
            case SILVERFISH -> {
                Silverfish silverfish = (Silverfish) world.spawnEntity(loc, EntityType.SILVERFISH);
                entity = silverfish;
            }
            case ENDERMITE -> {
                Endermite endermite = (Endermite) world.spawnEntity(loc, EntityType.ENDERMITE);
                entity = endermite;
            }
            case AXOLOTL -> {
                Axolotl axolotl = (Axolotl) world.spawnEntity(loc, EntityType.AXOLOTL);
                axolotl.setVariant(getAxolotlVariant(type));
                entity = axolotl;
            }
            case SQUID -> {
                Squid squid = (Squid) world.spawnEntity(loc, EntityType.SQUID);
                entity = squid;
            }
            case ENDERMAN -> {
                Enderman enderman = (Enderman) world.spawnEntity(loc, EntityType.ENDERMAN);
                entity = enderman;
            }
            case SHULKER -> {
                Shulker shulker = (Shulker) world.spawnEntity(loc, EntityType.SHULKER);
                shulker.setColor(getShulkerColor(type));
                entity = shulker;
            }
            case ENDER_DRAGON -> {
                // Les EnderDragons sont trop grands et problématiques - utiliser un Phantom stylisé
                Phantom dragonPet = (Phantom) world.spawnEntity(loc, EntityType.PHANTOM);
                dragonPet.setSize(2); // Un peu plus grand pour ressembler à un dragon
                entity = dragonPet;
            }
            case ARMOR_STAND -> {
                // Pour les golems et autres - ArmorStand visible avec équipement
                ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
                stand.setVisible(true);
                stand.setSmall(true);
                stand.setArms(true);
                stand.setBasePlate(false);
                configureArmorStandAppearance(stand, type);
                entity = stand;
            }
            default -> {
                // Fallback: Allay (visible et petit)
                Allay fallback = (Allay) world.spawnEntity(loc, EntityType.ALLAY);
                entity = fallback;
            }
        }

        return entity;
    }

    /**
     * Configure l'apparence d'un ArmorStand selon le type de pet
     */
    private void configureArmorStandAppearance(ArmorStand stand, PetType type) {
        org.bukkit.inventory.ItemStack helmet = null;

        switch (type) {
            case GOLEM_POCHE -> helmet = new org.bukkit.inventory.ItemStack(Material.IRON_BLOCK);
            case GOLEM_CRISTAL -> helmet = new org.bukkit.inventory.ItemStack(Material.AMETHYST_CLUSTER);
            case GOLEM_LAVE -> helmet = new org.bukkit.inventory.ItemStack(Material.MAGMA_BLOCK);
            case TITAN_MINIATURE -> helmet = new org.bukkit.inventory.ItemStack(Material.NETHERITE_BLOCK);
            case COLOSSUS_OUBLIE -> helmet = new org.bukkit.inventory.ItemStack(Material.ANCIENT_DEBRIS);
            default -> helmet = new org.bukkit.inventory.ItemStack(Material.STONE);
        }

        if (helmet != null) {
            stand.getEquipment().setHelmet(helmet);
        }
    }

    /**
     * Obtient le type de chat selon le pet
     */
    private Cat.Type getCatType(PetType type) {
        return switch (type) {
            case FELIN_OMBRE -> Cat.Type.ALL_BLACK;
            default -> Cat.Type.BLACK;
        };
    }

    /**
     * Obtient la variante d'axolotl selon le type de pet
     */
    private Axolotl.Variant getAxolotlVariant(PetType type) {
        return switch (type) {
            case SALAMANDRE_ELEMENTAIRE -> Axolotl.Variant.GOLD;
            default -> Axolotl.Variant.WILD;
        };
    }

    /**
     * Obtient la couleur du shulker selon le type de pet
     */
    private DyeColor getShulkerColor(PetType type) {
        return switch (type.getRarity()) {
            case COMMON -> DyeColor.GRAY;
            case UNCOMMON -> DyeColor.LIME;
            case RARE -> DyeColor.CYAN;
            case EPIC -> DyeColor.PURPLE;
            case LEGENDARY -> DyeColor.YELLOW;
            case MYTHIC -> DyeColor.RED;
        };
    }

    /**
     * Configure l'entité du pet (nom, tags, etc.)
     */
    private void configurePetEntity(Entity entity, PetType type, Player player) {
        // Ne plus utiliser setCustomName() - l'affichage est géré par les TextDisplay hologrammes
        // On désactive le customName pour éviter les doublons visuels
        entity.setCustomNameVisible(false);

        // Tags pour identification
        entity.addScoreboardTag("zombiez_pet");
        entity.addScoreboardTag("pet_owner_" + player.getUniqueId());
        entity.addScoreboardTag("pet_type_" + type.name());

        // Configuration commune
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setPersistent(false);
        entity.setGravity(!isFlyingEntity(entity.getType()));

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
        if (nameUpdateTask != null) {
            nameUpdateTask.cancel();
        }

        // Supprimer tous les hologrammes TextDisplay
        for (UUID line1Uuid : hologramLine1.values()) {
            Entity entity = Bukkit.getEntity(line1Uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        hologramLine1.clear();

        for (UUID line2Uuid : hologramLine2.values()) {
            Entity entity = Bukkit.getEntity(line2Uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        hologramLine2.clear();

        // Nettoyer les caches
        lastLine1Text.clear();
        lastLine2Text.clear();
        hologramBaseLocations.clear();

        // Supprimer les entités de pet
        for (UUID entityUuid : activePetEntities.values()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activePetEntities.clear();
        playerWorlds.clear();
        targetLocations.clear();
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
