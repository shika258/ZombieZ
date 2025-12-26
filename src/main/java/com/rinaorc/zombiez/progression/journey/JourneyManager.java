package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import com.rinaorc.zombiez.mobs.PassiveMobManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

    // Gestionnaire des coffres mystères
    @Getter
    private final MysteryChestManager mysteryChestManager;

    // Cache des gates débloquées par joueur pour accès rapide
    private final Map<UUID, Set<JourneyGate>> unlockedGatesCache = new ConcurrentHashMap<>();

    // Cache des chapitres complétés par joueur
    private final Map<UUID, Set<Integer>> completedChaptersCache = new ConcurrentHashMap<>();

    // Progression actuelle par joueur (étape_id -> progress)
    private final Map<UUID, Map<String, Integer>> stepProgressCache = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, JourneyStep> currentStepCache = new ConcurrentHashMap<>();

    // BossBar de progression par joueur
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();

    public JourneyManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.mysteryChestManager = new MysteryChestManager(plugin, this);
    }

    /**
     * Démarre les systèmes du Journey (coffres mystères, etc.)
     * Appelé après l'initialisation complète du plugin
     */
    public void start() {
        mysteryChestManager.start();
    }

    /**
     * Arrête les systèmes du Journey proprement
     */
    public void shutdown() {
        mysteryChestManager.shutdown();
    }

    // ==================== SYSTÈME DE BOSSBAR ====================

    /**
     * Crée ou met à jour la BossBar de progression d'un joueur
     */
    public void createOrUpdateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        JourneyStep step = getCurrentStep(player);

        BossBar bossBar = playerBossBars.get(uuid);

        if (bossBar == null) {
            // Créer une nouvelle BossBar
            bossBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
            playerBossBars.put(uuid, bossBar);
        }

        // Si parcours complété
        if (step == null) {
            bossBar.setTitle("§6✦ §e§lLÉGENDE VIVANTE §6✦ §7Journal complété!");
            bossBar.setProgress(1.0);
            bossBar.setColor(BarColor.PURPLE);
            bossBar.setStyle(BarStyle.SOLID);
            return;
        }

        // Calculer la progression
        int progress = getStepProgress(player, step);
        double percent = step.getProgressPercent(progress) / 100.0;
        String progressText = step.getProgressText(progress);

        // Vérification automatique de complétion pour les objectifs basés sur l'état (LEVEL, CLASS_LEVEL, etc.)
        // Cette vérification est cruciale car ces objectifs peuvent être atteints avant d'arriver à l'étape
        if (step.isCompleted(progress) && getCurrentStep(player) == step) {
            checkCurrentStepCompletion(player, step);
            // Note: la complétion se fera en async, la prochaine mise à jour de la BossBar reflètera le changement
        }

        // Couleur selon la phase
        BarColor color = switch (step.getChapter().getPhase()) {
            case 1 -> BarColor.GREEN;
            case 2 -> BarColor.YELLOW;
            case 3 -> BarColor.RED;
            case 4 -> BarColor.PURPLE;
            default -> BarColor.WHITE;
        };

        // Icône selon la phase
        String phaseIcon = switch (step.getChapter().getPhase()) {
            case 1 -> "§a⚔";
            case 2 -> "§e⚔";
            case 3 -> "§c⚔";
            case 4 -> "§5⚔";
            default -> "§7⚔";
        };

        // Format: ⚔ Ch.2 | Choisis ta classe | 0/1
        String title = String.format("%s §7Ch.%d §8| §f%s §8| §e%s",
            phaseIcon,
            step.getChapter().getId(),
            truncate(step.getName(), 25),
            progressText
        );

        bossBar.setTitle(title);
        bossBar.setProgress(Math.min(1.0, Math.max(0.0, percent)));
        bossBar.setColor(color);
        bossBar.setStyle(BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    /**
     * Supprime la BossBar d'un joueur
     */
    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Cache temporairement la BossBar (ex: pendant un événement)
     */
    public void hideBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
    }

    /**
     * Réaffiche la BossBar
     */
    public void showBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setVisible(true);
        }
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
        player.sendMessage("§7Consulte ton §eJournal §7(/journey) pour progresser!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

    /**
     * Repousse physiquement le joueur hors d'une zone bloquée
     */
    public void pushBackFromZone(Player player, int blockedZoneId) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Obtenir la zone du plugin
        var zoneManager = plugin.getZoneManager();
        if (zoneManager == null) return;

        var zone = zoneManager.getZoneById(blockedZoneId);
        if (zone == null) return;

        // Calculer la position de recul (vers le sud, Z+)
        int safeZ = zone.getMaxZ() + 5; // 5 blocs après la limite
        Location safeLoc = new Location(world, loc.getX(), loc.getY(), safeZ, loc.getYaw(), loc.getPitch());

        // Trouver une position sûre en Y
        int highestY = world.getHighestBlockYAt(safeLoc);
        safeLoc.setY(Math.max(highestY + 1, world.getMinHeight() + 1));

        player.teleport(safeLoc);

        // Effets visuels
        world.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.5);
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
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;

        // Pour les types basés sur l'état actuel du joueur, retourner la valeur réelle
        return switch (step.getType()) {
            case LEVEL -> data.getLevel().get();
            case CLASS_LEVEL -> {
                ClassData classData = plugin.getClassManager().getClassData(player);
                yield classData != null ? classData.getClassLevel().get() : 0;
            }
            case PRESTIGE_LEVEL -> data.getLevel().get(); // Niveau après prestige
            case PRESTIGE -> data.getPrestigeLevel();
            case ZONE_PROGRESS -> {
                // Exploration par chunks - déterminer la zone ciblée par l'étape
                int targetZone = getTargetZoneForStep(step);
                if (targetZone > 0) {
                    var zone = plugin.getZoneManager().getZoneById(targetZone);
                    if (zone != null) {
                        int exploredCount = data.getExploredChunkCount(targetZone);
                        yield zone.getExplorationPercent(exploredCount);
                    }
                }
                yield 0;
            }
            case DISCOVER_CHEST -> {
                // Compter les coffres découverts pour le chapitre de cette étape
                int count = 0;
                for (MysteryChest chest : MysteryChest.getChestsForChapter(step.getChapter())) {
                    if (data.hasDiscoveredChest(chest.getId())) {
                        count++;
                    }
                }
                yield count;
            }
            default -> {
                // Pour les autres types, utiliser la progression stockée
                UUID uuid = player.getUniqueId();
                String stepId = step.getId();

                // Vérifier le cache
                Map<String, Integer> progress = stepProgressCache.get(uuid);
                if (progress != null && progress.containsKey(stepId)) {
                    yield progress.get(stepId);
                }

                // Charger depuis PlayerData
                yield data.getJourneyStepProgress(stepId);
            }
        };
    }

    /**
     * Vérifie si une étape a été complétée par un joueur
     */
    public boolean isStepCompleted(Player player, JourneyStep step) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        return data.hasCompletedJourneyStep(step.getId());
    }

    /**
     * Détermine la zone ciblée par une étape ZONE_PROGRESS
     * Basé sur le chapitre de l'étape
     */
    private int getTargetZoneForStep(JourneyStep step) {
        // STEP_1_4 = Zone 1
        // Logique simple : le chapitre correspond généralement à la zone
        return switch (step) {
            case STEP_1_4 -> 1; // "Explore la Zone 1 (50%)"
            default -> step.getChapter().getId(); // Par défaut, zone = chapitre
        };
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

            // Déclencher les effets spéciaux de l'étape (spawn d'animaux, etc.)
            triggerStepStartEffects(player, nextStep);

            // Vérifier si la nouvelle étape est déjà complétée
            // (ex: le joueur a atteint le niveau 2 avant d'arriver à l'étape "Atteins le niveau 2")
            checkCurrentStepCompletion(player, nextStep);
        } else {
            // Fin du chapitre !
            completeChapter(player, step.getChapter());
        }
    }

    /**
     * Vérifie si l'étape actuelle est déjà complétée (basée sur l'état actuel du joueur)
     * Utile pour les étapes de type LEVEL, CLASS_LEVEL, etc. où la progression
     * peut avoir été atteinte avant d'arriver à cette étape
     */
    private void checkCurrentStepCompletion(Player player, JourneyStep step) {
        int currentProgress = getStepProgress(player, step);
        if (step.isCompleted(currentProgress)) {
            // Utiliser un délai court pour éviter les appels récursifs trop rapides
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    // Re-vérifier que c'est toujours l'étape actuelle
                    JourneyStep current = getCurrentStep(player);
                    if (current == step) {
                        completeStep(player, step);
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    /**
     * Déclenche les effets spéciaux au démarrage d'une étape
     * Ex: Spawn d'animaux pour l'étape de chasse, reset exploration, etc.
     */
    private void triggerStepStartEffects(Player player, JourneyStep step) {
        // Étape 1.6: Chasser 3 animaux - Spawn 3 animaux aléatoires autour du joueur
        if (step == JourneyStep.STEP_1_6) {
            spawnAnimalsForHuntingStep(player);
        }

        // Pour les étapes d'exploration: reset les chunks explorés de la zone cible
        // Évite que les chunks visités AVANT le déblocage de l'étape soient comptés
        if (step.getType() == JourneyStep.StepType.ZONE_EXPLORATION) {
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
            if (data != null) {
                int targetZone = step.getTargetValue();
                data.clearExploredChunks(targetZone);
                plugin.getLogger().info("[Journey] Reset exploration zone " + targetZone + " pour " + player.getName());
            }
        }

        // Pour ZONE_PROGRESS (Step 1.4 - Zone 1): reset zone 1
        if (step.getType() == JourneyStep.StepType.ZONE_PROGRESS && step == JourneyStep.STEP_1_4) {
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
            if (data != null) {
                data.clearExploredChunks(1); // Zone 1
                plugin.getLogger().info("[Journey] Reset exploration zone 1 pour " + player.getName());
            }
        }

        // Pour les étapes DISCOVER_CHEST: reset le coffre de la zone cible
        // Évite que les coffres découverts AVANT le déblocage de l'étape soient comptés
        if (step.getType() == JourneyStep.StepType.DISCOVER_CHEST) {
            MysteryChestManager chestManager = plugin.getMysteryChestManager();
            if (chestManager != null) {
                int targetZone = step.getTargetValue();
                chestManager.clearDiscoveredChestForZone(player.getUniqueId(), targetZone);
            }
        }

        // Étape 2.7: Aide Igor - Donne une hache spéciale pour couper du bois en Adventure
        if (step == JourneyStep.STEP_2_7) {
            giveWoodcutterAxe(player);
        }
    }

    // Nom de la hache pour identification
    private static final String WOODCUTTER_AXE_NAME = "Hache de Bûcheron";

    /**
     * Vérifie si le joueur possède la hache de bûcheron
     */
    public boolean hasWoodcutterAxe(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWoodcutterAxe(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un item est la hache de bûcheron
     */
    public boolean isWoodcutterAxe(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        // Vérifier le nom (en utilisant le plain text)
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
            .plainText().serialize(meta.displayName());
        return displayName.contains(WOODCUTTER_AXE_NAME);
    }

    /**
     * Donne une hache spéciale au joueur pour couper du bois en mode Adventure
     * La hache a le tag can_break pour OAK_LOG uniquement
     * @param isReplacement true si c'est un remplacement (message différent)
     */
    @SuppressWarnings("deprecation")
    public void giveWoodcutterAxe(Player player, boolean isReplacement) {
        // Ne pas donner si le joueur en a déjà une
        if (hasWoodcutterAxe(player)) {
            if (isReplacement) {
                player.sendMessage("§6§lIgor: §f\"Tu as déjà ma hache, survivant!\"");
            }
            return;
        }

        ItemStack axe = createWoodcutterAxe();

        // Donner au joueur
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(axe);
        } else {
            // Inventaire plein - forcer dans la main secondaire ou dropper avec message clair
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() == Material.AIR) {
                player.getInventory().setItemInOffHand(axe);
                player.sendMessage("§e§l⚠ §7Inventaire plein! Hache placée dans ta main secondaire.");
            } else {
                // Vraiment plein - drop avec glow effect
                org.bukkit.entity.Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), axe);
                droppedItem.setGlowing(true);
                droppedItem.setCustomNameVisible(true);
                droppedItem.customName(Component.text("§6§lHache de Bûcheron §7(Ramasse-moi!)", NamedTextColor.GOLD));
                // Empêcher le despawn pendant 10 minutes
                droppedItem.setUnlimitedLifetime(true);
                player.sendMessage("§c§l⚠ §eInventaire plein! §7La hache brille au sol, ramasse-la!");
            }
        }

        // Message
        player.sendMessage("");
        if (isReplacement) {
            player.sendMessage("§6§lIgor: §f\"Tiens, je te reprête ma hache!\"");
        } else {
            player.sendMessage("§6§l✦ §eIgor t'a prêté sa §6Hache de Bûcheron§e!");
        }
        player.sendMessage("§7Tu peux maintenant couper des bûches de chêne.");
        player.sendMessage("§7Ramène-lui §f8 bûches §7pour l'aider à reconstruire.");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
    }

    /**
     * Crée la hache de bûcheron (sans la donner)
     */
    @SuppressWarnings("deprecation")
    private ItemStack createWoodcutterAxe() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta == null) return axe;

        // Nom et lore
        meta.displayName(Component.text(WOODCUTTER_AXE_NAME, NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text(""),
            Component.text("Hache spéciale d'Igor", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Peut couper les bûches de chêne", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
            Component.text(""),
            Component.text("Ramène 8 bûches à Igor!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        // Permettre de casser les bûches de chêne en mode Adventure
        meta.setDestroyableKeys(List.of(
            NamespacedKey.minecraft("oak_log"),
            NamespacedKey.minecraft("oak_wood"),
            NamespacedKey.minecraft("stripped_oak_log"),
            NamespacedKey.minecraft("stripped_oak_wood")
        ));

        // Rendre incassable pour ne pas perdre la hache
        meta.setUnbreakable(true);

        axe.setItemMeta(meta);
        return axe;
    }

    /**
     * Wrapper pour l'appel initial (non-replacement)
     */
    private void giveWoodcutterAxe(Player player) {
        giveWoodcutterAxe(player, false);
    }

    /**
     * Fait spawn 3 animaux passifs ZombieZ custom autour du joueur pour l'étape de chasse
     * Utilise le PassiveMobManager pour spawn des animaux avec noms, vie et drops custom
     */
    private void spawnAnimalsForHuntingStep(Player player) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return;

        PassiveMobManager passiveMobManager = plugin.getPassiveMobManager();
        if (passiveMobManager == null) return;

        // Types d'animaux passifs ZombieZ disponibles
        PassiveMobManager.PassiveMobType[] animalTypes = {
            PassiveMobManager.PassiveMobType.PIG,
            PassiveMobManager.PassiveMobType.COW,
            PassiveMobManager.PassiveMobType.SHEEP,
            PassiveMobManager.PassiveMobType.CHICKEN,
            PassiveMobManager.PassiveMobType.RABBIT
        };

        Random random = new Random();

        // Message au joueur
        player.sendMessage("");
        player.sendMessage("§a§l➤ §eDes animaux sont apparus près de toi !");
        player.sendMessage("§7  Chasse-les pour compléter l'étape.");
        player.sendMessage("");

        // Spawn 3 animaux passifs ZombieZ à des positions aléatoires autour du joueur
        for (int i = 0; i < 3; i++) {
            // Position aléatoire dans un rayon de 5-10 blocs
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 5 + random.nextDouble() * 5; // 5-10 blocs
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            Location spawnLoc = playerLoc.clone().add(offsetX, 0, offsetZ);

            // Trouver le sol le plus proche
            spawnLoc = findSafeSpawnLocation(spawnLoc);
            if (spawnLoc == null) continue;

            // Choisir un type d'animal aléatoire
            PassiveMobManager.PassiveMobType animalType = animalTypes[random.nextInt(animalTypes.length)];

            // Spawn l'animal passif ZombieZ custom (avec nom, vie, drops, etc.)
            passiveMobManager.spawnPassiveMob(animalType, spawnLoc, 1);

            // Effet visuel de spawn
            world.spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
        }

        // Son d'apparition
        player.playSound(playerLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    /**
     * Trouve une position de spawn sûre (sur le sol, pas dans l'eau)
     */
    private Location findSafeSpawnLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        // Chercher le sol en dessous
        int startY = loc.getBlockY();
        for (int y = startY; y > startY - 10 && y > world.getMinHeight(); y--) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (checkLoc.getBlock().getType().isSolid() &&
                !checkLoc.getBlock().isLiquid()) {
                return checkLoc.add(0.5, 1, 0.5); // Centre du bloc, au-dessus
            }
        }

        // Chercher le sol au-dessus si on est dans le vide
        for (int y = startY; y < startY + 10 && y < world.getMaxHeight(); y++) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (checkLoc.getBlock().getType().isSolid() &&
                !checkLoc.getBlock().isLiquid()) {
                return checkLoc.add(0.5, 1, 0.5);
            }
        }

        return null;
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
        int maxUnlockedZone = 1;
        for (JourneyGate gate : chapter.getUnlocks()) {
            data.addJourneyGate(gate.name());
            unlockedGatesCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                .add(gate);

            // Tracker la zone max débloquée pour le WorldBorder
            if (gate.getType() == JourneyGate.GateType.ZONE) {
                maxUnlockedZone = Math.max(maxUnlockedZone, gate.getValue());
            }
        }

        // Mettre à jour le WorldBorder du joueur si des zones ont été débloquées
        if (maxUnlockedZone > 1 && plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().onZoneUnlocked(player, maxUnlockedZone);
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

                // Vérifier si la première étape du nouveau chapitre est déjà complétée
                checkCurrentStepCompletion(player, firstStep);
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
            "§eTu as complété le Journal du Survivant!",
            20, 100, 30
        );

        player.sendMessage("");
        player.sendMessage("§6§l§m                                                        ");
        player.sendMessage("");
        player.sendMessage("          §6§l✦ ✦ ✦ JOURNAL COMPLÉTÉ ✦ ✦ ✦");
        player.sendMessage("");
        player.sendMessage("          §eTu es désormais une §6§lLÉGENDE VIVANTE§e!");
        player.sendMessage("");
        player.sendMessage("          §7Tu as prouvé ta valeur à travers 21 chapitres");
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
            p.sendMessage("§7  Il a complété tout le Journal du Survivant!");
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
        Location playerLoc = player.getLocation();
        World playerWorld = playerLoc.getWorld();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == player) continue;
            // Vérifier que les joueurs sont dans le même monde avant de calculer la distance
            if (playerWorld != null && playerWorld.equals(p.getWorld())) {
                if (p.getLocation().distanceSquared(playerLoc) < 10000) { // 100² pour éviter sqrt
                    p.sendMessage(message);
                }
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

        // Charger les coffres mystères découverts
        mysteryChestManager.loadPlayerData(player, data.getDiscoveredMysteryChests());

        // Charger l'étape actuelle
        JourneyChapter chapter = JourneyChapter.getById(data.getCurrentJourneyChapter());
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int stepNum = data.getCurrentJourneyStep();
        if (stepNum > 0 && stepNum <= steps.size()) {
            JourneyStep currentStep = steps.get(stepNum - 1);
            currentStepCache.put(uuid, currentStep);

            // Vérifier si l'étape actuelle est déjà complétée (le joueur a peut-être progressé hors-ligne)
            checkCurrentStepCompletion(player, currentStep);
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
        mysteryChestManager.unloadPlayer(uuid);
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
     * Tronque un texte si trop long
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }
}
