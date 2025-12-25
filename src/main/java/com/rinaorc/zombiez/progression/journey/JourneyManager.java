package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire principal du système de Parcours (Journey)
 *
 * Responsabilités:
 * - Tracker la progression des joueurs dans les étapes
 * - Vérifier les conditions de complétion
 * - Gérer les blocages (gates) de zones et fonctionnalités
 * - Appliquer les récompenses
 * - Envoyer les notifications visuelles et sonores
 *
 * IMPORTANT: Ce système BLOQUE réellement la progression
 * Le joueur ne peut pas accéder aux zones/fonctionnalités verrouillées
 */
public class JourneyManager {

    private final ZombieZPlugin plugin;

    // Cache des gates débloquées par joueur pour accès rapide
    private final Map<UUID, Set<JourneyGate>> unlockedGatesCache = new ConcurrentHashMap<>();

    // Cache des chapitres complétés par joueur
    private final Map<UUID, Set<Integer>> completedChaptersCache = new ConcurrentHashMap<>();

    // Progression actuelle par joueur (étape_id -> progress)
    private final Map<UUID, Map<String, Integer>> stepProgressCache = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, JourneyStep> currentStepCache = new ConcurrentHashMap<>();

    public JourneyManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== VÉRIFICATION DES GATES (BLOCAGE) ====================

    /**
     * Vérifie si un joueur peut accéder à une zone
     * RETOURNE FALSE SI LA ZONE EST BLOQUÉE
     *
     * @param player Le joueur
     * @param zoneId L'ID de la zone
     * @return true si le joueur peut entrer, false sinon
     */
    public boolean canAccessZone(Player player, int zoneId) {
        // Zone 1 toujours accessible
        if (zoneId <= 1) return true;

        // Obtenir la gate correspondante à cette zone
        JourneyGate gate = JourneyGate.getZoneGate(zoneId);
        if (gate == null) return true; // Pas de gate = accessible

        return hasUnlockedGate(player, gate);
    }

    /**
     * Vérifie si un joueur peut accéder à la sélection de classe
     */
    public boolean canAccessClassSelection(Player player) {
        return hasUnlockedGate(player, JourneyGate.CLASS_SELECTION);
    }

    /**
     * Vérifie si un joueur peut accéder aux talents d'un tier spécifique
     */
    public boolean canAccessTalentTier(Player player, int tier) {
        JourneyGate gate = JourneyGate.getTalentGate(tier);
        if (gate == null) return true;
        return hasUnlockedGate(player, gate);
    }

    /**
     * Vérifie si un joueur peut accéder au skill tree
     */
    public boolean canAccessSkillTree(Player player) {
        return hasUnlockedGate(player, JourneyGate.SKILL_TREE);
    }

    /**
     * Vérifie si un joueur peut accéder aux missions quotidiennes
     */
    public boolean canAccessDailyMissions(Player player) {
        return hasUnlockedGate(player, JourneyGate.DAILY_MISSIONS);
    }

    /**
     * Vérifie si un joueur peut accéder aux missions hebdomadaires
     */
    public boolean canAccessWeeklyMissions(Player player) {
        return hasUnlockedGate(player, JourneyGate.WEEKLY_MISSIONS);
    }

    /**
     * Vérifie si un joueur peut accéder au Battle Pass
     */
    public boolean canAccessBattlePass(Player player) {
        return hasUnlockedGate(player, JourneyGate.BATTLE_PASS);
    }

    /**
     * Vérifie si un joueur peut utiliser le prestige
     */
    public boolean canAccessPrestige(Player player) {
        return hasUnlockedGate(player, JourneyGate.PRESTIGE);
    }

    /**
     * Vérifie si un joueur peut faire des échanges
     */
    public boolean canAccessTrading(Player player) {
        return hasUnlockedGate(player, JourneyGate.TRADING);
    }

    /**
     * Vérifie si un joueur a débloqué une gate spécifique
     */
    public boolean hasUnlockedGate(Player player, JourneyGate gate) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cache
        Set<JourneyGate> unlocked = unlockedGatesCache.get(uuid);
        if (unlocked != null && unlocked.contains(gate)) {
            return true;
        }

        // Charger depuis PlayerData si pas en cache
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            return data.hasJourneyGate(gate.name());
        }

        return false;
    }

    /**
     * Envoie un message de blocage au joueur
     */
    public void sendBlockedMessage(Player player, JourneyGate gate) {
        player.sendMessage("");
        player.sendMessage("§c§l⛔ ACCÈS BLOQUÉ ⛔");
        player.sendMessage("");
        player.sendMessage("§7" + gate.getDisplayName());
        player.sendMessage("§e➤ " + gate.getRequirement());
        player.sendMessage("");
        player.sendMessage("§7Consulte ton §eParcours §7(/journey) pour progresser!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

    /**
     * Repousse physiquement le joueur hors d'une zone bloquée
     */
    public void pushBackFromZone(Player player, int blockedZoneId) {
        // Téléporter le joueur à la bordure de la zone précédente
        Location loc = player.getLocation();

        // Obtenir la zone du plugin
        var zone = plugin.getZoneManager().getZoneById(blockedZoneId);
        if (zone == null) return;

        // Calculer la position de recul (vers le sud, Z+)
        int safeZ = zone.getMaxZ() + 5; // 5 blocs après la limite
        Location safeLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), safeZ, loc.getYaw(), loc.getPitch());

        // Trouver une position sûre en Y
        safeLoc.setY(loc.getWorld().getHighestBlockYAt(safeLoc) + 1);

        player.teleport(safeLoc);

        // Effets visuels
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.5);
    }

    // ==================== PROGRESSION DES ÉTAPES ====================

    /**
     * Obtient le chapitre actuel d'un joueur
     */
    public JourneyChapter getCurrentChapter(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return JourneyChapter.CHAPTER_1;
        return JourneyChapter.getById(data.getCurrentJourneyChapter());
    }

    /**
     * Obtient l'étape actuelle d'un joueur
     */
    public JourneyStep getCurrentStep(Player player) {
        // Vérifier le cache
        JourneyStep cached = currentStepCache.get(player.getUniqueId());
        if (cached != null) return cached;

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return JourneyStep.STEP_1_1;

        JourneyChapter chapter = JourneyChapter.getById(data.getCurrentJourneyChapter());
        int stepNum = data.getCurrentJourneyStep();

        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        if (stepNum > 0 && stepNum <= steps.size()) {
            JourneyStep step = steps.get(stepNum - 1);
            currentStepCache.put(player.getUniqueId(), step);
            return step;
        }

        return JourneyStep.getFirstStep(chapter);
    }

    /**
     * Obtient la progression d'une étape
     */
    public int getStepProgress(Player player, JourneyStep step) {
        UUID uuid = player.getUniqueId();
        String stepId = step.getId();

        // Vérifier le cache
        Map<String, Integer> progress = stepProgressCache.get(uuid);
        if (progress != null && progress.containsKey(stepId)) {
            return progress.get(stepId);
        }

        // Charger depuis PlayerData
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            return data.getJourneyStepProgress(stepId);
        }

        return 0;
    }

    /**
     * Met à jour la progression d'une étape
     * C'est cette méthode qui est appelée par le JourneyListener
     */
    public void updateProgress(Player player, JourneyStep.StepType type, int newValue) {
        JourneyStep currentStep = getCurrentStep(player);
        if (currentStep == null) return;

        // Vérifier si le type correspond à l'étape actuelle
        if (currentStep.getType() != type) return;

        UUID uuid = player.getUniqueId();
        String stepId = currentStep.getId();

        // Mettre à jour le cache
        stepProgressCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(stepId, newValue);

        // Mettre à jour PlayerData
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            data.setJourneyStepProgress(stepId, newValue);
        }

        // Vérifier la complétion
        if (currentStep.isCompleted(newValue)) {
            completeStep(player, currentStep);
        }
    }

    /**
     * Incrémente la progression d'une étape
     */
    public void incrementProgress(Player player, JourneyStep.StepType type, int amount) {
        JourneyStep currentStep = getCurrentStep(player);
        if (currentStep == null) return;

        // Vérifier si le type correspond à l'étape actuelle
        if (currentStep.getType() != type) return;

        int current = getStepProgress(player, currentStep);
        updateProgress(player, type, current + amount);
    }

    /**
     * Complète une étape et passe à la suivante
     */
    private void completeStep(Player player, JourneyStep step) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Marquer comme complétée
        data.addCompletedJourneyStep(step.getId());

        // Donner les récompenses
        plugin.getEconomyManager().addPoints(player, step.getPointReward());
        plugin.getEconomyManager().addGems(player, step.getGemReward());

        // Notification
        sendStepCompletedNotification(player, step);

        // Passer à l'étape suivante
        JourneyStep nextStep = step.getNextInChapter();
        if (nextStep != null) {
            // Prochaine étape dans le même chapitre
            data.setCurrentJourneyStep(nextStep.getStepNumber());
            currentStepCache.put(player.getUniqueId(), nextStep);

            // Afficher la prochaine étape
            sendNextStepNotification(player, nextStep);
        } else {
            // Fin du chapitre !
            completeChapter(player, step.getChapter());
        }
    }

    /**
     * Complète un chapitre et débloque les gates associées
     */
    private void completeChapter(Player player, JourneyChapter chapter) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        UUID uuid = player.getUniqueId();

        // Marquer le chapitre comme complété
        data.addCompletedJourneyChapter(chapter.getId());
        completedChaptersCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
            .add(chapter.getId());

        // Débloquer les gates
        for (JourneyGate gate : chapter.getUnlocks()) {
            data.addJourneyGate(gate.name());
            unlockedGatesCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                .add(gate);
        }

        // Récompenses du chapitre
        plugin.getEconomyManager().addPoints(player, chapter.getBonusPoints());
        plugin.getEconomyManager().addGems(player, chapter.getBonusGems());

        // Récompenses spéciales selon le chapitre
        applyChapterBonusRewards(player, chapter);

        // Notification spectaculaire
        sendChapterCompletedNotification(player, chapter);

        // Passer au chapitre suivant
        JourneyChapter nextChapter = chapter.getNext();
        if (nextChapter != null) {
            data.setCurrentJourneyChapter(nextChapter.getId());
            data.setCurrentJourneyStep(1);

            JourneyStep firstStep = JourneyStep.getFirstStep(nextChapter);
            if (firstStep != null) {
                currentStepCache.put(uuid, firstStep);
            }

            // Notification du nouveau chapitre
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendNewChapterNotification(player, nextChapter);
                }
            }.runTaskLater(plugin, 80L); // 4 secondes après
        } else {
            // FIN DU PARCOURS !
            sendJourneyCompletedNotification(player);
        }
    }

    /**
     * Applique les récompenses bonus spéciales du chapitre
     */
    private void applyChapterBonusRewards(Player player, JourneyChapter chapter) {
        switch (chapter) {
            case CHAPTER_5 -> {
                // +3 Points de Skill gratuits
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
                if (data != null) {
                    data.addBonusSkillPoints(3);
                }
            }
            case CHAPTER_8 -> {
                // Titre "Chasseur d'Élites"
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
                if (data != null) {
                    data.addTitle("elite_hunter_journey");
                }
            }
            case CHAPTER_11 -> {
                // Titre "Titan Slayer"
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
                if (data != null) {
                    data.addTitle("titan_slayer_journey");
                }
            }
            case CHAPTER_12 -> {
                // Titre "Légende Vivante" + Aura cosmétique
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
                if (data != null) {
                    data.addTitle("living_legend_journey");
                    data.addCosmetic("aura_legend");
                }
            }
            default -> {}
        }
    }

    // ==================== NOTIFICATIONS ====================

    private void sendStepCompletedNotification(Player player, JourneyStep step) {
        player.sendTitle(
            "§a✓ ÉTAPE COMPLÉTÉE",
            step.getFormattedName(),
            10, 50, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l✓ ÉTAPE COMPLÉTÉE!");
        player.sendMessage("  §7" + step.getName());
        player.sendMessage("");
        player.sendMessage("  §e+" + step.getPointReward() + " Points §8| §d+" + step.getGemReward() + " Gems");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);

        // Particules
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5);
    }

    private void sendNextStepNotification(Player player, JourneyStep step) {
        player.sendMessage("");
        player.sendMessage("§e▶ Prochaine étape: §f" + step.getName());
        player.sendMessage("§7  " + step.getDescription());
        player.sendMessage("");
    }

    private void sendChapterCompletedNotification(Player player, JourneyChapter chapter) {
        // Title épique
        player.sendTitle(
            "§6§l✦ CHAPITRE COMPLÉTÉ ✦",
            chapter.getFormattedTitle(),
            20, 80, 30
        );

        // Message détaillé
        player.sendMessage("");
        player.sendMessage("§8§m                                                    ");
        player.sendMessage("");
        player.sendMessage("    §6§l✦ CHAPITRE " + chapter.getId() + " COMPLÉTÉ! ✦");
        player.sendMessage("    " + chapter.getColoredName());
        player.sendMessage("");
        player.sendMessage("    §e§l+" + formatNumber(chapter.getBonusPoints()) + " Points");
        player.sendMessage("    §d§l+" + chapter.getBonusGems() + " Gems");
        player.sendMessage("    " + chapter.getBonusReward());
        player.sendMessage("");

        // Afficher les déblocages
        if (chapter.getUnlocks().length > 0) {
            player.sendMessage("    §a§l🔓 DÉBLOQUÉ:");
            for (JourneyGate gate : chapter.getUnlocks()) {
                player.sendMessage("    §a  • " + gate.getDisplayName());
            }
            player.sendMessage("");
        }

        player.sendMessage("§8§m                                                    ");
        player.sendMessage("");

        // Sons épiques
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);

        // Feu d'artifice
        spawnChapterFireworks(player);

        // Broadcast aux joueurs proches
        broadcastChapterCompletion(player, chapter);
    }

    private void sendNewChapterNotification(Player player, JourneyChapter chapter) {
        player.sendTitle(
            chapter.getPhaseName(),
            "§7Chapitre " + chapter.getId() + ": " + chapter.getColoredName(),
            20, 60, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§l▶ NOUVEAU CHAPITRE");
        player.sendMessage("");
        player.sendMessage("  " + chapter.getFormattedTitle());
        player.sendMessage("  §7" + chapter.getDescription());
        player.sendMessage("");

        JourneyStep firstStep = JourneyStep.getFirstStep(chapter);
        if (firstStep != null) {
            player.sendMessage("  §e➤ Première étape: §f" + firstStep.getName());
        }

        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    private void sendJourneyCompletedNotification(Player player) {
        // Title ultra-épique
        player.sendTitle(
            "§6§l✦ LÉGENDE VIVANTE ✦",
            "§eTu as complété le Parcours du Survivant!",
            20, 100, 30
        );

        player.sendMessage("");
        player.sendMessage("§6§l§m                                                        ");
        player.sendMessage("");
        player.sendMessage("          §6§l✦ ✦ ✦ PARCOURS COMPLÉTÉ ✦ ✦ ✦");
        player.sendMessage("");
        player.sendMessage("          §eTu es désormais une §6§lLÉGENDE VIVANTE§e!");
        player.sendMessage("");
        player.sendMessage("          §7Tu as prouvé ta valeur à travers 12 chapitres");
        player.sendMessage("          §7et maîtrisé tous les aspects de ZombieZ.");
        player.sendMessage("");
        player.sendMessage("          §6Récompenses finales:");
        player.sendMessage("          §e• Titre: §6§l✦ Légende Vivante ✦");
        player.sendMessage("          §e• Aura cosmétique exclusive");
        player.sendMessage("          §e• Set légendaire");
        player.sendMessage("");
        player.sendMessage("§6§l§m                                                        ");
        player.sendMessage("");

        // Sons ultra-épiques
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);

        // Multiple feux d'artifice
        for (int i = 0; i < 5; i++) {
            int delay = i * 10;
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnLegendFirework(player);
                }
            }.runTaskLater(plugin, delay);
        }

        // Broadcast serveur
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage("§6§l✦ §e" + player.getName() + " §7est devenu une §6§lLÉGENDE VIVANTE§7! ✦");
            p.sendMessage("§7  Il a complété tout le Parcours du Survivant!");
            p.sendMessage("");

            if (p != player) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1f);
            }
        }
    }

    private void spawnChapterFireworks(Player player) {
        Location loc = player.getLocation();

        Firework fw = loc.getWorld().spawn(loc.clone().add(0, 1, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(Color.YELLOW, Color.ORANGE)
            .withFade(Color.WHITE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                fw.detonate();
            }
        }.runTaskLater(plugin, 2L);
    }

    private void spawnLegendFirework(Player player) {
        Location loc = player.getLocation().add(
            (Math.random() - 0.5) * 4,
            1,
            (Math.random() - 0.5) * 4
        );

        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.STAR)
            .withColor(Color.PURPLE, Color.FUCHSIA, Color.YELLOW)
            .withFade(Color.WHITE, Color.ORANGE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                fw.detonate();
            }
        }.runTaskLater(plugin, 2L);
    }

    private void broadcastChapterCompletion(Player player, JourneyChapter chapter) {
        String message = "§e" + player.getName() + " §7a complété le " + chapter.getFormattedTitle() + "§7!";

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player && p.getLocation().distance(player.getLocation()) < 100) {
                p.sendMessage(message);
            }
        }
    }

    // ==================== UTILITAIRES ====================

    /**
     * Charge les données de parcours d'un joueur
     */
    public void loadPlayerJourney(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Charger les gates débloquées
        Set<JourneyGate> gates = ConcurrentHashMap.newKeySet();
        for (String gateName : data.getUnlockedJourneyGates()) {
            try {
                gates.add(JourneyGate.valueOf(gateName));
            } catch (IllegalArgumentException ignored) {}
        }
        unlockedGatesCache.put(uuid, gates);

        // Charger les chapitres complétés
        Set<Integer> chapters = ConcurrentHashMap.newKeySet();
        chapters.addAll(data.getCompletedJourneyChapters());
        completedChaptersCache.put(uuid, chapters);

        // Charger l'étape actuelle
        JourneyChapter chapter = JourneyChapter.getById(data.getCurrentJourneyChapter());
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int stepNum = data.getCurrentJourneyStep();
        if (stepNum > 0 && stepNum <= steps.size()) {
            currentStepCache.put(uuid, steps.get(stepNum - 1));
        }
    }

    /**
     * Nettoie le cache d'un joueur
     */
    public void unloadPlayer(UUID uuid) {
        unlockedGatesCache.remove(uuid);
        completedChaptersCache.remove(uuid);
        stepProgressCache.remove(uuid);
        currentStepCache.remove(uuid);
    }

    /**
     * Obtient le pourcentage de progression global du parcours
     */
    public double getOverallProgress(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;

        int completedSteps = data.getCompletedJourneySteps().size();
        int totalSteps = JourneyStep.values().length;

        return (double) completedSteps / totalSteps * 100;
    }

    /**
     * Obtient le nombre de chapitres complétés
     */
    public int getCompletedChaptersCount(Player player) {
        Set<Integer> completed = completedChaptersCache.get(player.getUniqueId());
        return completed != null ? completed.size() : 0;
    }

    /**
     * Vérifie si un chapitre est complété
     */
    public boolean isChapterCompleted(Player player, JourneyChapter chapter) {
        Set<Integer> completed = completedChaptersCache.get(player.getUniqueId());
        return completed != null && completed.contains(chapter.getId());
    }

    /**
     * Formate un nombre
     */
    private String formatNumber(int value) {
        if (value >= 1000) {
            return String.format("%.1fK", value / 1000.0);
        }
        return String.valueOf(value);
    }

    /**
     * Affiche la barre de progression dans l'ActionBar
     */
    public void showProgressActionBar(Player player) {
        JourneyStep step = getCurrentStep(player);
        if (step == null) return;

        int progress = getStepProgress(player, step);
        String progressText = step.getProgressText(progress);
        double percent = step.getProgressPercent(progress);

        // Barre de progression visuelle
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (percent / 10);
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a■");
            } else {
                bar.append("§7□");
            }
        }
        bar.append("§8]");

        String message = String.format("§7Ch.%d §8| %s §e%s §8| %s",
            step.getChapter().getId(),
            bar,
            step.getName(),
            progressText
        );

        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }
}
