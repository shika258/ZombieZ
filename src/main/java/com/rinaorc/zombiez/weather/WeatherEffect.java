package com.rinaorc.zombiez.weather;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Représente un effet météorologique actif dans le monde ZombieZ
 *
 * Les effets météo sont globaux et affectent tous les joueurs et zombies.
 * Chaque effet a une durée limitée et des impacts sur le gameplay.
 *
 * Caractéristiques:
 * - Effets visuels (particules autour des joueurs)
 * - Modifications du spawn rate des zombies
 * - Buffs/debuffs sur joueurs et zombies
 * - Dégâts environnementaux (optionnel)
 * - Boss bar pour informer les joueurs
 */
@Getter
public class WeatherEffect {

    protected final ZombieZPlugin plugin;
    protected final String id;
    protected final WeatherType type;
    protected final long startTime;

    @Setter
    protected boolean active = false;
    protected boolean completed = false;
    protected boolean cancelled = false;

    // Durée de l'effet (en ticks)
    protected int duration;
    protected int elapsedTicks = 0;

    // Joueurs affectés - Thread-safe
    protected final Set<UUID> affectedPlayers = ConcurrentHashMap.newKeySet();

    // Joueurs protégés (sous un abri)
    protected final Set<UUID> shelteredPlayers = ConcurrentHashMap.newKeySet();

    // Tâches planifiées
    protected BukkitTask mainTask;
    protected BukkitTask particleTask;
    protected BukkitTask damageTask;

    // Boss bar globale
    protected BossBar bossBar;

    // Configuration (peut être modifiée par le manager)
    @Setter protected double spawnMultiplierOverride = -1;    // -1 = utiliser la valeur du type
    @Setter protected double damageMultiplierOverride = -1;
    @Setter protected double intensityMultiplier = 1.0;       // Intensité des effets visuels

    // Statistiques
    protected int totalDamageDealt = 0;
    protected int playersAffectedCount = 0;

    // Monde cible (null = tous les mondes)
    protected World targetWorld;

    public WeatherEffect(ZombieZPlugin plugin, WeatherType type, int durationTicks) {
        this.plugin = plugin;
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.duration = durationTicks;
        this.startTime = System.currentTimeMillis();

        // Définir le monde cible (monde principal par défaut)
        this.targetWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    public WeatherEffect(ZombieZPlugin plugin, WeatherType type) {
        this(plugin, type, calculateRandomDuration(type));
    }

    /**
     * Calcule une durée aléatoire basée sur les min/max du type
     */
    private static int calculateRandomDuration(WeatherType type) {
        Random random = new Random();
        int range = type.getMaxDuration() - type.getMinDuration();
        return type.getMinDuration() + (range > 0 ? random.nextInt(range) : 0);
    }

    /**
     * Démarre l'effet météo
     */
    public void start() {
        if (active) return;

        active = true;

        // Créer la boss bar
        createBossBar();

        // Appliquer la météo Minecraft
        applyMinecraftWeather();

        // Annoncer le changement de météo
        announceStart();

        // Démarrer les tâches
        startTasks();
    }

    /**
     * Termine l'effet météo naturellement
     */
    public void complete() {
        if (!active || completed || cancelled) return;

        completed = true;
        active = false;

        // Annoncer la fin
        announceEnd();

        // Cleanup
        cleanup();
    }

    /**
     * Annule l'effet météo prématurément
     */
    public void cancel() {
        if (!active || completed || cancelled) return;

        cancelled = true;
        active = false;

        // Annoncer l'interruption
        announceInterruption();

        // Cleanup
        cleanup();
    }

    /**
     * Force l'arrêt immédiat sans annonces
     */
    public void forceStop() {
        active = false;
        cleanup();
    }

    /**
     * Nettoyage des ressources
     */
    protected void cleanup() {
        // Annuler les tâches
        if (mainTask != null && !mainTask.isCancelled()) {
            mainTask.cancel();
        }
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
        if (damageTask != null && !damageTask.isCancelled()) {
            damageTask.cancel();
        }

        // Supprimer la boss bar
        if (bossBar != null) {
            bossBar.removeAll();
        }

        // Restaurer la météo Minecraft
        restoreMinecraftWeather();

        // Retirer les effets des joueurs
        removePlayerEffects();
    }

    /**
     * Créé la boss bar de l'effet météo
     */
    protected void createBossBar() {
        String title = type.getColor() + "§l" + type.getIcon() + " " + type.getDisplayName();
        bossBar = plugin.getServer().createBossBar(title, type.getBarColor(), BarStyle.SEGMENTED_20);
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        // Ajouter tous les joueurs
        for (Player player : getAffectedPlayers()) {
            bossBar.addPlayer(player);
        }
    }

    /**
     * Met à jour la boss bar
     */
    protected void updateBossBar() {
        if (bossBar == null) return;

        double progress = 1.0 - ((double) elapsedTicks / duration);
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));

        int remainingSeconds = getRemainingTimeSeconds();
        String timeStr = formatTime(remainingSeconds);

        String title = type.getColor() + "§l" + type.getIcon() + " " + type.getDisplayName() +
            " §7- §e" + timeStr;
        bossBar.setTitle(title);

        // Mettre à jour les joueurs
        Set<Player> current = new HashSet<>(bossBar.getPlayers());
        Set<Player> affected = new HashSet<>(getAffectedPlayers());

        // Ajouter les nouveaux joueurs
        for (Player player : affected) {
            if (!current.contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        // Retirer les joueurs qui ne sont plus affectés
        for (Player player : current) {
            if (!affected.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    /**
     * Formate le temps en MM:SS
     */
    protected String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    /**
     * Applique la météo Minecraft native
     */
    protected void applyMinecraftWeather() {
        if (targetWorld == null) return;

        if (type.isMinecraftRain()) {
            targetWorld.setStorm(true);
            targetWorld.setWeatherDuration(duration + 20 * 60); // +1 minute de marge
        }

        if (type.isMinecraftThunder()) {
            targetWorld.setThundering(true);
            targetWorld.setThunderDuration(duration + 20 * 60);
        }
    }

    /**
     * Restaure la météo Minecraft
     */
    protected void restoreMinecraftWeather() {
        if (targetWorld == null) return;

        targetWorld.setStorm(false);
        targetWorld.setThundering(false);
    }

    /**
     * Démarre les tâches périodiques
     */
    protected void startTasks() {
        // Tâche principale (chaque seconde)
        mainTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tick();
        }, 20L, 20L);

        // Tâche de particules (4 fois par seconde pour plus de fluidité)
        if (type.getParticle() != null) {
            particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                spawnParticles();
            }, 5L, 5L);
        }

        // Tâche de dégâts environnementaux
        if (type.isDangerous() && type.getDamageInterval() > 0) {
            damageTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                applyEnvironmentalDamage();
            }, type.getDamageInterval(), type.getDamageInterval());
        }
    }

    /**
     * Tick principal (appelé chaque seconde)
     */
    public void tick() {
        if (!active) return;

        elapsedTicks += 20; // +1 seconde

        // Mettre à jour la boss bar
        updateBossBar();

        // Appliquer les effets de debuff
        applyDebuffEffects();

        // Jouer le son ambiant périodiquement
        if (elapsedTicks % (20 * 10) == 0 && type.getAmbientSound() != null) {
            playAmbientSound();
        }

        // Vérifier si l'effet doit se terminer
        if (elapsedTicks >= duration) {
            complete();
        }
    }

    /**
     * Génère les particules autour des joueurs
     */
    protected void spawnParticles() {
        if (type.getParticle() == null) return;

        for (Player player : getAffectedPlayers()) {
            Location loc = player.getLocation();

            // Particules dans un rayon autour du joueur
            int particleCount = (int) (15 * intensityMultiplier);
            double radius = 8.0;

            for (int i = 0; i < particleCount; i++) {
                double x = loc.getX() + (Math.random() - 0.5) * radius * 2;
                double y = loc.getY() + Math.random() * 6 + 2;
                double z = loc.getZ() + (Math.random() - 0.5) * radius * 2;

                Location particleLoc = new Location(loc.getWorld(), x, y, z);

                // Vitesse de chute des particules selon le type
                double velocityY = type == WeatherType.RAIN || type == WeatherType.ACID_RAIN
                    ? -0.5 : (type == WeatherType.BLIZZARD ? -0.2 : 0);

                player.spawnParticle(type.getParticle(), particleLoc, 1, 0, velocityY, 0, 0);
            }

            // Particules spéciales selon le type
            spawnSpecialParticles(player);
        }
    }

    /**
     * Génère des particules spéciales selon le type de météo
     */
    protected void spawnSpecialParticles(Player player) {
        Location loc = player.getLocation();

        switch (type) {
            case STORM -> {
                // Éclairs aléatoires dans le ciel
                if (Math.random() < 0.02) {
                    double x = loc.getX() + (Math.random() - 0.5) * 100;
                    double z = loc.getZ() + (Math.random() - 0.5) * 100;
                    Location lightningLoc = new Location(loc.getWorld(), x, loc.getY() + 30, z);
                    player.spawnParticle(Particle.FLASH, lightningLoc, 1);
                }
            }
            case BLOOD_MOON -> {
                // Particules rouges montantes
                player.spawnParticle(Particle.DUST, loc.add(0, 0.5, 0),
                    3, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(Color.RED, 1.0f));
            }
            case AURORA -> {
                // Particules colorées dans le ciel
                Color[] colors = {Color.PURPLE, Color.BLUE, Color.AQUA, Color.GREEN};
                Color color = colors[(int) (Math.random() * colors.length)];
                Location skyLoc = loc.clone().add(
                    (Math.random() - 0.5) * 20,
                    15 + Math.random() * 10,
                    (Math.random() - 0.5) * 20
                );
                player.spawnParticle(Particle.DUST, skyLoc, 2, 1, 0.5, 1, 0,
                    new Particle.DustOptions(color, 2.0f));
            }
            case ACID_RAIN -> {
                // Particules vertes tombantes
                player.spawnParticle(Particle.DUST, loc.clone().add(0, 2, 0),
                    2, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.LIME, 0.8f));
            }
            default -> {}
        }
    }

    /**
     * Applique les dégâts environnementaux aux joueurs
     */
    protected void applyEnvironmentalDamage() {
        if (!type.isDangerous()) return;

        double damage = type.getEnvironmentalDamage() * (damageMultiplierOverride > 0 ? damageMultiplierOverride : 1.0);

        for (Player player : getAffectedPlayers()) {
            // Vérifier si le joueur est à l'abri
            if (isPlayerSheltered(player)) {
                shelteredPlayers.add(player.getUniqueId());
                continue;
            } else {
                shelteredPlayers.remove(player.getUniqueId());
            }

            // Appliquer les dégâts
            double finalDamage = damage;

            // Réduire les dégâts si le joueur a une armure adaptée
            finalDamage = calculateReducedDamage(player, finalDamage);

            if (finalDamage > 0) {
                player.damage(finalDamage);
                totalDamageDealt += (int) finalDamage;

                // Message d'avertissement (pas trop fréquent)
                if (elapsedTicks % (20 * 10) == 0) {
                    player.sendMessage(type.getColor() + type.getIcon() + " §cVous subissez des dégâts de " +
                        type.getDisplayName().toLowerCase() + "! Trouvez un abri!");
                }
            }
        }
    }

    /**
     * Vérifie si un joueur est à l'abri (sous un toit)
     */
    protected boolean isPlayerSheltered(Player player) {
        Location loc = player.getLocation();

        // Vérifier s'il y a un bloc solide au-dessus du joueur
        for (int y = 1; y <= 10; y++) {
            Location checkLoc = loc.clone().add(0, y, 0);
            if (checkLoc.getBlock().getType().isSolid()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calcule les dégâts réduits selon l'équipement du joueur
     */
    protected double calculateReducedDamage(Player player, double baseDamage) {
        // Réduction basée sur l'armure
        double armorReduction = 0;

        // Casque protège contre les dégâts aériens (pluie, cendres)
        if (player.getInventory().getHelmet() != null) {
            armorReduction += 0.15;
        }

        // Plastron pour la protection générale
        if (player.getInventory().getChestplate() != null) {
            armorReduction += 0.10;
        }

        return baseDamage * (1.0 - armorReduction);
    }

    /**
     * Applique les effets de debuff aux joueurs
     */
    protected void applyDebuffEffects() {
        PotionEffectType debuffType = type.getPlayerDebuffEffect();
        if (debuffType == null) return;

        int amplifier = type.getDebuffAmplifier();
        int duration = type.getDebuffDuration();

        for (Player player : getAffectedPlayers()) {
            // Les joueurs à l'abri ne reçoivent pas les debuffs
            if (shelteredPlayers.contains(player.getUniqueId())) continue;

            // Appliquer l'effet
            player.addPotionEffect(new PotionEffect(debuffType, duration, amplifier, true, false, true));
        }
    }

    /**
     * Retire les effets des joueurs à la fin de la météo
     */
    protected void removePlayerEffects() {
        PotionEffectType debuffType = type.getPlayerDebuffEffect();
        if (debuffType == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.removePotionEffect(debuffType);
        }
    }

    /**
     * Joue le son ambiant
     */
    protected void playAmbientSound() {
        if (type.getAmbientSound() == null) return;

        for (Player player : getAffectedPlayers()) {
            player.playSound(player.getLocation(), type.getAmbientSound(), 0.5f, 1.0f);
        }
    }

    /**
     * Annonce le début de l'effet météo
     */
    protected void announceStart() {
        String title = type.getColor() + "§l" + type.getIcon() + " " + type.getDisplayName().toUpperCase();
        String subtitle = "§7" + type.getDescription();

        for (Player player : getAffectedPlayers()) {
            player.sendTitle(title, subtitle, 20, 60, 20);

            if (type.getAmbientSound() != null) {
                player.playSound(player.getLocation(), type.getAmbientSound(), 1.0f, 0.8f);
            }

            // Message détaillé dans le chat
            player.sendMessage("");
            player.sendMessage(type.getColor() + "§l" + type.getIcon() + " MÉTÉO: " + type.getDisplayName());
            player.sendMessage("§7" + type.getDescription());

            if (type.isDangerous()) {
                player.sendMessage("§c⚠ Attention: Cette météo inflige des dégâts! Trouvez un abri!");
            }
            if (type.buffZombies()) {
                player.sendMessage("§c⚠ Les zombies sont renforcés pendant cette météo!");
            }
            if (type.isBeneficial()) {
                player.sendMessage("§a✓ Cette météo est favorable aux survivants!");
            }

            player.sendMessage("§7Durée estimée: §e" + formatTime(duration / 20));
            player.sendMessage("");
        }
    }

    /**
     * Annonce la fin naturelle de l'effet
     */
    protected void announceEnd() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle(
                "§a§l☀ Météo Terminée",
                "§7" + type.getDisplayName() + " se dissipe...",
                10, 40, 20
            );

            player.sendMessage("");
            player.sendMessage("§a§l☀ §7La météo §e" + type.getDisplayName() + " §7s'est dissipée.");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }

    /**
     * Annonce l'interruption de l'effet
     */
    protected void announceInterruption() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage("§7La météo §e" + type.getDisplayName() + " §7a été interrompue.");
        }
    }

    /**
     * Obtient la liste des joueurs affectés par cette météo
     */
    public Collection<Player> getAffectedPlayers() {
        if (targetWorld == null) {
            return plugin.getServer().getOnlinePlayers();
        }

        List<Player> players = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Obtient le multiplicateur de spawn effectif
     */
    public double getEffectiveSpawnMultiplier() {
        if (spawnMultiplierOverride > 0) {
            return spawnMultiplierOverride;
        }
        return type.getSpawnMultiplier();
    }

    /**
     * Obtient le multiplicateur de dégâts zombies effectif
     */
    public double getEffectiveZombieDamageMultiplier() {
        return type.getZombieDamageMultiplier();
    }

    /**
     * Obtient le multiplicateur de vitesse zombies effectif
     */
    public double getEffectiveZombieSpeedMultiplier() {
        return type.getZombieSpeedMultiplier();
    }

    /**
     * Vérifie si l'effet est toujours actif et valide
     */
    public boolean isValid() {
        return active && !completed && !cancelled;
    }

    /**
     * Obtient le temps restant en secondes
     */
    public int getRemainingTimeSeconds() {
        int remainingTicks = Math.max(0, duration - elapsedTicks);
        return remainingTicks / 20;
    }

    /**
     * Obtient le pourcentage de temps restant
     */
    public double getRemainingTimePercent() {
        return Math.max(0, 1.0 - ((double) elapsedTicks / duration));
    }

    /**
     * Obtient les informations de debug
     */
    public String getDebugInfo() {
        return String.format(
            "[%s] %s | Remaining: %ds | Damage: %d | Sheltered: %d",
            id, type.getDisplayName(),
            getRemainingTimeSeconds(), totalDamageDealt,
            shelteredPlayers.size()
        );
    }

    /**
     * Obtient un résumé pour l'affichage
     */
    public String getSummary() {
        return type.getColor() + type.getIcon() + " " + type.getDisplayName() +
            " §7[" + formatTime(getRemainingTimeSeconds()) + "]";
    }
}
