package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de Multi-Kill Cascade
 *
 * Effet dopamine: Récompense visuellement et auditivement les kills rapides successifs
 * avec des effets de chaîne spectaculaires, créant des moments mémorables.
 *
 * Fonctionnalités:
 * - Détection des Double Kill, Triple Kill, Quad Kill, Penta Kill, etc.
 * - Effet visuel de chaîne/éclair entre les positions de kill
 * - Sons progressifs qui montent en intensité
 * - Bonus de points/XP pour les multi-kills
 * - Annonces pour les multi-kills impressionnants
 *
 * @author ZombieZ Dopamine System
 */
public class MultiKillCascadeManager implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // Configuration
    private static final long MULTI_KILL_WINDOW = 2000; // 2 secondes pour enchaîner les kills
    private static final int MAX_CHAIN_POSITIONS = 10;  // Nombre max de positions trackées

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
            data.killPositions.clear();
        }

        // Enregistrer ce kill
        data.currentChainCount++;
        data.lastKillTime = now;
        data.addKillPosition(location);

        // Vérifier si on a atteint un tier de multi-kill
        MultiKillTier tier = getTierForCount(data.currentChainCount);
        double bonusMultiplier = 1.0;

        if (tier != null && data.currentChainCount == tier.killCount) {
            // Nouveau tier atteint!
            triggerMultiKillEffects(player, data, tier);
            bonusMultiplier = 1.0 + (tier.killCount * 0.1); // +10% par kill dans la chaîne
        }

        // Effet de chaîne visuelle à chaque kill après le premier
        if (data.currentChainCount >= 2 && data.killPositions.size() >= 2) {
            spawnChainEffect(data.getLastTwoPositions());
        }

        // ActionBar feedback pour montrer le compteur de chaîne
        if (data.currentChainCount >= 2) {
            showChainActionBar(player, data.currentChainCount);
        }

        return bonusMultiplier;
    }

    /**
     * Affiche l'ActionBar avec le compteur de chaîne actuel
     */
    private void showChainActionBar(Player player, int chainCount) {
        // Construire la barre de progression visuelle
        NamedTextColor color = getActionBarColor(chainCount);
        String tierText = getChainTierText(chainCount);

        // Créer des symboles de chaîne visuels
        StringBuilder chainSymbols = new StringBuilder();
        int maxSymbols = Math.min(chainCount, 10);
        for (int i = 0; i < maxSymbols; i++) {
            chainSymbols.append("⚔");
        }
        if (chainCount > 10) {
            chainSymbols.append("+");
        }

        Component actionBar = Component.text("⚡ ", NamedTextColor.YELLOW)
            .append(Component.text("CHAÎNE x" + chainCount, color, TextDecoration.BOLD))
            .append(Component.text(" " + chainSymbols + " ", NamedTextColor.WHITE))
            .append(Component.text(tierText, color));

        player.sendActionBar(actionBar);
    }

    /**
     * Obtient la couleur de l'ActionBar selon le nombre de kills
     */
    private NamedTextColor getActionBarColor(int chainCount) {
        if (chainCount >= 10) return NamedTextColor.DARK_RED;
        if (chainCount >= 7) return NamedTextColor.DARK_PURPLE;
        if (chainCount >= 5) return NamedTextColor.RED;
        if (chainCount >= 4) return NamedTextColor.GOLD;
        if (chainCount >= 3) return NamedTextColor.YELLOW;
        return NamedTextColor.GREEN;
    }

    /**
     * Obtient le texte du tier pour l'ActionBar
     */
    private String getChainTierText(int chainCount) {
        for (int i = TIERS.length - 1; i >= 0; i--) {
            if (chainCount >= TIERS[i].killCount) {
                return TIERS[i].name;
            }
        }
        return chainCount >= 2 ? "→ Triple: " + (3 - chainCount) : "";
    }

    /**
     * Déclenche les effets visuels et sonores d'un multi-kill
     */
    private void triggerMultiKillEffects(Player player, MultiKillData data, MultiKillTier tier) {
        Location playerLoc = player.getLocation();

        // ═══════════════════════════════════════════════════════════════════
        // 1. TITRE SPECTACULAIRE
        // ═══════════════════════════════════════════════════════════════════
        String title = tier.color + "§l" + tier.name + "!";
        String subtitle = "§7" + tier.killCount + " kills en chaîne!";

        player.sendTitle(title, subtitle, 5, 25, 10);

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
        // 3. EFFET DE CASCADE VISUELLE
        // ═══════════════════════════════════════════════════════════════════
        spawnCascadeEffect(player, data.killPositions, tier);

        // ═══════════════════════════════════════════════════════════════════
        // 4. PARTICULES AUTOUR DU JOUEUR
        // ═══════════════════════════════════════════════════════════════════
        spawnPlayerAura(player, tier);

        // ═══════════════════════════════════════════════════════════════════
        // 5. ANNONCE SERVEUR POUR LES GROS MULTI-KILLS
        // ═══════════════════════════════════════════════════════════════════
        if (tier.killCount >= 5) {
            String announcement = tier.color + "⚔ " + player.getName() + " §7a réalisé un " +
                tier.color + "§l" + tier.name + "§7!";
            plugin.getServer().broadcastMessage(announcement);
        }

        // ═══════════════════════════════════════════════════════════════════
        // 6. MISE À JOUR DU RECORD PERSONNEL
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
     * Spawn l'effet de chaîne entre deux positions
     */
    private void spawnChainEffect(Location[] positions) {
        if (positions[0] == null || positions[1] == null) return;
        if (positions[0].getWorld() == null) return;

        World world = positions[0].getWorld();
        Location start = positions[0].clone().add(0, 1, 0);
        Location end = positions[1].clone().add(0, 1, 0);

        // Vérifier la distance (éviter les effets trop longs)
        if (start.distance(end) > 30) return;

        // Créer une ligne de particules entre les deux points
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        // Particules le long de la ligne
        for (double d = 0; d < distance; d += 0.3) {
            Location point = start.clone().add(direction.clone().multiply(d));

            // Particules d'électricité
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.1, 0.1, 0.1, 0);
        }

        // Flash aux extrémités
        world.spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FLASH, end, 1, 0, 0, 0, 0);

        // Son d'électricité
        world.playSound(start, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.3f, 2.0f);
    }

    /**
     * Spawn l'effet de cascade complet pour un multi-kill
     */
    private void spawnCascadeEffect(Player player, List<Location> positions, MultiKillTier tier) {
        if (positions.size() < 2) return;

        World world = player.getWorld();
        Color chainColor = getColorForTier(tier);

        new BukkitRunnable() {
            int index = 0;
            Location previous = null;

            @Override
            public void run() {
                if (index >= positions.size()) {
                    // Effet final au joueur
                    spawnFinalBurst(player.getLocation(), tier);
                    cancel();
                    return;
                }

                Location current = positions.get(index).clone().add(0, 1, 0);

                // Explosion à chaque position
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, current, 10, 0.3, 0.3, 0.3, 0.1);

                // Ligne vers la position précédente
                if (previous != null && previous.getWorld() == world) {
                    spawnAnimatedChain(world, previous, current, chainColor);
                }

                // Son progressif
                float pitch = 0.5f + (index * 0.2f);
                world.playSound(current, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, Math.min(2.0f, pitch));

                previous = current;
                index++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Spawn une chaîne animée entre deux points
     */
    private void spawnAnimatedChain(World world, Location start, Location end, Color color) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance > 30) return;

        direction.normalize();
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            // Légère oscillation pour effet de chaîne
            double offset = Math.sin(d * 3) * 0.15;
            point.add(offset, offset, offset);

            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    /**
     * Spawn l'explosion finale de la cascade
     */
    private void spawnFinalBurst(Location location, MultiKillTier tier) {
        World world = location.getWorld();
        if (world == null) return;

        // Explosion de particules
        world.spawnParticle(Particle.FIREWORK, location.clone().add(0, 1, 0),
            30 + (tier.killCount * 5), 1, 1, 1, 0.1);

        // Cercle de particules au sol
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 / 20) * i;
            double radius = 2.0;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            world.spawnParticle(Particle.FLAME, location.clone().add(x, 0.1, z), 3, 0, 0.1, 0, 0.02);
        }
    }

    /**
     * Spawn l'aura autour du joueur pour un multi-kill
     */
    private void spawnPlayerAura(Player player, MultiKillTier tier) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 15;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);
                double radius = 1.0 + (ticks * 0.1);

                // Spirale de particules
                for (int i = 0; i < 3; i++) {
                    double angle = (ticks * 0.4) + (i * Math.PI * 2 / 3);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = ticks * 0.1;

                    Particle particle = tier.killCount >= 5 ? Particle.END_ROD : Particle.ENCHANT;
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Obtient la couleur pour un tier
     */
    private Color getColorForTier(MultiKillTier tier) {
        return switch (tier.killCount) {
            case 2 -> Color.LIME;
            case 3 -> Color.YELLOW;
            case 4 -> Color.ORANGE;
            case 5 -> Color.RED;
            case 6 -> Color.FUCHSIA;
            case 7 -> Color.PURPLE;
            default -> Color.RED;
        };
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
        final List<Location> killPositions = new ArrayList<>();

        void addKillPosition(Location loc) {
            killPositions.add(loc.clone());
            // Garder seulement les dernières positions
            while (killPositions.size() > MAX_CHAIN_POSITIONS) {
                killPositions.remove(0);
            }
        }

        Location[] getLastTwoPositions() {
            int size = killPositions.size();
            if (size < 2) return new Location[]{null, null};
            return new Location[]{killPositions.get(size - 2), killPositions.get(size - 1)};
        }
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
