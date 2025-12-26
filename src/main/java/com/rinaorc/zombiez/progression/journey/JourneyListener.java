package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.api.events.PlayerZoneChangeEvent;
import com.rinaorc.zombiez.api.events.ZombieDeathEvent;
import com.rinaorc.zombiez.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener principal du système de Parcours
 *
 * Responsabilités:
 * 1. Tracker les actions du joueur pour mettre à jour la progression
 * 2. BLOQUER l'accès aux zones non débloquées
 * 3. Afficher la progression dans l'ActionBar
 */
public class JourneyListener implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    public JourneyListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Tâche périodique pour mettre à jour la BossBar de progression
        startBossBarUpdateTask();

        // Tâche périodique pour tracker le temps de survie en zone
        startSurvivalTimeTracker();
    }

    // ==================== GESTION CONNEXION/DÉCONNEXION ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Charger les données de parcours et créer la BossBar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                journeyManager.loadPlayerJourney(player);

                // Créer la BossBar de progression
                journeyManager.createOrUpdateBossBar(player);

                // Envoyer le WorldBorder basé sur la progression
                if (plugin.getZoneBorderManager() != null) {
                    plugin.getZoneBorderManager().sendInitialBorder(player);
                }

                // Afficher le message de bienvenue avec la progression
                showJourneyWelcome(player);
            }
        }.runTaskLater(plugin, 40L); // 2 secondes après connexion
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();

        // Supprimer la BossBar
        journeyManager.removeBossBar(player);

        journeyManager.unloadPlayer(uuid);

        // Supprimer le joueur du cache WorldBorder
        if (plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().removePlayer(uuid);
        }

        // Nettoyer les caches locaux
        blockMessageCooldown.remove(uuid);
        zoneEntryTime.remove(uuid);
        lastZoneId.remove(uuid);
    }

    // ==================== BLOCAGE DES ZONES (CRITIQUE) ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onZoneChange(PlayerZoneChangeEvent event) {
        Player player = event.getPlayer();
        int newZoneId = event.getToZone().getId();

        // Vérifier si le joueur peut accéder à cette zone
        if (!journeyManager.canAccessZone(player, newZoneId)) {
            // BLOQUER L'ACCÈS
            event.setCancelled(true);

            // Envoyer le message de blocage
            JourneyGate gate = JourneyGate.getZoneGate(newZoneId);
            if (gate != null) {
                journeyManager.sendBlockedMessage(player, gate);
            }

            // Repousser le joueur
            journeyManager.pushBackFromZone(player, newZoneId);

            return;
        }

        // Le joueur peut accéder - mettre à jour la progression si c'est une nouvelle zone
        if (event.isFirstTime()) {
            updateZoneProgress(player, newZoneId);
        }
    }

    /**
     * Backup: Vérifier aussi sur le mouvement pour empêcher les contournements
     * + Tracker la progression d'exploration de zone par chunks
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Pré-calcul des coordonnées (évite les appels répétés)
        int fromX = event.getFrom().getBlockX();
        int fromZ = event.getFrom().getBlockZ();
        int toX = event.getTo().getBlockX();
        int toZ = event.getTo().getBlockZ();

        // Optimisation: ne vérifier que si le joueur a changé de bloc
        if (fromX == toX && fromZ == toZ) {
            return;
        }

        Player player = event.getPlayer();

        // Obtenir la zone de destination
        var zone = plugin.getZoneManager().getZoneAt(event.getTo());
        if (zone == null) return;

        int zoneId = zone.getId();

        // Vérifier si le joueur peut y accéder
        if (!journeyManager.canAccessZone(player, zoneId)) {
            // Annuler le mouvement
            event.setCancelled(true);

            // Envoyer le message (avec cooldown pour éviter le spam)
            sendBlockedMessageWithCooldown(player, zoneId);
            return;
        }

        // Optimisation: tracker l'exploration UNIQUEMENT si changement de chunk (16x16 blocs)
        // Réduit les appels de ~16x par rapport à chaque bloc
        int fromChunkX = fromX >> 4;
        int fromChunkZ = fromZ >> 4;
        int toChunkX = toX >> 4;
        int toChunkZ = toZ >> 4;

        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            trackChunkExploration(player, toChunkX, toChunkZ, zone);
        }
    }

    /**
     * Rayon d'exploration en chunks (correspond à la view distance du joueur)
     * Un rayon de 8 chunks = 128 blocs de visibilité dans chaque direction
     */
    private static final int EXPLORATION_RADIUS = 8;
    private static final int EXPLORATION_RADIUS_SQUARED = EXPLORATION_RADIUS * EXPLORATION_RADIUS;

    /**
     * Tracke l'exploration d'un chunk dans une zone (OPTIMISÉ)
     * Marque le chunk ET tous les chunks visibles (dans le rayon de view distance) comme explorés
     *
     * Optimisations appliquées:
     * - Early exit si pas d'objectif ZONE_PROGRESS ou ZONE_EXPLORATION actif
     * - Pré-calcul des limites de zone (évite recalcul dans la boucle)
     * - Bornes de boucle limitées à l'intersection zone/rayon
     *
     * @param chunkX coordonnée X du chunk central (position du joueur)
     * @param chunkZ coordonnée Z du chunk central (position du joueur)
     */
    private void trackChunkExploration(Player player, int chunkX, int chunkZ, com.rinaorc.zombiez.zones.Zone zone) {
        // EARLY EXIT: Vérifier si le joueur a un objectif d'exploration AVANT tout calcul
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null ||
            (currentStep.getType() != JourneyStep.StepType.ZONE_PROGRESS &&
             currentStep.getType() != JourneyStep.StepType.ZONE_EXPLORATION)) {
            return; // Pas d'objectif d'exploration, on skip tout le travail
        }

        // Pour ZONE_EXPLORATION, vérifier que c'est la bonne zone
        if (currentStep.getType() == JourneyStep.StepType.ZONE_EXPLORATION) {
            int targetZone = currentStep.getTargetValue(); // La zone cible est stockée dans targetValue
            if (zone.getId() != targetZone) {
                return; // Pas la bonne zone
            }
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        int zoneId = zone.getId();

        // Pré-calculer les limites de la zone UNE SEULE FOIS
        int zoneMinChunkX = zone.getMinChunkX();
        int zoneMaxChunkX = zone.getMaxChunkX();
        int zoneMinChunkZ = zone.getMinChunkZ();
        int zoneMaxChunkZ = zone.getMaxChunkZ();

        // Calculer les bornes de la boucle: intersection entre le cercle et la zone
        // Évite d'itérer sur des chunks qui sont forcément hors zone
        int startX = Math.max(chunkX - EXPLORATION_RADIUS, zoneMinChunkX);
        int endX = Math.min(chunkX + EXPLORATION_RADIUS, zoneMaxChunkX);
        int startZ = Math.max(chunkZ - EXPLORATION_RADIUS, zoneMinChunkZ);
        int endZ = Math.min(chunkZ + EXPLORATION_RADIUS, zoneMaxChunkZ);

        int newChunksExplored = 0;

        // Itérer UNIQUEMENT sur les chunks dans l'intersection zone/rayon
        for (int targetChunkX = startX; targetChunkX <= endX; targetChunkX++) {
            int dx = targetChunkX - chunkX;
            int dxSquared = dx * dx;

            for (int targetChunkZ = startZ; targetChunkZ <= endZ; targetChunkZ++) {
                int dz = targetChunkZ - chunkZ;

                // Vérifier si le chunk est dans le cercle de vision
                if (dxSquared + dz * dz > EXPLORATION_RADIUS_SQUARED) continue;

                // Marquer le chunk comme exploré
                if (data.markChunkExplored(zoneId, targetChunkX, targetChunkZ)) {
                    newChunksExplored++;
                }
            }
        }

        // Mettre à jour la progression seulement si de nouveaux chunks ont été découverts
        if (newChunksExplored > 0) {
            int exploredCount = data.getExploredChunkCount(zoneId);
            int explorationPercent = zone.getExplorationPercent(exploredCount);

            // Mettre à jour selon le type d'étape
            if (currentStep.getType() == JourneyStep.StepType.ZONE_PROGRESS) {
                journeyManager.updateProgress(player, JourneyStep.StepType.ZONE_PROGRESS, explorationPercent);
            } else if (currentStep.getType() == JourneyStep.StepType.ZONE_EXPLORATION) {
                journeyManager.updateProgress(player, JourneyStep.StepType.ZONE_EXPLORATION, explorationPercent);
            }
        }
    }

    // Cooldown pour les messages de blocage
    private final java.util.Map<java.util.UUID, Long> blockMessageCooldown = new java.util.concurrent.ConcurrentHashMap<>();

    // Tracking du temps de survie en zone (UUID -> timestamp d'entrée dans la zone)
    private final java.util.Map<java.util.UUID, Long> zoneEntryTime = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Integer> lastZoneId = new java.util.concurrent.ConcurrentHashMap<>();

    private void sendBlockedMessageWithCooldown(Player player, int zoneId) {
        long now = System.currentTimeMillis();
        Long lastMessage = blockMessageCooldown.get(player.getUniqueId());

        if (lastMessage == null || now - lastMessage > 3000) { // 3 secondes de cooldown
            blockMessageCooldown.put(player.getUniqueId(), now);

            JourneyGate gate = JourneyGate.getZoneGate(zoneId);
            if (gate != null) {
                journeyManager.sendBlockedMessage(player, gate);
            }
        }
    }

    // ==================== TRACKING DES KILLS ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieKill(ZombieDeathEvent event) {
        Player killer = event.getKiller();
        if (killer == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayer(killer);
        if (data == null) return;

        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep == null) return;

        // Mettre à jour selon le type d'étape
        switch (currentStep.getType()) {
            case ZOMBIE_KILLS -> {
                int progress = journeyManager.getStepProgress(killer, currentStep);
                journeyManager.updateProgress(killer, JourneyStep.StepType.ZOMBIE_KILLS, progress + 1);
            }
            case CLASS_KILLS -> {
                // Vérifier que le joueur a une classe
                if (plugin.getClassManager().getClassData(killer).hasClass()) {
                    int progress = journeyManager.getStepProgress(killer, currentStep);
                    journeyManager.updateProgress(killer, JourneyStep.StepType.CLASS_KILLS, progress + 1);
                }
            }
            case TOTAL_KILLS -> {
                long totalKills = data.getTotalKills();
                journeyManager.updateProgress(killer, JourneyStep.StepType.TOTAL_KILLS, (int) totalKills);
            }
            case ZONE_KILLS -> {
                // Kills dans une zone spécifique (zone 4+)
                int currentZone = data.getCurrentZone().get();
                if (currentZone >= 4) {
                    int progress = journeyManager.getStepProgress(killer, currentStep);
                    journeyManager.updateProgress(killer, JourneyStep.StepType.ZONE_KILLS, progress + 1);
                }
            }
            case ADVANCED_ZONE_KILLS -> {
                // Kills en zone 8+
                int currentZone = data.getCurrentZone().get();
                if (currentZone >= 8) {
                    int progress = journeyManager.getStepProgress(killer, currentStep);
                    journeyManager.updateProgress(killer, JourneyStep.StepType.ADVANCED_ZONE_KILLS, progress + 1);
                }
            }
            case ELITE_KILLS -> {
                if (event.isElite()) {
                    long eliteKills = data.getEliteKills().get();
                    journeyManager.updateProgress(killer, JourneyStep.StepType.ELITE_KILLS, (int) eliteKills);
                }
            }
            case BOSS_KILLS -> {
                if (event.isBoss()) {
                    long bossKills = data.getBossKills().get();
                    journeyManager.updateProgress(killer, JourneyStep.StepType.BOSS_KILLS, (int) bossKills);
                }
            }
            case ADVANCED_BOSS_KILL -> {
                if (event.isBoss()) {
                    int currentZone = data.getCurrentZone().get();
                    if (currentZone >= 8) {
                        int progress = journeyManager.getStepProgress(killer, currentStep);
                        journeyManager.updateProgress(killer, JourneyStep.StepType.ADVANCED_BOSS_KILL, progress + 1);
                    }
                }
            }
            case KILL_STREAK -> {
                int streak = data.getKillStreak().get();
                journeyManager.updateProgress(killer, JourneyStep.StepType.KILL_STREAK, streak);
            }
            case EVENT_KILLS -> {
                // Vérifier si un événement est en cours
                var eventManager = plugin.getEventManager();
                if (eventManager != null && eventManager.getCurrentEvent() != null && eventManager.getCurrentEvent().isActive()) {
                    int progress = journeyManager.getStepProgress(killer, currentStep);
                    journeyManager.updateProgress(killer, JourneyStep.StepType.EVENT_KILLS, progress + 1);
                }
            }
            case KILL_PATIENT_ZERO -> {
                if (event.getZombieType() != null && event.getZombieType().equalsIgnoreCase("PATIENT_ZERO")) {
                    journeyManager.updateProgress(killer, JourneyStep.StepType.KILL_PATIENT_ZERO, 1);
                }
            }
            default -> {}
        }
    }

    // ==================== TRACKING DES KILLS D'ANIMAUX ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimalKill(EntityDeathEvent event) {
        // Vérifier que c'est un animal passif
        if (!(event.getEntity() instanceof Animals)) {
            return;
        }

        // Vérifier que le tueur est un joueur
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep == null) return;

        // Vérifier si l'étape actuelle est de type PASSIVE_ANIMAL_KILLS
        if (currentStep.getType() == JourneyStep.StepType.PASSIVE_ANIMAL_KILLS) {
            int progress = journeyManager.getStepProgress(killer, currentStep);
            journeyManager.updateProgress(killer, JourneyStep.StepType.PASSIVE_ANIMAL_KILLS, progress + 1);
        }
    }

    // ==================== TRACKING DES ZONES ====================

    private void updateZoneProgress(Player player, int newZoneId) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        switch (currentStep.getType()) {
            case REACH_ZONE -> {
                journeyManager.updateProgress(player, JourneyStep.StepType.REACH_ZONE, newZoneId);
            }
            case ZONE_PROGRESS -> {
                // L'exploration par chunks est gérée dans trackChunkExploration()
                // Cette méthode est appelée lors du changement de zone
                var zone = plugin.getZoneManager().getZoneById(newZoneId);
                if (zone != null) {
                    int chunkX = player.getLocation().getBlockX() >> 4;
                    int chunkZ = player.getLocation().getBlockZ() >> 4;
                    trackChunkExploration(player, chunkX, chunkZ, zone);
                }
            }
            default -> {}
        }
    }

    // ==================== AUTRES TRACKERS (à appeler depuis les autres managers) ====================

    /**
     * Appelé quand le joueur monte de niveau
     */
    public void onLevelUp(Player player, int newLevel) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.LEVEL) {
            journeyManager.updateProgress(player, JourneyStep.StepType.LEVEL, newLevel);
        }

        // Aussi tracker PRESTIGE_LEVEL si le joueur a fait un prestige
        if (currentStep.getType() == JourneyStep.StepType.PRESTIGE_LEVEL) {
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
            if (data != null && data.getPrestigeLevel() > 0) {
                journeyManager.updateProgress(player, JourneyStep.StepType.PRESTIGE_LEVEL, newLevel);
            }
        }
    }

    /**
     * Appelé quand le joueur monte de niveau de classe
     */
    public void onClassLevelUp(Player player, int newClassLevel) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.CLASS_LEVEL) {
            journeyManager.updateProgress(player, JourneyStep.StepType.CLASS_LEVEL, newClassLevel);
        }
    }

    /**
     * Appelé quand le joueur sélectionne une classe
     */
    public void onClassSelect(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.SELECT_CLASS) {
            journeyManager.updateProgress(player, JourneyStep.StepType.SELECT_CLASS, 1);
        }

        // Étape en 2 temps: classe + voie (progress 0 -> 1)
        if (currentStep.getType() == JourneyStep.StepType.SELECT_CLASS_AND_BRANCH) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            if (progress < 1) {
                journeyManager.updateProgress(player, JourneyStep.StepType.SELECT_CLASS_AND_BRANCH, 1);
            }
        }
    }

    /**
     * Appelé quand le joueur sélectionne une voie/branche
     */
    public void onBranchSelect(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        // Étape en 2 temps: classe + voie (progress 1 -> 2)
        if (currentStep.getType() == JourneyStep.StepType.SELECT_CLASS_AND_BRANCH) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            if (progress >= 1 && progress < 2) {
                journeyManager.updateProgress(player, JourneyStep.StepType.SELECT_CLASS_AND_BRANCH, 2);
            }
        }
    }

    /**
     * Appelé quand le joueur utilise un trait de classe
     */
    public void onUseClassTrait(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.USE_CLASS_TRAIT) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            journeyManager.updateProgress(player, JourneyStep.StepType.USE_CLASS_TRAIT, progress + 1);
        }
    }

    /**
     * Appelé quand le joueur débloque un talent
     */
    public void onUnlockTalent(Player player, int totalTalents) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.UNLOCK_TALENT) {
            journeyManager.updateProgress(player, JourneyStep.StepType.UNLOCK_TALENT, totalTalents);
        }

        // Aussi vérifier TALENTS_UNLOCKED (nombre de tiers avec au moins 1 talent)
        if (currentStep.getType() == JourneyStep.StepType.TALENTS_UNLOCKED) {
            // Compter le nombre de tiers différents
            var classData = plugin.getClassManager().getClassData(player);
            if (classData != null) {
                int tiersWithTalent = (int) classData.getAllSelectedTalents().size();
                journeyManager.updateProgress(player, JourneyStep.StepType.TALENTS_UNLOCKED, tiersWithTalent);
            }
        }
    }

    /**
     * Appelé quand le joueur utilise un talent
     */
    public void onUseTalent(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.USE_TALENT) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            journeyManager.updateProgress(player, JourneyStep.StepType.USE_TALENT, progress + 1);
        }
    }

    /**
     * Appelé quand le joueur débloque un skill passif
     */
    public void onUnlockSkill(Player player, int totalSkills) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.UNLOCK_SKILLS) {
            journeyManager.updateProgress(player, JourneyStep.StepType.UNLOCK_SKILLS, totalSkills);
        }
    }

    /**
     * Appelé quand le joueur atteint un tier de skill
     */
    public void onSkillTierReached(Player player, int tier) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.SKILL_TIER) {
            journeyManager.updateProgress(player, JourneyStep.StepType.SKILL_TIER, tier);
        }
    }

    /**
     * Appelé quand le joueur survie à une Blood Moon
     */
    public void onSurviveBloodMoon(Player player, int totalSurvived) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.SURVIVE_BLOOD_MOON) {
            journeyManager.updateProgress(player, JourneyStep.StepType.SURVIVE_BLOOD_MOON, totalSurvived);
        }
    }

    /**
     * Appelé quand le joueur participe à un événement
     */
    public void onEventParticipation(Player player, int totalEvents) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.PARTICIPATE_EVENT) {
            journeyManager.updateProgress(player, JourneyStep.StepType.PARTICIPATE_EVENT, totalEvents);
        }
    }

    /**
     * Appelé quand le joueur fait un prestige
     */
    public void onPrestige(Player player, int prestigeLevel) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.PRESTIGE) {
            journeyManager.updateProgress(player, JourneyStep.StepType.PRESTIGE, prestigeLevel);
        }
    }

    /**
     * Appelé quand le joueur débloque un achievement
     */
    public void onAchievementUnlock(Player player, int totalAchievements) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.ACHIEVEMENTS) {
            journeyManager.updateProgress(player, JourneyStep.StepType.ACHIEVEMENTS, totalAchievements);
        }
    }

    /**
     * Appelé quand le joueur recycle des items
     */
    public void onRecycleItems(Player player, int totalRecycled) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        if (currentStep.getType() == JourneyStep.StepType.RECYCLE_ITEMS) {
            journeyManager.updateProgress(player, JourneyStep.StepType.RECYCLE_ITEMS, totalRecycled);
        }
    }

    // ==================== TRACKING DU TEMPS DE SURVIE ====================

    /**
     * Lance la tâche périodique de tracking du temps de survie en zone
     */
    private void startSurvivalTimeTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateSurvivalTimeProgress(player);
                }
            }
        }.runTaskTimer(plugin, 200L, 20L); // Toutes les secondes
    }

    /**
     * Met à jour la progression de temps de survie pour un joueur
     */
    private void updateSurvivalTimeProgress(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return;

        java.util.UUID uuid = player.getUniqueId();

        // Obtenir la zone actuelle
        var zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        if (zone == null) return;
        int currentZoneId = zone.getId();

        // Gérer le changement de zone
        Integer previousZone = lastZoneId.get(uuid);
        if (previousZone == null || previousZone != currentZoneId) {
            // Nouvelle zone - reset le timer
            zoneEntryTime.put(uuid, System.currentTimeMillis());
            lastZoneId.put(uuid, currentZoneId);
        }

        // Calculer le temps passé dans la zone
        Long entryTime = zoneEntryTime.get(uuid);
        if (entryTime == null) return;

        int secondsInZone = (int) ((System.currentTimeMillis() - entryTime) / 1000);

        // Mettre à jour selon le type d'étape
        if (currentStep.getType() == JourneyStep.StepType.SURVIVE_ZONE_TIME) {
            // STEP_3_2: Survie 5 minutes en Zone 2
            if (currentZoneId >= 2) {
                journeyManager.updateProgress(player, JourneyStep.StepType.SURVIVE_ZONE_TIME, secondsInZone);
            }
        } else if (currentStep.getType() == JourneyStep.StepType.SURVIVE_ENVIRONMENT) {
            // STEP_10_2: Survie aux effets environnementaux 10min en zone 8+
            if (currentZoneId >= 8) {
                journeyManager.updateProgress(player, JourneyStep.StepType.SURVIVE_ENVIRONMENT, secondsInZone);
            }
        }
    }

    /**
     * Reset le timer de survie d'un joueur (appelé lors de la mort)
     */
    public void onPlayerDeath(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        zoneEntryTime.remove(uuid);
        lastZoneId.remove(uuid);
    }

    // ==================== AFFICHAGE BOSSBAR ====================

    /**
     * Lance la tâche périodique de mise à jour de la BossBar
     */
    private void startBossBarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    journeyManager.createOrUpdateBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 100L, 20L); // Toutes les secondes
    }

    private void showJourneyWelcome(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        JourneyChapter currentChapter = journeyManager.getCurrentChapter(player);
        double overallProgress = journeyManager.getOverallProgress(player);

        player.sendMessage("");
        player.sendMessage("§8§m                                                ");
        player.sendMessage("  §e§lJOURNAL DU SURVIVANT");
        player.sendMessage("");
        player.sendMessage("  §7Progression: §e" + String.format("%.1f", overallProgress) + "%");
        player.sendMessage("  §7Chapitre: " + currentChapter.getFormattedTitle());
        if (currentStep != null) {
            player.sendMessage("  §7Étape: §f" + currentStep.getName());
        }
        player.sendMessage("");
        player.sendMessage("  §7Tape §e/journey §7pour voir ton journal!");
        player.sendMessage("§8§m                                                ");
        player.sendMessage("");
    }
}
