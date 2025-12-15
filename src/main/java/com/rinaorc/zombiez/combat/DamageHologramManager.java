package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Système d'hologrammes de dégâts optimisé
 *
 * Optimisations:
 * - Pool d'ArmorStands réutilisables
 * - Limite de hologrammes par zone
 * - Nettoyage automatique
 * - Per-player toggle
 * - Batching des updates
 */
public class DamageHologramManager {

    private final ZombieZPlugin plugin;

    // Pool d'ArmorStands disponibles
    private final Queue<ArmorStand> armorStandPool = new ConcurrentLinkedQueue<>();

    // Hologrammes actifs (ArmorStand -> timestamp expiration)
    private final Map<ArmorStand, HologramData> activeHolograms = new ConcurrentHashMap<>();

    // Joueurs avec hologrammes désactivés
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    // Configuration
    private static final int POOL_SIZE = 100;              // Taille du pool
    private static final int MAX_HOLOGRAMS_PER_CHUNK = 15; // Limite par chunk
    private static final long HOLOGRAM_LIFETIME_MS = 1500; // Durée de vie (1.5s)
    private static final double FLOAT_SPEED = 0.05;        // Vitesse de montée
    private static final double RANDOM_OFFSET = 0.5;       // Offset aléatoire XZ

    // Compteur par chunk pour limiter
    private final Map<Long, Integer> chunkHologramCount = new ConcurrentHashMap<>();

    public DamageHologramManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initializePool();
        startCleanupTask();
        startAnimationTask();
    }

    /**
     * Initialise le pool d'ArmorStands
     */
    private void initializePool() {
        // Le pool sera rempli à la demande pour éviter de créer des entités inutilement
    }

    /**
     * Spawn un hologramme de dégâts
     */
    public void spawnDamageHologram(Location location, DamageInfo damageInfo, Player viewer) {
        // Vérifier si le joueur a désactivé les hologrammes
        if (disabledPlayers.contains(viewer.getUniqueId())) return;

        // Vérifier la limite par chunk
        long chunkKey = getChunkKey(location);
        int currentCount = chunkHologramCount.getOrDefault(chunkKey, 0);
        if (currentCount >= MAX_HOLOGRAMS_PER_CHUNK) return;

        // Créer l'hologramme
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ArmorStand hologram = getOrCreateArmorStand(location);
            if (hologram == null) return;

            // Configurer l'hologramme
            setupHologram(hologram, location, damageInfo);

            // Enregistrer
            activeHolograms.put(hologram, new HologramData(
                System.currentTimeMillis() + HOLOGRAM_LIFETIME_MS,
                location.clone(),
                chunkKey
            ));

            chunkHologramCount.merge(chunkKey, 1, Integer::sum);
        });
    }

    /**
     * Spawn un hologramme pour plusieurs viewers
     */
    public void spawnDamageHologramForNearby(Location location, DamageInfo damageInfo, double radius) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radius * radius) {
                spawnDamageHologram(location, damageInfo, player);
                break; // Un seul hologramme par dégât
            }
        }
    }

    /**
     * Obtient ou crée un ArmorStand
     */
    private ArmorStand getOrCreateArmorStand(Location location) {
        // Essayer de récupérer du pool
        ArmorStand stand = armorStandPool.poll();

        if (stand != null && stand.isValid()) {
            stand.teleport(location);
            return stand;
        }

        // Créer un nouveau
        return createArmorStand(location);
    }

    /**
     * Crée un nouvel ArmorStand optimisé
     */
    private ArmorStand createArmorStand(Location location) {
        // Ajouter un offset aléatoire pour éviter l'empilement
        Location spawnLoc = location.clone().add(
            (Math.random() - 0.5) * RANDOM_OFFSET,
            1.5 + Math.random() * 0.3,
            (Math.random() - 0.5) * RANDOM_OFFSET
        );

        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

        // Configuration optimisée
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(true);           // Pas de hitbox
        stand.setInvulnerable(true);
        stand.setCustomNameVisible(true);
        stand.setCollidable(false);
        stand.setSilent(true);
        stand.setPersistent(false);       // Ne pas sauvegarder

        return stand;
    }

    /**
     * Configure l'hologramme avec le texte de dégâts
     */
    private void setupHologram(ArmorStand hologram, Location location, DamageInfo info) {
        String text = buildDamageText(info);
        hologram.setCustomName(text);
    }

    /**
     * Construit le texte de dégâts avec couleurs et icônes
     */
    private String buildDamageText(DamageInfo info) {
        StringBuilder text = new StringBuilder();

        // Formater le nombre
        String damageStr = formatDamage(info.totalDamage);

        // Style selon le type principal
        switch (info.primaryType) {
            case CRITICAL -> {
                text.append("§c§l✦ ").append(damageStr).append(" §c§l✦");
            }
            case FIRE -> {
                text.append("§6🔥 ").append(damageStr);
                if (info.fireDamage > 0) {
                    text.append(" §7(+§6").append(formatDamage(info.fireDamage)).append("§7)");
                }
            }
            case ICE -> {
                text.append("§b❄ ").append(damageStr);
                if (info.iceDamage > 0) {
                    text.append(" §7(+§b").append(formatDamage(info.iceDamage)).append("§7)");
                }
            }
            case LIGHTNING -> {
                text.append("§e⚡ ").append(damageStr);
                if (info.lightningDamage > 0) {
                    text.append(" §7(+§e").append(formatDamage(info.lightningDamage)).append("§7)");
                }
            }
            case POISON -> {
                text.append("§2☠ ").append(damageStr);
                if (info.poisonDamage > 0) {
                    text.append(" §7(+§2").append(formatDamage(info.poisonDamage)).append("§7)");
                }
            }
            case EXECUTE -> {
                text.append("§4§l☠ ").append(damageStr).append(" §4§lEXECUTE");
            }
            case BERSERKER -> {
                text.append("§5§l⚔ ").append(damageStr).append(" §5§lRAGE");
            }
            case HEAL -> {
                text.append("§a§l+ ").append(damageStr).append(" §a❤");
            }
            case LIFESTEAL -> {
                text.append("§4❤ +").append(damageStr);
            }
            case DODGE -> {
                text.append("§a§l↷ ESQUIVE!");
            }
            case BLOCKED -> {
                text.append("§7§l✖ BLOQUÉ");
            }
            case IMMUNE -> {
                text.append("§8§lIMMUNE");
            }
            default -> {
                // Normal - couleur basée sur le montant
                String color = getDamageColor(info.totalDamage);
                text.append(color).append(damageStr);
            }
        }

        // Ajouter indicateurs secondaires
        if (info.isCritical && info.primaryType != DamageType.CRITICAL) {
            text.append(" §c✦");
        }

        if (info.comboMultiplier > 1.0) {
            text.append(" §6x").append(String.format("%.1f", info.comboMultiplier));
        }

        return text.toString();
    }

    /**
     * Formate le nombre de dégâts
     */
    private String formatDamage(double damage) {
        if (damage >= 1000) {
            return String.format("%.1fK", damage / 1000);
        } else if (damage >= 100) {
            return String.valueOf((int) damage);
        } else if (damage >= 10) {
            return String.format("%.1f", damage);
        } else {
            return String.format("%.2f", damage);
        }
    }

    /**
     * Obtient la couleur basée sur le montant de dégâts
     */
    private String getDamageColor(double damage) {
        if (damage >= 500) return "§4§l";      // Rouge foncé gras
        if (damage >= 200) return "§c§l";      // Rouge gras
        if (damage >= 100) return "§c";        // Rouge
        if (damage >= 50) return "§6";         // Orange
        if (damage >= 25) return "§e";         // Jaune
        if (damage >= 10) return "§f";         // Blanc
        return "§7";                            // Gris
    }

    /**
     * Task d'animation (faire monter les hologrammes)
     */
    private void startAnimationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<ArmorStand, HologramData> entry : activeHolograms.entrySet()) {
                    ArmorStand stand = entry.getKey();
                    if (stand.isValid()) {
                        // Faire monter
                        Location loc = stand.getLocation();
                        loc.add(0, FLOAT_SPEED, 0);
                        stand.teleport(loc);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Chaque tick
    }

    /**
     * Task de nettoyage des hologrammes expirés
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<ArmorStand, HologramData>> iterator = activeHolograms.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<ArmorStand, HologramData> entry = iterator.next();
                    ArmorStand stand = entry.getKey();
                    HologramData data = entry.getValue();

                    if (now >= data.expireTime || !stand.isValid()) {
                        // Recycler ou supprimer
                        recycleArmorStand(stand);
                        iterator.remove();

                        // Décrémenter le compteur de chunk
                        chunkHologramCount.computeIfPresent(data.chunkKey, (k, v) -> v > 1 ? v - 1 : null);
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L); // Toutes les 5 ticks
    }

    /**
     * Recycle un ArmorStand dans le pool
     */
    private void recycleArmorStand(ArmorStand stand) {
        if (stand.isValid()) {
            // Remettre dans le pool si pas plein
            if (armorStandPool.size() < POOL_SIZE) {
                stand.setCustomName("");
                stand.teleport(new Location(stand.getWorld(), 0, -100, 0)); // Hors vue
                armorStandPool.offer(stand);
            } else {
                stand.remove();
            }
        }
    }

    /**
     * Obtient la clé unique d'un chunk
     */
    private long getChunkKey(Location location) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    // ==================== GESTION DES PRÉFÉRENCES ====================

    /**
     * Toggle l'affichage des hologrammes pour un joueur
     */
    public boolean toggleHolograms(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
            return true; // Activé
        } else {
            disabledPlayers.add(uuid);
            return false; // Désactivé
        }
    }

    /**
     * Vérifie si les hologrammes sont activés pour un joueur
     */
    public boolean areHologramsEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }

    /**
     * Active les hologrammes pour un joueur
     */
    public void enableHolograms(Player player) {
        disabledPlayers.remove(player.getUniqueId());
    }

    /**
     * Désactive les hologrammes pour un joueur
     */
    public void disableHolograms(Player player) {
        disabledPlayers.add(player.getUniqueId());
    }

    /**
     * Charge les préférences depuis PlayerData
     */
    public void loadPreferences(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null && !data.isDamageHologramsEnabled()) {
            disabledPlayers.add(player.getUniqueId());
        }
    }

    /**
     * Sauvegarde les préférences dans PlayerData
     */
    public void savePreferences(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            data.setDamageHologramsEnabled(!disabledPlayers.contains(player.getUniqueId()));
        }
    }

    // ==================== CLEANUP ====================

    /**
     * Nettoie tous les hologrammes (appelé à l'arrêt du plugin)
     */
    public void cleanup() {
        // Supprimer tous les hologrammes actifs
        for (ArmorStand stand : activeHolograms.keySet()) {
            if (stand.isValid()) {
                stand.remove();
            }
        }
        activeHolograms.clear();

        // Vider le pool
        ArmorStand stand;
        while ((stand = armorStandPool.poll()) != null) {
            if (stand.isValid()) {
                stand.remove();
            }
        }

        chunkHologramCount.clear();
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Données d'un hologramme actif
     */
    private record HologramData(long expireTime, Location originalLocation, long chunkKey) {}

    /**
     * Types de dégâts pour l'affichage
     */
    @Getter
    public enum DamageType {
        NORMAL("§f", ""),
        CRITICAL("§c§l", "✦"),
        FIRE("§6", "🔥"),
        ICE("§b", "❄"),
        LIGHTNING("§e", "⚡"),
        POISON("§2", "☠"),
        EXECUTE("§4§l", "☠"),
        BERSERKER("§5§l", "⚔"),
        HEAL("§a", "❤"),
        LIFESTEAL("§4", "❤"),
        DODGE("§a§l", "↷"),
        BLOCKED("§7§l", "✖"),
        IMMUNE("§8§l", "");

        private final String color;
        private final String icon;

        DamageType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    /**
     * Informations sur les dégâts à afficher
     */
    @Getter
    public static class DamageInfo {
        private final double totalDamage;
        private final DamageType primaryType;
        private final boolean isCritical;
        private final double comboMultiplier;

        // Dégâts élémentaires détaillés
        private double fireDamage = 0;
        private double iceDamage = 0;
        private double lightningDamage = 0;
        private double poisonDamage = 0;

        public DamageInfo(double totalDamage, DamageType primaryType) {
            this.totalDamage = totalDamage;
            this.primaryType = primaryType;
            this.isCritical = false;
            this.comboMultiplier = 1.0;
        }

        public DamageInfo(double totalDamage, DamageType primaryType, boolean isCritical, double comboMultiplier) {
            this.totalDamage = totalDamage;
            this.primaryType = primaryType;
            this.isCritical = isCritical;
            this.comboMultiplier = comboMultiplier;
        }

        // Builder pattern pour les dégâts élémentaires
        public DamageInfo withFireDamage(double damage) {
            this.fireDamage = damage;
            return this;
        }

        public DamageInfo withIceDamage(double damage) {
            this.iceDamage = damage;
            return this;
        }

        public DamageInfo withLightningDamage(double damage) {
            this.lightningDamage = damage;
            return this;
        }

        public DamageInfo withPoisonDamage(double damage) {
            this.poisonDamage = damage;
            return this;
        }

        // Factory methods pour des cas communs
        public static DamageInfo normal(double damage) {
            return new DamageInfo(damage, DamageType.NORMAL);
        }

        public static DamageInfo critical(double damage) {
            return new DamageInfo(damage, DamageType.CRITICAL, true, 1.0);
        }

        public static DamageInfo critical(double damage, double comboMultiplier) {
            return new DamageInfo(damage, DamageType.CRITICAL, true, comboMultiplier);
        }

        public static DamageInfo fire(double totalDamage, double fireDamage) {
            return new DamageInfo(totalDamage, DamageType.FIRE).withFireDamage(fireDamage);
        }

        public static DamageInfo ice(double totalDamage, double iceDamage) {
            return new DamageInfo(totalDamage, DamageType.ICE).withIceDamage(iceDamage);
        }

        public static DamageInfo lightning(double totalDamage, double lightningDamage) {
            return new DamageInfo(totalDamage, DamageType.LIGHTNING).withLightningDamage(lightningDamage);
        }

        public static DamageInfo poison(double totalDamage, double poisonDamage) {
            return new DamageInfo(totalDamage, DamageType.POISON).withPoisonDamage(poisonDamage);
        }

        public static DamageInfo execute(double damage) {
            return new DamageInfo(damage, DamageType.EXECUTE);
        }

        public static DamageInfo berserker(double damage) {
            return new DamageInfo(damage, DamageType.BERSERKER);
        }

        public static DamageInfo heal(double amount) {
            return new DamageInfo(amount, DamageType.HEAL);
        }

        public static DamageInfo lifesteal(double amount) {
            return new DamageInfo(amount, DamageType.LIFESTEAL);
        }

        public static DamageInfo dodge() {
            return new DamageInfo(0, DamageType.DODGE);
        }

        public static DamageInfo blocked() {
            return new DamageInfo(0, DamageType.BLOCKED);
        }

        public static DamageInfo immune() {
            return new DamageInfo(0, DamageType.IMMUNE);
        }
    }
}
