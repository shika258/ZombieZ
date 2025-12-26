package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de Multi-Kill Cascade
 *
 * Effet dopamine: Récompense auditivement les kills rapides successifs,
 * créant des moments mémorables.
 *
 * Fonctionnalités:
 * - Détection des Double Kill, Triple Kill, Quad Kill, Penta Kill, etc.
 * - Sons progressifs qui montent en intensité
 * - Bonus de points/XP pour les multi-kills
 * - Annonces pour les multi-kills impressionnants
 *
 * @author ZombieZ Dopamine System
 */
public class MultiKillCascadeManager implements Listener {

    private final ZombieZPlugin plugin;

    // Configuration
    private static final long MULTI_KILL_WINDOW = 2000; // 2 secondes pour enchaîner les kills

    // Données par joueur
    private final Map<UUID, MultiKillData> playerData = new ConcurrentHashMap<>();

    // Définitions des multi-kills
    private static final MultiKillTier[] TIERS = {
        new MultiKillTier(2, "DOUBLE KILL", "§a", Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.1f),
        new MultiKillTier(3, "TRIPLE KILL", "§e", Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.3f),
        new MultiKillTier(4, "QUAD KILL", "§6", Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f),
        new MultiKillTier(5, "PENTA KILL", "§c", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f),
        new MultiKillTier(6, "HEXA KILL", "§d", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f),
        new MultiKillTier(7, "ULTRA KILL", "§5", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f),
        new MultiKillTier(8, "MONSTER KILL", "§4", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.6f),
        new MultiKillTier(10, "GODLIKE", "§c§l", Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f)
    };

    public MultiKillCascadeManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    /**
     * Enregistre un kill et vérifie les multi-kills
     *
     * @param player   Le joueur qui a tué
     * @param location La position du mob tué
     * @return Le bonus de points à appliquer (multiplicateur)
     */
    public double registerKill(Player player, Location location) {
        if (player == null || location == null) return 1.0;

        UUID uuid = player.getUniqueId();
        MultiKillData data = playerData.computeIfAbsent(uuid, k -> new MultiKillData());

        long now = System.currentTimeMillis();

        // Vérifier si on est toujours dans la fenêtre de multi-kill
        if (now - data.lastKillTime > MULTI_KILL_WINDOW) {
            // Fenêtre expirée - réinitialiser le compteur
            data.currentChainCount = 0;
        }

        // Enregistrer ce kill
        data.currentChainCount++;
        data.lastKillTime = now;

        // Vérifier si on a atteint un tier de multi-kill
        MultiKillTier tier = getTierForCount(data.currentChainCount);
        double bonusMultiplier = 1.0;

        if (tier != null && data.currentChainCount == tier.killCount) {
            // Nouveau tier atteint!
            triggerMultiKillEffects(player, data, tier);
            bonusMultiplier = 1.0 + (tier.killCount * 0.1); // +10% par kill dans la chaîne
        }


        // Note: On n'affiche plus le compteur de chaîne entre les tiers
        // Seuls les noms de kills (Triple Kill, Hexa Kill, etc.) sont affichés

        return bonusMultiplier;
    }

    /**
     * Déclenche les effets sonores d'un multi-kill
     */
    private void triggerMultiKillEffects(Player player, MultiKillData data, MultiKillTier tier) {
        Location playerLoc = player.getLocation();

        // ═══════════════════════════════════════════════════════════════════
        // 1. AFFICHAGE DU MULTI-KILL (subtitle uniquement, à partir de 5 kills)
        // ═══════════════════════════════════════════════════════════════════
        if (tier.killCount >= 5) {
            String killName = tier.color + "§l" + tier.name + "!";
            // Affiche uniquement le nom du kill en subtitle (pas de title)
            player.sendTitle("", killName, 5, 25, 10);
        }

        // ═══════════════════════════════════════════════════════════════════
        // 2. SONS PROGRESSIFS
        // ═══════════════════════════════════════════════════════════════════
        player.playSound(playerLoc, tier.sound, tier.volume, tier.pitch);

        // Son additionnel d'impact
        player.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.5f);

        // Pour les gros multi-kills, ajouter un son dramatique
        if (tier.killCount >= 5) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.playSound(playerLoc, Sound.ENTITY_WITHER_SPAWN, 0.2f, 2.0f);
            }, 5L);
        }


        // ═══════════════════════════════════════════════════════════════════
        // 5. MISE À JOUR DU RECORD PERSONNEL
        // ═══════════════════════════════════════════════════════════════════
        if (tier.killCount > data.bestMultiKill) {
            data.bestMultiKill = tier.killCount;

            // Notification de nouveau record
            if (tier.killCount >= 4) {
                player.sendMessage("§6§l★ §eNouveau record personnel: §f" + tier.name + "§e!");
            }
        }
    }

    /**
     * Obtient le tier pour un nombre de kills
     */
    private MultiKillTier getTierForCount(int count) {
        MultiKillTier result = null;
        for (MultiKillTier tier : TIERS) {
            if (tier.killCount <= count) {
                result = tier;
            }
        }
        return result;
    }

    /**
     * Obtient le meilleur multi-kill du joueur
     */
    public int getBestMultiKill(Player player) {
        MultiKillData data = playerData.get(player.getUniqueId());
        return data != null ? data.bestMultiKill : 0;
    }

    /**
     * Obtient le nombre de kills dans la chaîne actuelle
     */
    public int getCurrentChain(Player player) {
        MultiKillData data = playerData.get(player.getUniqueId());
        if (data == null) return 0;

        // Vérifier si la chaîne est encore active
        if (System.currentTimeMillis() - data.lastKillTime > MULTI_KILL_WINDOW) {
            return 0;
        }
        return data.currentChainCount;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // Vérifier si c'est un mob ZombieZ
        if (!plugin.getZombieManager().isZombieZMob(entity)) return;

        // Enregistrer le kill
        registerKill(killer, entity.getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tâche de nettoyage des données expirées
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            playerData.entrySet().removeIf(entry -> {
                MultiKillData data = entry.getValue();
                // Supprimer les données des joueurs déconnectés après 5 minutes
                return now - data.lastKillTime > 300000;
            });
        }, 20L * 60, 20L * 60); // Toutes les minutes
    }

    public void shutdown() {
        playerData.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Données de multi-kill par joueur
     */
    private static class MultiKillData {
        int currentChainCount = 0;
        long lastKillTime = 0;
        int bestMultiKill = 0;
    }

    /**
     * Définition d'un tier de multi-kill
     */
    private record MultiKillTier(
        int killCount,
        String name,
        String color,
        Sound sound,
        float volume,
        float pitch
    ) {}
}
