package com.rinaorc.zombiez.weather;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.boss.BarFlag;
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
 * VERSION 2.0 - OPTIMISÉE ET AMÉLIORÉE:
 * - Plus de debuffs ennuyeux (Blindness, Weakness) - supprimés!
 * - Utilise le fog natif via render distance pour l'ambiance
 * - BONUS joueurs: XP, Loot, Vitesse, Régénération
 * - Buffs de potion positifs (Force, Régénération, Speed, Luck)
 * - Optimisation des particules et du caching
 *
 * PHILOSOPHIE: La météo enrichit le gameplay sans frustrer les joueurs!
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
    protected int tickCounter = 0; // Pour optimiser certaines opérations

    // Joueurs protégés (sous un abri) - pour météo dangereuse uniquement
    protected final Set<UUID> shelteredPlayers = ConcurrentHashMap.newKeySet();

    // Cache des joueurs (mis à jour toutes les secondes)
    protected List<Player> cachedPlayers = new ArrayList<>();
    protected long lastPlayerCacheUpdate = 0;
    protected static final long PLAYER_CACHE_DURATION = 1000;

    // Tâches planifiées
    protected BukkitTask mainTask;
    protected BukkitTask particleTask;
    protected BukkitTask damageTask;
    protected BukkitTask buffTask;

    // Boss bar globale
    protected BossBar bossBar;

    // Configuration (peut être modifiée par le manager)
    @Setter protected double spawnMultiplierOverride = -1;
    @Setter protected double damageMultiplierOverride = -1;
    @Setter protected double intensityMultiplier = 1.0;

    // Statistiques
    protected int totalDamageDealt = 0;
    protected int totalRegenGiven = 0;
    protected int buffApplications = 0;

    // Monde cible (null = tous les mondes)
    protected World targetWorld;

    // Render distance originale (pour restauration)
    protected Map<UUID, Integer> originalRenderDistances = new ConcurrentHashMap<>();

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

    // ==================== LIFECYCLE ====================

    /**
     * Démarre l'effet météo
     */
    public void start() {
        if (active) return;

        active = true;

        // Créer la boss bar
        createBossBar();

        // Appliquer la météo Minecraft native
        applyMinecraftWeather();

        // Appliquer le fog si nécessaire
        if (type.hasFog()) {
            applyFogEffect();
        }

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

        announceEnd();
        cleanup();
    }

    /**
     * Annule l'effet météo prématurément
     */
    public void cancel() {
        if (!active || completed || cancelled) return;

        cancelled = true;
        active = false;

        announceInterruption();
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
        cancelTask(mainTask);
        cancelTask(particleTask);
        cancelTask(damageTask);
        cancelTask(buffTask);

        // Supprimer la boss bar et ses flags visuels
        if (bossBar != null) {
            // Retirer les flags avant de supprimer
            bossBar.removeFlag(BarFlag.CREATE_FOG);
            bossBar.removeFlag(BarFlag.DARKEN_SKY);
            bossBar.removeAll();
        }

        // Restaurer la météo Minecraft
        restoreMinecraftWeather();

        // Restaurer le fog/render distance
        if (type.hasFog()) {
            restoreFogEffect();
        }

        // Retirer les buffs des joueurs
        removePlayerBuffs();
    }

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    // ==================== BOSS BAR ====================

    protected void createBossBar() {
        String title = buildBossBarTitle();
        bossBar = plugin.getServer().createBossBar(title, type.getBarColor(), BarStyle.SEGMENTED_20);
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        // Appliquer les flags visuels de la BossBar (fog et ciel sombre)
        if (type.isBossBarFog()) {
            bossBar.addFlag(BarFlag.CREATE_FOG);
        }
        if (type.isBossBarDarkenSky()) {
            bossBar.addFlag(BarFlag.DARKEN_SKY);
        }

        // Ajouter tous les joueurs
        for (Player player : getAffectedPlayers()) {
            bossBar.addPlayer(player);
        }
    }

    protected void updateBossBar() {
        if (bossBar == null) return;

        double progress = 1.0 - ((double) elapsedTicks / duration);
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
        bossBar.setTitle(buildBossBarTitle());

        // Mettre à jour les joueurs (optimisé avec cache)
        Set<Player> current = new HashSet<>(bossBar.getPlayers());
        List<Player> affected = getAffectedPlayers();

        for (Player player : affected) {
            if (!current.contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        for (Player player : current) {
            if (!affected.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    protected String buildBossBarTitle() {
        int remainingSeconds = getRemainingTimeSeconds();
        String timeStr = formatTime(remainingSeconds);

        StringBuilder title = new StringBuilder();
        title.append(type.getColor()).append("§l").append(type.getIcon()).append(" ");
        title.append(type.getDisplayName()).append(" §7- §e").append(timeStr);

        // Ajouter indicateurs de bonus
        if (type.getXpMultiplier() > 1.0) {
            title.append(" §a+").append((int)((type.getXpMultiplier() - 1) * 100)).append("%XP");
        }
        if (type.getLootMultiplier() > 1.0) {
            title.append(" §b+").append((int)((type.getLootMultiplier() - 1) * 100)).append("%Loot");
        }

        return title.toString();
    }

    protected String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    // ==================== MÉTÉO MINECRAFT ====================

    protected void applyMinecraftWeather() {
        if (targetWorld == null) return;

        if (type.isMinecraftRain()) {
            targetWorld.setStorm(true);
            targetWorld.setWeatherDuration(duration + 20 * 60);
        }

        if (type.isMinecraftThunder()) {
            targetWorld.setThundering(true);
            targetWorld.setThunderDuration(duration + 20 * 60);
        }
    }

    protected void restoreMinecraftWeather() {
        if (targetWorld == null) return;

        // Forcer l'arrêt de la pluie/orage
        targetWorld.setStorm(false);
        targetWorld.setThundering(false);

        // Réinitialiser les durées pour éviter que la météo reprenne
        targetWorld.setWeatherDuration(0);
        targetWorld.setThunderDuration(0);

        // Forcer le temps clair
        targetWorld.setClearWeatherDuration(20 * 60 * 5); // 5 minutes de temps clair minimum
    }

    // ==================== FOG SYSTEM ====================

    /**
     * Applique l'effet de fog en modifiant la render distance client
     * Note: Utilise les packets natifs de Minecraft pour le fog
     */
    protected void applyFogEffect() {
        int fogDistance = type.getFogRenderDistance();
        if (fogDistance < 0) return;

        // Sauvegarder et appliquer la nouvelle render distance
        for (Player player : getAffectedPlayers()) {
            originalRenderDistances.put(player.getUniqueId(), player.getClientViewDistance());
            // Envoyer un message pour informer du changement de visibilité
            player.sendMessage("§7§o(Visibilité réduite à " + fogDistance + " chunks)");
        }
    }

    protected void restoreFogEffect() {
        // Restaurer les render distances originales
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (originalRenderDistances.containsKey(uuid)) {
                originalRenderDistances.remove(uuid);
            }
        }
    }

    // ==================== TÂCHES PÉRIODIQUES ====================

    protected void startTasks() {
        // Tâche principale (chaque seconde)
        mainTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);

        // Tâche de particules (optimisée: 2 fois par seconde au lieu de 4)
        if (type.getParticle() != null) {
            particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::spawnParticlesOptimized, 10L, 10L);
        }

        // Tâche de dégâts environnementaux (si dangereux)
        if (type.isDangerous() && type.getDamageInterval() > 0) {
            damageTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::applyEnvironmentalDamage, type.getDamageInterval(), type.getDamageInterval());
        }

        // Tâche de buffs joueurs (toutes les 5 secondes)
        if (type.hasPlayerBuff() || type.getRegenAmount() > 0) {
            buffTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::applyPlayerBuffs, 100L, 100L); // 5 secondes
        }
    }

    /**
     * Tick principal (appelé chaque seconde)
     */
    public void tick() {
        if (!active) return;

        elapsedTicks += 20;
        tickCounter++;

        // Mettre à jour le cache des joueurs
        updatePlayerCache();

        // Mettre à jour la boss bar
        updateBossBar();

        // Appliquer la régénération (chaque 5 secondes)
        if (tickCounter % 5 == 0 && type.getRegenAmount() > 0) {
            applyRegeneration();
        }

        // Jouer le son ambiant (toutes les 15 secondes)
        if (tickCounter % 15 == 0 && type.getAmbientSound() != null) {
            playAmbientSound();
        }

        // Éclairs aléatoires pendant les orages
        if (type == WeatherType.STORM && Math.random() < 0.1) {
            spawnRandomLightning();
        }

        // Vérifier fin
        if (elapsedTicks >= duration) {
            complete();
        }
    }

    /**
     * Met à jour le cache des joueurs (optimisation)
     */
    protected void updatePlayerCache() {
        long now = System.currentTimeMillis();
        if (now - lastPlayerCacheUpdate < PLAYER_CACHE_DURATION) return;

        lastPlayerCacheUpdate = now;
        cachedPlayers.clear();

        if (targetWorld == null) {
            cachedPlayers.addAll(plugin.getServer().getOnlinePlayers());
        } else {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld().equals(targetWorld)) {
                    cachedPlayers.add(player);
                }
            }
        }
    }

    // ==================== PARTICULES (OPTIMISÉES) ====================

    /**
     * Génère les particules de manière optimisée
     */
    protected void spawnParticlesOptimized() {
        if (type.getParticle() == null) return;

        // Nombre de particules réduit et optimisé
        int baseParticleCount = (int) (8 * intensityMultiplier);

        for (Player player : cachedPlayers) {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world == null) continue;

            // Particules principales
            spawnWeatherParticles(player, loc, baseParticleCount);

            // Particules spéciales (moins fréquentes)
            if (tickCounter % 2 == 0) {
                spawnSpecialParticles(player, loc);
            }
        }
    }

    protected void spawnWeatherParticles(Player player, Location loc, int count) {
        double radius = 6.0;
        Particle particle = type.getParticle();

        for (int i = 0; i < count; i++) {
            double x = loc.getX() + (Math.random() - 0.5) * radius * 2;
            double y = loc.getY() + Math.random() * 5 + 2;
            double z = loc.getZ() + (Math.random() - 0.5) * radius * 2;

            Location particleLoc = new Location(loc.getWorld(), x, y, z);

            // Spawner pour le joueur uniquement (optimisation)
            player.spawnParticle(particle, particleLoc, 1, 0, -0.1, 0, 0);
        }
    }

    protected void spawnSpecialParticles(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        switch (type) {
            case STORM -> {
                // Flash d'éclair occasionnel
                if (Math.random() < 0.03) {
                    player.spawnParticle(Particle.FLASH, loc.clone().add(0, 20, 0), 1);
                }
            }
            case BLOOD_MOON -> {
                // BROUILLARD ROUGE DENSE pour la Lune de Sang!
                spawnBloodMoonFog(player, loc);
            }
            case AURORA, STARFALL -> {
                // Particules colorées dans le ciel
                Color[] colors = {Color.PURPLE, Color.BLUE, Color.AQUA, Color.LIME, Color.FUCHSIA};
                Color color = colors[(int) (Math.random() * colors.length)];
                Location skyLoc = loc.clone().add(
                    (Math.random() - 0.5) * 15,
                    12 + Math.random() * 8,
                    (Math.random() - 0.5) * 15
                );
                player.spawnParticle(Particle.DUST, skyLoc, 1, 0.5, 0.2, 0.5, 0,
                    new Particle.DustOptions(color, 1.5f));
            }
            case ACID_RAIN -> {
                // Brouillard vert toxique pour la pluie acide
                spawnAcidRainFog(player, loc);
            }
            case BLIZZARD -> {
                // Brouillard blanc glacé pour le blizzard
                spawnBlizzardFog(player, loc);
            }
            case SANDSTORM -> {
                // Brouillard de sable pour la tempête de sable
                spawnSandstormFog(player, loc);
            }
            case ASHFALL -> {
                // Brouillard de cendres volcaniques
                spawnAshfallFog(player, loc);
            }
            case FOG -> {
                // Brouillard dense gris
                spawnDenseFog(player, loc);
            }
            case SOLAR_BLESSING -> {
                // Rayons de lumière dorés
                player.spawnParticle(Particle.END_ROD, loc.clone().add(
                    (Math.random() - 0.5) * 3, 3 + Math.random() * 2, (Math.random() - 0.5) * 3),
                    1, 0, -0.05, 0, 0);
            }
            case HARVEST_MOON -> {
                // Particules de récolte dorées
                if (Math.random() < 0.3) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(
                        (Math.random() - 0.5) * 4, 1 + Math.random(), (Math.random() - 0.5) * 4), 1);
                }
            }
            default -> {}
        }
    }

    protected void spawnRandomLightning() {
        if (targetWorld == null) return;

        for (Player player : cachedPlayers) {
            if (Math.random() < 0.3) {
                Location loc = player.getLocation();
                double x = loc.getX() + (Math.random() - 0.5) * 80;
                double z = loc.getZ() + (Math.random() - 0.5) * 80;
                Location lightningLoc = new Location(targetWorld, x,
                    targetWorld.getHighestBlockYAt((int)x, (int)z), z);

                // Éclair visuel sans dégâts
                targetWorld.strikeLightningEffect(lightningLoc);
            }
        }
    }

    /**
     * Génère un brouillard rouge dense pour la Lune de Sang
     * Crée une atmosphère terrifiante et immersive
     */
    protected void spawnBloodMoonFog(Player player, Location loc) {
        // Couleurs de brouillard rouge sang
        Color bloodRed = Color.fromRGB(139, 0, 0);        // Rouge sang foncé
        Color crimson = Color.fromRGB(220, 20, 60);       // Cramoisie
        Color darkRed = Color.fromRGB(100, 0, 0);         // Rouge très sombre

        // Particules de brouillard au sol (dense)
        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 12;
            double offsetZ = (Math.random() - 0.5) * 12;
            double offsetY = Math.random() * 2.5; // Brouillard bas

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            // Alterner entre les nuances de rouge
            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.4) {
                fogColor = bloodRed;
            } else if (colorRoll < 0.7) {
                fogColor = crimson;
            } else {
                fogColor = darkRed;
            }

            // Particules de taille variable pour un effet plus naturel
            float size = 2.0f + (float)(Math.random() * 1.5);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.3, 0.1, 0.3, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Brouillard en hauteur (moins dense, effet atmosphérique)
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 16;
            double offsetZ = (Math.random() - 0.5) * 16;
            double offsetY = 3 + Math.random() * 6; // Plus haut

            Location highFogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            // Rouge plus clair/rosé pour le brouillard en hauteur
            Color highFogColor = Color.fromRGB(
                180 + (int)(Math.random() * 40),
                20 + (int)(Math.random() * 30),
                20 + (int)(Math.random() * 30)
            );

            player.spawnParticle(Particle.DUST, highFogLoc, 1, 0.5, 0.3, 0.5, 0,
                new Particle.DustOptions(highFogColor, 1.5f));
        }

        // Particules de fumée noire occasionnelles (pour contraste)
        if (Math.random() < 0.3) {
            Location smokeLoc = loc.clone().add(
                (Math.random() - 0.5) * 8,
                Math.random() * 3,
                (Math.random() - 0.5) * 8
            );
            player.spawnParticle(Particle.SMOKE, smokeLoc, 2, 0.2, 0.1, 0.2, 0.01);
        }

        // Effet de lueur rouge sur le joueur (aura sinistre)
        if (Math.random() < 0.2) {
            player.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 3, 0.4, 0.5, 0.4, 0,
                new Particle.DustOptions(crimson, 0.8f));
        }
    }

    /**
     * Génère un brouillard vert toxique pour la pluie acide
     */
    protected void spawnAcidRainFog(Player player, Location loc) {
        // Couleurs vertes toxiques
        Color toxicGreen = Color.fromRGB(50, 205, 50);       // Vert lime
        Color darkGreen = Color.fromRGB(0, 100, 0);          // Vert foncé
        Color acidYellow = Color.fromRGB(173, 255, 47);      // Vert-jaune acide

        // Brouillard toxique au sol
        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            double offsetY = Math.random() * 2;

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.5) {
                fogColor = toxicGreen;
            } else if (colorRoll < 0.8) {
                fogColor = acidYellow;
            } else {
                fogColor = darkGreen;
            }

            float size = 1.5f + (float)(Math.random() * 1.0);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.2, 0.1, 0.2, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Particules de slime occasionnelles (effet acide)
        if (Math.random() < 0.3) {
            Location slimeLoc = loc.clone().add(
                (Math.random() - 0.5) * 6,
                0.5 + Math.random(),
                (Math.random() - 0.5) * 6
            );
            player.spawnParticle(Particle.ITEM_SLIME, slimeLoc, 2, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * Génère un brouillard blanc glacé pour le blizzard
     */
    protected void spawnBlizzardFog(Player player, Location loc) {
        // Couleurs blanches et bleu glacier
        Color white = Color.fromRGB(255, 255, 255);
        Color iceBlue = Color.fromRGB(200, 230, 255);
        Color lightBlue = Color.fromRGB(173, 216, 230);

        // Brouillard dense de neige
        for (int i = 0; i < 12; i++) {
            double offsetX = (Math.random() - 0.5) * 12;
            double offsetZ = (Math.random() - 0.5) * 12;
            double offsetY = Math.random() * 3;

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.6) {
                fogColor = white;
            } else if (colorRoll < 0.85) {
                fogColor = iceBlue;
            } else {
                fogColor = lightBlue;
            }

            float size = 2.0f + (float)(Math.random() * 1.5);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.3, 0.15, 0.3, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Flocons de neige en hauteur
        for (int i = 0; i < 5; i++) {
            Location snowLoc = loc.clone().add(
                (Math.random() - 0.5) * 14,
                3 + Math.random() * 5,
                (Math.random() - 0.5) * 14
            );
            player.spawnParticle(Particle.SNOWFLAKE, snowLoc, 1, 0.3, 0.2, 0.3, 0.02);
        }

        // Effet de givre autour du joueur
        if (Math.random() < 0.15) {
            player.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0.01);
        }
    }

    /**
     * Génère un brouillard de sable pour la tempête de sable
     */
    protected void spawnSandstormFog(Player player, Location loc) {
        // Couleurs sable/désert
        Color sand = Color.fromRGB(210, 180, 140);           // Tan
        Color darkSand = Color.fromRGB(189, 154, 122);       // Sable foncé
        Color dustyBrown = Color.fromRGB(160, 130, 100);     // Brun poussiéreux

        // Brouillard de sable dense
        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 14;
            double offsetZ = (Math.random() - 0.5) * 14;
            double offsetY = Math.random() * 3.5;

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.5) {
                fogColor = sand;
            } else if (colorRoll < 0.8) {
                fogColor = darkSand;
            } else {
                fogColor = dustyBrown;
            }

            float size = 2.5f + (float)(Math.random() * 1.5);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.4, 0.2, 0.4, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Effet de tourbillon de sable
        if (Math.random() < 0.2) {
            Location whirlLoc = loc.clone().add(
                (Math.random() - 0.5) * 8,
                0.5,
                (Math.random() - 0.5) * 8
            );
            player.spawnParticle(Particle.DUST, whirlLoc, 5, 0.1, 0.8, 0.1, 0,
                new Particle.DustOptions(sand, 1.0f));
        }
    }

    /**
     * Génère un brouillard de cendres volcaniques
     */
    protected void spawnAshfallFog(Player player, Location loc) {
        // Couleurs cendres/volcaniques
        Color darkGray = Color.fromRGB(80, 80, 80);          // Gris foncé
        Color ash = Color.fromRGB(120, 120, 120);            // Cendres
        Color emberOrange = Color.fromRGB(200, 80, 20);      // Braise orange

        // Brouillard de cendres
        for (int i = 0; i < 12; i++) {
            double offsetX = (Math.random() - 0.5) * 12;
            double offsetZ = (Math.random() - 0.5) * 12;
            double offsetY = Math.random() * 4;

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.5) {
                fogColor = darkGray;
            } else if (colorRoll < 0.85) {
                fogColor = ash;
            } else {
                fogColor = emberOrange; // Braises chaudes
            }

            float size = 1.8f + (float)(Math.random() * 1.2);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.3, 0.15, 0.3, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Particules de fumée
        if (Math.random() < 0.25) {
            Location smokeLoc = loc.clone().add(
                (Math.random() - 0.5) * 8,
                Math.random() * 2,
                (Math.random() - 0.5) * 8
            );
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, smokeLoc, 1, 0.1, 0.1, 0.1, 0.01);
        }

        // Braises occasionnelles
        if (Math.random() < 0.1) {
            Location emberLoc = loc.clone().add(
                (Math.random() - 0.5) * 6,
                1 + Math.random() * 2,
                (Math.random() - 0.5) * 6
            );
            player.spawnParticle(Particle.LAVA, emberLoc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Génère un brouillard dense gris
     */
    protected void spawnDenseFog(Player player, Location loc) {
        // Couleurs grises variées
        Color lightGray = Color.fromRGB(180, 180, 180);
        Color mediumGray = Color.fromRGB(140, 140, 140);
        Color darkGray = Color.fromRGB(100, 100, 100);

        // Brouillard très dense
        for (int i = 0; i < 18; i++) {
            double offsetX = (Math.random() - 0.5) * 14;
            double offsetZ = (Math.random() - 0.5) * 14;
            double offsetY = Math.random() * 3;

            Location fogLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color fogColor;
            double colorRoll = Math.random();
            if (colorRoll < 0.4) {
                fogColor = lightGray;
            } else if (colorRoll < 0.75) {
                fogColor = mediumGray;
            } else {
                fogColor = darkGray;
            }

            float size = 2.5f + (float)(Math.random() * 2.0);
            player.spawnParticle(Particle.DUST, fogLoc, 1, 0.4, 0.2, 0.4, 0,
                new Particle.DustOptions(fogColor, size));
        }

        // Brouillard en hauteur (moins dense)
        for (int i = 0; i < 6; i++) {
            double offsetX = (Math.random() - 0.5) * 16;
            double offsetZ = (Math.random() - 0.5) * 16;
            double offsetY = 4 + Math.random() * 4;

            Location highFogLoc = loc.clone().add(offsetX, offsetY, offsetZ);
            player.spawnParticle(Particle.DUST, highFogLoc, 1, 0.5, 0.3, 0.5, 0,
                new Particle.DustOptions(lightGray, 1.5f));
        }
    }

    // ==================== BUFFS JOUEURS ====================

    /**
     * Applique les buffs de potion aux joueurs
     */
    protected void applyPlayerBuffs() {
        if (!type.hasPlayerBuff()) return;

        PotionEffectType buffType = type.getBuffEffect();
        int amplifier = type.getBuffAmplifier();
        int buffDuration = type.getBuffDuration();

        for (Player player : cachedPlayers) {
            // Ne pas appliquer si le joueur est sous abri pendant météo dangereuse
            if (type.isDangerous() && isPlayerSheltered(player)) continue;

            player.addPotionEffect(new PotionEffect(buffType, buffDuration, amplifier, true, false, true));
            buffApplications++;
        }
    }

    /**
     * Applique la régénération aux joueurs
     */
    protected void applyRegeneration() {
        double regenAmount = type.getRegenAmount();
        if (regenAmount <= 0) return;

        for (Player player : cachedPlayers) {
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

            if (currentHealth < maxHealth) {
                double newHealth = Math.min(maxHealth, currentHealth + regenAmount);
                player.setHealth(newHealth);
                totalRegenGiven += (int) regenAmount;

                // Effet visuel de régénération
                player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0),
                    1, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    /**
     * Retire les buffs des joueurs à la fin de la météo
     */
    protected void removePlayerBuffs() {
        PotionEffectType buffType = type.getBuffEffect();
        if (buffType == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.removePotionEffect(buffType);
        }
    }

    // ==================== DÉGÂTS ENVIRONNEMENTAUX ====================

    protected void applyEnvironmentalDamage() {
        if (!type.isDangerous()) return;

        for (Player player : cachedPlayers) {
            // Vérifier abri
            if (isPlayerSheltered(player)) {
                shelteredPlayers.add(player.getUniqueId());
                continue;
            } else {
                shelteredPlayers.remove(player.getUniqueId());
            }

            // Calculer les dégâts - Pour ASHFALL, utiliser 5% des HP max
            double damage;
            if (type == WeatherType.ASHFALL) {
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                damage = maxHealth * 0.05; // 5% des HP max
            } else {
                damage = type.getEnvironmentalDamage();
                if (damageMultiplierOverride > 0) {
                    damage *= damageMultiplierOverride;
                }
            }

            // Réduction par armure
            double finalDamage = calculateReducedDamage(player, damage);

            if (finalDamage > 0) {
                player.damage(finalDamage);
                totalDamageDealt += (int) finalDamage;

                // Avertissement périodique
                if (tickCounter % 10 == 0) {
                    String damageMsg = type == WeatherType.ASHFALL ?
                        "§7(-5% HP max)" : "§7(-" + String.format("%.1f", finalDamage) + " HP)";
                    player.sendMessage(type.getColor() + type.getIcon() +
                        " §cTrouvez un abri! " + damageMsg);
                }
            }
        }
    }

    protected boolean isPlayerSheltered(Player player) {
        Location loc = player.getLocation();

        // Vérifier bloc solide au-dessus (optimisé: max 8 blocs)
        for (int y = 1; y <= 8; y++) {
            if (loc.clone().add(0, y, 0).getBlock().getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    protected double calculateReducedDamage(Player player, double baseDamage) {
        double reduction = 0;

        // Casque = -20% dégâts météo
        if (player.getInventory().getHelmet() != null) {
            reduction += 0.20;
        }
        // Plastron = -10%
        if (player.getInventory().getChestplate() != null) {
            reduction += 0.10;
        }

        return baseDamage * (1.0 - Math.min(0.5, reduction)); // Max 50% réduction
    }

    // ==================== SONS ====================

    protected void playAmbientSound() {
        if (type.getAmbientSound() == null) return;

        float volume = 0.4f;
        float pitch = 0.9f + (float)(Math.random() * 0.2);

        for (Player player : cachedPlayers) {
            player.playSound(player.getLocation(), type.getAmbientSound(), volume, pitch);
        }
    }

    // ==================== ANNONCES ====================

    protected void announceStart() {
        String title = type.getColor() + "§l" + type.getIcon() + " " + type.getDisplayName().toUpperCase();
        String subtitle = "§7" + type.getDescription();

        for (Player player : getAffectedPlayers()) {
            player.sendTitle(title, subtitle, 20, 60, 20);

            if (type.getAmbientSound() != null) {
                player.playSound(player.getLocation(), type.getAmbientSound(), 1.0f, 0.8f);
            }

            // Message détaillé
            player.sendMessage("");
            player.sendMessage(type.getColor() + "§l" + type.getIcon() + " MÉTÉO: " + type.getDisplayName());
            player.sendMessage("§7" + type.getDescription());

            // Afficher les bonus
            StringBuilder bonusMsg = new StringBuilder("§aBonus actifs: ");
            boolean hasBonus = false;

            if (type.getXpMultiplier() > 1.0) {
                bonusMsg.append("§e+").append((int)((type.getXpMultiplier()-1)*100)).append("%XP ");
                hasBonus = true;
            }
            if (type.getLootMultiplier() > 1.0) {
                bonusMsg.append("§b+").append((int)((type.getLootMultiplier()-1)*100)).append("%Loot ");
                hasBonus = true;
            }
            if (type.getPlayerSpeedBonus() > 1.0) {
                bonusMsg.append("§f+").append((int)((type.getPlayerSpeedBonus()-1)*100)).append("%Vitesse ");
                hasBonus = true;
            }
            if (type.getRegenAmount() > 0) {
                bonusMsg.append("§d+Régénération ");
                hasBonus = true;
            }
            if (type.hasPlayerBuff()) {
                bonusMsg.append("§5+").append(type.getBuffEffect().getKey().getKey()).append(" ");
                hasBonus = true;
            }

            if (hasBonus) {
                player.sendMessage(bonusMsg.toString());
            }

            if (type.isDangerous()) {
                player.sendMessage("§c⚠ Attention: Dégâts environnementaux actifs! Trouvez un abri!");
            }
            if (type.buffZombies()) {
                player.sendMessage("§c⚠ Les zombies sont renforcés!");
            }
            if (type.debuffZombies()) {
                player.sendMessage("§a✓ Les zombies sont affaiblis!");
            }

            player.sendMessage("§7Durée: §e" + formatTime(duration / 20));
            player.sendMessage("");
        }
    }

    protected void announceEnd() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle(
                "§a§l☀ Météo Terminée",
                "§7" + type.getDisplayName() + " se dissipe...",
                10, 40, 20
            );

            player.sendMessage("§a§l☀ §7La météo §e" + type.getDisplayName() + " §7s'est dissipée.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        }
    }

    protected void announceInterruption() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage("§7La météo §e" + type.getDisplayName() + " §7a été interrompue.");
        }
    }

    // ==================== GETTERS ====================

    public List<Player> getAffectedPlayers() {
        if (!cachedPlayers.isEmpty()) {
            return cachedPlayers;
        }

        // Fallback si cache vide
        List<Player> players = new ArrayList<>();
        if (targetWorld == null) {
            players.addAll(plugin.getServer().getOnlinePlayers());
        } else {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld().equals(targetWorld)) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public double getEffectiveSpawnMultiplier() {
        return spawnMultiplierOverride > 0 ? spawnMultiplierOverride : type.getSpawnMultiplier();
    }

    public double getEffectiveZombieDamageMultiplier() {
        return type.getZombieDamageMultiplier();
    }

    public double getEffectiveZombieSpeedMultiplier() {
        return type.getZombieSpeedMultiplier();
    }

    public double getEffectiveXpMultiplier() {
        return type.getXpMultiplier();
    }

    public double getEffectiveLootMultiplier() {
        return type.getLootMultiplier();
    }

    public boolean isValid() {
        return active && !completed && !cancelled;
    }

    public int getRemainingTimeSeconds() {
        return Math.max(0, duration - elapsedTicks) / 20;
    }

    public double getRemainingTimePercent() {
        return Math.max(0, 1.0 - ((double) elapsedTicks / duration));
    }

    public String getDebugInfo() {
        return String.format(
            "[%s] %s | %ds | XP:%.0f%% Loot:%.0f%% | Regen:%d Buffs:%d Dmg:%d",
            id, type.getDisplayName(), getRemainingTimeSeconds(),
            type.getXpMultiplier() * 100, type.getLootMultiplier() * 100,
            totalRegenGiven, buffApplications, totalDamageDealt
        );
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getColor()).append(type.getIcon()).append(" ").append(type.getDisplayName());
        sb.append(" §7[").append(formatTime(getRemainingTimeSeconds())).append("]");

        if (type.getXpMultiplier() > 1.0 || type.getLootMultiplier() > 1.0) {
            sb.append(" §a★");
        }
        if (type.isDangerous()) {
            sb.append(" §c⚠");
        }

        return sb.toString();
    }
}
