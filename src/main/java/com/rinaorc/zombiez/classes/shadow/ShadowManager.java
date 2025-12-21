package com.rinaorc.zombiez.classes.shadow;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager pour le système de la Branche Ombre du Chasseur.
 * Gère les Points d'Ombre, les Marques, les Clones et toutes les mécaniques.
 *
 * Inspiré du Rogue Diablo 4 (Combo Points) et Demon Hunter D3 (Shadow Set).
 */
public class ShadowManager {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // Team pour le glowing violet
    private static final String DEATH_MARK_TEAM_NAME = "death_mark_purple";
    private Team deathMarkTeam;

    // === CONSTANTES ===
    public static final int MAX_SHADOW_POINTS = 5;
    public static final long SHADOW_STEP_COOLDOWN = 5000; // 5s
    public static final long MARK_DURATION = 8000; // 8s
    public static final long DANCE_MACABRE_INVIS_DURATION = 40; // 2s en ticks
    public static final long CLONE_DURATION = 10000; // 10s
    public static final long AVATAR_DURATION = 15000; // 15s
    public static final long AVATAR_COOLDOWN = 60000; // 60s

    // === TRACKING DES JOUEURS ===

    // Points d'Ombre (0-5)
    private final Map<UUID, Integer> shadowPoints = new ConcurrentHashMap<>();

    // Cooldown du Pas de l'Ombre
    private final Map<UUID, Long> shadowStepCooldown = new ConcurrentHashMap<>();

    // Marques Mortelles (target UUID -> expiration time)
    private final Map<UUID, Long> deathMarks = new ConcurrentHashMap<>();
    // Qui a marqué qui (target UUID -> player UUID)
    private final Map<UUID, UUID> markOwners = new ConcurrentHashMap<>();

    // Poison stacks (target UUID -> stacks count)
    private final Map<UUID, Integer> poisonStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> poisonExpiry = new ConcurrentHashMap<>();

    // Avatar des Ombres (player UUID -> end time)
    private final Map<UUID, Long> avatarActive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> avatarCooldown = new ConcurrentHashMap<>();

    // Danse Macabre - tracking frénésie et exécution préparée
    private final Map<UUID, Long> danceMacabreEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Long> preparedExecutionEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> preparedExecutionCost = new ConcurrentHashMap<>();

    // Clé pour l'AttributeModifier de vitesse d'attaque Danse Macabre
    private static final NamespacedKey DANSE_ATTACK_SPEED_KEY =
        NamespacedKey.fromString("zombiez:danse_macabre_attack_speed");

    // Cache des joueurs Ombre actifs
    private final Set<UUID> activeShadowPlayers = ConcurrentHashMap.newKeySet();

    // === LAMES SPECTRALES (Style Vampire Survivors) ===

    // Données des lames actives par joueur
    private final Map<UUID, SpectralBladesData> activeBlades = new ConcurrentHashMap<>();

    // Cooldown de frappe par cible (targetUUID -> dernière frappe timestamp)
    private final Map<UUID, Map<UUID, Long>> bladeHitCooldowns = new ConcurrentHashMap<>();

    // Constantes des lames
    private static final long BLADE_HIT_COOLDOWN_MS = 500; // 0.5s entre chaque frappe sur même cible
    private static final double BLADE_HIT_RADIUS = 1.2; // Rayon de hitbox de chaque lame

    /**
     * Données des Lames Spectrales d'un joueur
     */
    private static class SpectralBladesData {
        final UUID ownerUuid;
        final long startTime;
        final long duration;
        final int bladeCount;
        final double orbitRadius;
        final double damagePercent;
        final long rotationPeriod;
        double currentAngle = 0;

        SpectralBladesData(UUID owner, long duration, int bladeCount, double orbitRadius,
                          double damagePercent, long rotationPeriod) {
            this.ownerUuid = owner;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.bladeCount = bladeCount;
            this.orbitRadius = orbitRadius;
            this.damagePercent = damagePercent;
            this.rotationPeriod = rotationPeriod;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > startTime + duration;
        }

        long getRemainingTime() {
            return Math.max(0, (startTime + duration) - System.currentTimeMillis());
        }
    }

    public ShadowManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        initDeathMarkTeam();
        startTasks();
    }

    /**
     * Initialise la team pour le glowing violet des marques mortelles
     */
    private void initDeathMarkTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        deathMarkTeam = scoreboard.getTeam(DEATH_MARK_TEAM_NAME);

        if (deathMarkTeam == null) {
            deathMarkTeam = scoreboard.registerNewTeam(DEATH_MARK_TEAM_NAME);
        }

        // Couleur violette pour le glowing
        deathMarkTeam.color(NamedTextColor.DARK_PURPLE);
        // Ne pas afficher le préfixe de couleur dans le nom
        deathMarkTeam.prefix(Component.empty());
        deathMarkTeam.suffix(Component.empty());
        // Garder les noms visibles (ne pas cacher le displayname des mobs)
        deathMarkTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    /**
     * Démarre les tâches périodiques (cleanup, regeneration, ActionBar)
     */
    private void startTasks() {
        // Tâche ActionBar + cleanup (toutes les 5 ticks = 250ms)
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup des marques expirées
                deathMarks.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        UUID targetUuid = entry.getKey();
                        Entity entity = Bukkit.getEntity(targetUuid);
                        if (entity instanceof LivingEntity living) {
                            living.setGlowing(false);
                            // Retirer de la team violet
                            if (deathMarkTeam != null) {
                                deathMarkTeam.removeEntity(living);
                            }
                        }
                        markOwners.remove(targetUuid);
                        return true;
                    }
                    return false;
                });

                // Cleanup poison expiré
                poisonExpiry.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        poisonStacks.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });

                // Cleanup Danse Macabre et effets associés
                danceMacabreEnd.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        // Retirer le buff de vitesse d'attaque
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            removeDanseAttackSpeedBonus(player);
                        }
                        return true;
                    }
                    return false;
                });

                // Cleanup Exécution Préparée
                preparedExecutionEnd.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        preparedExecutionCost.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });

                // Régénération Avatar (1 point/s)
                avatarActive.forEach((uuid, endTime) -> {
                    if (now < endTime) {
                        addShadowPoints(uuid, 1);
                    }
                });

                // Cleanup Avatar expiré
                avatarActive.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        // Supprimer les lames d'Avatar
                        removeSpectralBlades(entry.getKey());
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            // Retirer le glowing violet et l'invisibilité
                            player.setGlowing(false);
                            if (deathMarkTeam != null) {
                                deathMarkTeam.removeEntity(player);
                            }
                            player.removePotionEffect(PotionEffectType.INVISIBILITY);

                            player.sendMessage("§8§l[OMBRE] §7Avatar des Ombres §cterminé§7.");
                            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f);
                        }
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 5L, 5L);

        // Tâche d'enregistrement ActionBar (toutes les 20 ticks = 1s)
        // Enregistre les providers pour les nouveaux joueurs Ombre
        new BukkitRunnable() {
            @Override
            public void run() {
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Tâche Lames Spectrales (toutes les 2 ticks = 100ms pour rotation fluide)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSpectralBlades();
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    // ==================== POINTS D'OMBRE ====================

    /**
     * Récupère les Points d'Ombre d'un joueur
     */
    public int getShadowPoints(UUID playerUuid) {
        return shadowPoints.getOrDefault(playerUuid, 0);
    }

    /**
     * Ajoute des Points d'Ombre (max 5)
     */
    public void addShadowPoints(UUID playerUuid, int amount) {
        int current = shadowPoints.getOrDefault(playerUuid, 0);
        int newValue = Math.min(current + amount, MAX_SHADOW_POINTS);

        if (newValue != current) {
            shadowPoints.put(playerUuid, newValue);

            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                // Son subtil quand on gagne des points
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f + (newValue * 0.1f));

                // Particules d'ombre autour du joueur
                player.getWorld().spawnParticle(Particle.SMOKE,
                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.01);

                // Message à 5 points
                if (newValue == MAX_SHADOW_POINTS && current < MAX_SHADOW_POINTS) {
                    player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
                }
            }
        }
    }

    /**
     * Consomme tous les Points d'Ombre
     * @return Le nombre de points consommés
     */
    public int consumeAllShadowPoints(UUID playerUuid) {
        int points = shadowPoints.getOrDefault(playerUuid, 0);
        shadowPoints.put(playerUuid, 0);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && points > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.2f);
        }

        return points;
    }

    /**
     * Vérifie si le joueur a assez de points
     */
    public boolean hasEnoughPoints(UUID playerUuid, int required) {
        return getShadowPoints(playerUuid) >= required;
    }

    // ==================== ActionBar ====================

    /**
     * Enregistre les providers d'ActionBar pour les joueurs Ombre auprès du ActionBarManager
     */
    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isShadowPlayer(player)) {
                // Enregistrer le provider si pas déjà fait
                if (!activeShadowPlayers.contains(uuid)) {
                    activeShadowPlayers.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                // Retirer le provider si le joueur n'est plus Ombre
                if (activeShadowPlayers.contains(uuid)) {
                    activeShadowPlayers.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    /**
     * Vérifie si un joueur est un Chasseur branche Ombre
     */
    public boolean isShadowPlayer(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return false;

        String branchId = data.getSelectedBranchId();
        return branchId != null && branchId.contains("ombre");
    }

    /**
     * Construit le contenu de l'ActionBar pour un joueur Ombre
     * Appelé par ActionBarManager quand le joueur est en combat
     */
    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        int points = getShadowPoints(uuid);

        StringBuilder bar = new StringBuilder();

        // Points d'Ombre
        bar.append("§8§l[§r ");
        for (int i = 0; i < MAX_SHADOW_POINTS; i++) {
            if (i < points) {
                bar.append("§5◆ "); // Point plein
            } else {
                bar.append("§8◇ "); // Point vide
            }
        }
        bar.append("§8§l]§r");

        // Cooldown Pas de l'Ombre
        long stepCd = getRemainingCooldown(shadowStepCooldown, uuid);
        if (stepCd > 0) {
            bar.append("  §7Pas: §c").append(String.format("%.1f", stepCd / 1000.0)).append("s");
        } else if (hasTalent(player, Talent.TalentEffectType.SHADOW_STEP)) {
            bar.append("  §7Pas: §aPRÊT");
        }

        // Avatar actif
        if (avatarActive.containsKey(uuid)) {
            long remaining = avatarActive.get(uuid) - System.currentTimeMillis();
            if (remaining > 0) {
                bar.append("  §5§lAVATAR §d").append(String.format("%.0f", remaining / 1000.0)).append("s");
            }
        }

        // Danse Macabre active (frénésie)
        if (isDanseMacabreActive(uuid)) {
            long remaining = danceMacabreEnd.get(uuid) - System.currentTimeMillis();
            bar.append("  §5§lDANSE §d").append(String.format("%.1f", remaining / 1000.0)).append("s");
        }

        // Exécution Préparée (coût réduit)
        int prepCost = getPreparedExecutionCost(uuid);
        if (prepCost > 0) {
            long prepRemaining = preparedExecutionEnd.get(uuid) - System.currentTimeMillis();
            bar.append("  §e§lEXÉC ").append(prepCost).append("pts §7(").append(String.format("%.0f", prepRemaining / 1000.0)).append("s)");
        }

        return bar.toString();
    }

    // ==================== PAS DE L'OMBRE ====================

    /**
     * Exécute le Pas de l'Ombre (téléportation derrière la cible)
     * Avec améliorations: portée 20 blocs, +50% vitesse pendant 3s, 125% dégâts
     *
     * @param player Le joueur
     * @param target La cible
     * @param talent Le talent (pour récupérer les valeurs)
     * @return Les dégâts infligés (0 si échec)
     */
    public double executeShadowStep(Player player, LivingEntity target, Talent talent) {
        UUID uuid = player.getUniqueId();

        // Récupérer les valeurs du talent
        double[] values = talent != null ? talent.getValues() : new double[]{5000, 2, 20, 60, 1.25};
        long cooldownMs = (long) values[0];
        int pointsGained = values.length > 1 ? (int) values[1] : 2;
        double range = values.length > 2 ? values[2] : 20.0;
        int speedBuffTicks = values.length > 3 ? (int) values[3] : 60;
        double damageMult = values.length > 4 ? values[4] : 1.25;

        // Vérifier cooldown
        if (isOnCooldown(shadowStepCooldown, uuid)) {
            return 0;
        }

        // Vérifier la portée (20 blocs max)
        double distance = player.getLocation().distance(target.getLocation());
        if (distance > range) {
            player.sendMessage("§c§l[OMBRE] §7Cible trop éloignée! (max §e" + (int) range + "§7 blocs)");
            return 0;
        }

        // Mettre en cooldown IMMÉDIATEMENT pour éviter les appels récursifs
        // (target.damage() déclenche un nouvel événement EntityDamageByEntityEvent)
        shadowStepCooldown.put(uuid, System.currentTimeMillis() + cooldownMs);

        // Calculer position derrière la cible
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().normalize().multiply(-1.5); // Derrière
        Location destination = targetLoc.clone().add(direction);
        destination.setY(targetLoc.getY());

        // Calculer le yaw pour faire face VERS la cible depuis la destination
        Vector toTarget = targetLoc.toVector().subtract(destination.toVector());
        destination.setDirection(toTarget);
        destination.setPitch(0); // Garder le regard horizontal

        // Vérifier que la destination est safe (pieds + tête passables, sol solide)
        destination = findSafeDestination(destination, targetLoc, target);

        // Effets visuels au départ (trainée d'ombre)
        Location start = player.getLocation().clone();
        Location startParticle = start.clone().add(0, 1, 0);
        start.getWorld().spawnParticle(Particle.LARGE_SMOKE, startParticle, 20, 0.3, 0.5, 0.3, 0.05);
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

        // Trainée de particules entre départ et arrivée
        createShadowTrail(start.clone().add(0, 1, 0), destination.clone().add(0, 1, 0));

        // Téléportation
        player.teleport(destination);

        // Effets visuels à l'arrivée
        destination.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination.clone().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.05);
        destination.getWorld().spawnParticle(Particle.WITCH, destination, 15, 0.3, 0.3, 0.3, 0);
        destination.getWorld().playSound(destination, Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.2f);

        // Appliquer le buff de vitesse (+50% pendant 3s)
        // Speed amplifier 1 = +40%, amplifier 2 = +60%, on prend amplifier 1 (proche de 50%)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedBuffTicks, 1, false, true));

        // Calculer et infliger les dégâts (125%)
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double finalDamage = baseDamage * damageMult;
        target.damage(finalDamage, player);

        // Effets de frappe
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
            target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
        target.getWorld().spawnParticle(Particle.CRIT,
            target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.3f);

        // Ajouter Points d'Ombre
        addShadowPoints(uuid, pointsGained);

        return finalDamage;
    }

    /**
     * Crée une trainée d'ombre entre deux points
     */
    private void createShadowTrail(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();

        for (double i = 0; i < length; i += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(Color.fromRGB(40, 0, 60), 1.0f));
        }
    }

    /**
     * Trouve une destination safe pour la téléportation Shadow Step.
     * Vérifie: pieds passables, tête passable, sol solide sous les pieds.
     * Teste plusieurs positions alternatives si la première n'est pas valide.
     */
    private Location findSafeDestination(Location preferred, Location targetLoc, LivingEntity target) {
        // Tester la destination préférée (derrière la cible)
        if (isLocationSafe(preferred)) {
            return preferred;
        }

        // Tester les côtés de la cible (gauche, droite, devant)
        Vector targetDirection = targetLoc.getDirection().normalize();
        Vector[] offsets = {
            rotateVectorY(targetDirection, 90).multiply(1.5),   // Côté gauche
            rotateVectorY(targetDirection, -90).multiply(1.5),  // Côté droit
            targetDirection.clone().multiply(1.5)               // Devant
        };

        for (Vector offset : offsets) {
            Location alt = targetLoc.clone().add(offset);
            alt.setY(targetLoc.getY());

            // Calculer le yaw vers la cible
            Vector toTarget = targetLoc.toVector().subtract(alt.toVector());
            alt.setDirection(toTarget);
            alt.setPitch(0);

            if (isLocationSafe(alt)) {
                return alt;
            }
        }

        // Dernier recours: à côté de la cible avec décalage Y
        Location fallback = targetLoc.clone().add(
            (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2
        );
        // S'assurer qu'on est au sol
        while (!fallback.getBlock().getType().isSolid() && fallback.getY() > targetLoc.getY() - 5) {
            fallback.subtract(0, 1, 0);
        }
        fallback.add(0, 1, 0); // Remonter d'un bloc pour être sur le sol

        Vector toTarget = targetLoc.toVector().subtract(fallback.toVector());
        fallback.setDirection(toTarget);
        fallback.setPitch(0);

        return fallback;
    }

    /**
     * Vérifie si une location est safe pour téléporter un joueur.
     */
    private boolean isLocationSafe(Location loc) {
        // Bloc des pieds doit être passable
        if (!loc.getBlock().isPassable()) {
            return false;
        }
        // Bloc de la tête doit être passable
        if (!loc.clone().add(0, 1, 0).getBlock().isPassable()) {
            return false;
        }
        // Bloc sous les pieds doit être solide (ou au moins pas de l'air/vide)
        Location below = loc.clone().subtract(0, 1, 0);
        return below.getBlock().getType().isSolid() || !below.getBlock().isPassable();
    }

    /**
     * Fait pivoter un vecteur autour de l'axe Y.
     */
    private Vector rotateVectorY(Vector v, double angleDegrees) {
        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    /**
     * Reset le cooldown du Pas de l'Ombre (appelé par Danse Macabre)
     */
    public void resetShadowStepCooldown(UUID playerUuid) {
        shadowStepCooldown.remove(playerUuid);
    }

    // ==================== MARQUE MORTELLE ====================

    /**
     * Marque une cible avec effet de glowing violet
     */
    public void applyDeathMark(Player owner, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = owner.getUniqueId();

        // Appliquer la marque
        deathMarks.put(targetUuid, System.currentTimeMillis() + MARK_DURATION);
        markOwners.put(targetUuid, ownerUuid);

        // Ajouter à la team violet pour le glowing coloré
        if (deathMarkTeam != null) {
            deathMarkTeam.addEntity(target);
        }

        // Effet Glowing (sera violet grâce à la team)
        target.setGlowing(true);

        // Effets visuels
        target.getWorld().spawnParticle(Particle.WITCH,
            target.getLocation().add(0, target.getHeight() + 0.5, 0), 20, 0.3, 0.3, 0.3, 0);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.5f);

        // Particule au-dessus de la tête
        spawnMarkIndicator(target);
    }

    /**
     * Affiche un indicateur visuel de marque
     */
    private void spawnMarkIndicator(LivingEntity target) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 160 || target.isDead() || !deathMarks.containsKey(target.getUniqueId())) {
                    cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    target.getWorld().spawnParticle(Particle.DUST,
                        target.getLocation().add(0, target.getHeight() + 0.5, 0),
                        3, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.0f));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Vérifie si une cible est marquée
     */
    public boolean isMarked(UUID targetUuid) {
        Long expiry = deathMarks.get(targetUuid);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /**
     * Vérifie si le joueur a marqué cette cible
     */
    public boolean isMarkedBy(UUID targetUuid, UUID ownerUuid) {
        return isMarked(targetUuid) && ownerUuid.equals(markOwners.get(targetUuid));
    }

    /**
     * Retire la marque d'une cible
     */
    public void removeMark(UUID targetUuid) {
        deathMarks.remove(targetUuid);
        markOwners.remove(targetUuid);

        Entity entity = Bukkit.getEntity(targetUuid);
        if (entity instanceof LivingEntity living) {
            living.setGlowing(false);
            // Retirer de la team violet
            if (deathMarkTeam != null) {
                deathMarkTeam.removeEntity(living);
            }
        }
    }

    // ==================== EXÉCUTION ====================

    /**
     * Exécute une Exécution (5 Points d'Ombre)
     * 250% dégâts de base, 400% si la cible est marquée
     *
     * @param player Le joueur
     * @param target La cible
     * @param isMarked Si la cible est marquée par le joueur
     * @return Les dégâts infligés (0 si échec)
     */
    public double executeExecution(Player player, LivingEntity target, boolean isMarked) {
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // Vérifier les points
        if (!hasEnoughPoints(uuid, MAX_SHADOW_POINTS)) {
            player.sendMessage("§c§l[OMBRE] §7Pas assez de Points d'Ombre! (§c" +
                getShadowPoints(uuid) + "§7/§55§7)");
            return 0;
        }

        // Consommer les points
        consumeAllShadowPoints(uuid);

        // Calculer les dégâts selon le statut marqué
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double multiplier = isMarked ? 4.0 : 2.5; // 400% si marqué, 250% sinon

        double finalDamage = baseDamage * multiplier;

        // Effets visuels (plus intenses si marqué)
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.5, 0.5, 0.5, 0);
        target.getWorld().spawnParticle(Particle.CRIT, loc, isMarked ? 50 : 30, 0.5, 0.5, 0.5, 0.3);
        target.getWorld().spawnParticle(Particle.WITCH, loc, isMarked ? 35 : 20, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.8f);
        target.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, isMarked ? 0.8f : 0.5f, 1.5f);

        // Appliquer les dégâts
        target.damage(finalDamage, player);

        // Retirer la marque si présente
        if (isMarked) {
            removeMark(targetUuid);
        }

        // Message avec info sur le bonus
        String bonusInfo = isMarked ? "§d§l[MARQUÉ] " : "";
        player.sendMessage("§5§l[EXÉCUTION] " + bonusInfo + "§7Dégâts: §c" + String.format("%.0f", finalDamage) +
            " §7(" + (int)(multiplier * 100) + "%)");

        return finalDamage;
    }

    /**
     * Exécute une Exécution avec un coût de points personnalisé
     * Utilisé par Exécution Préparée (Danse Macabre) pour réduire le coût à 3
     *
     * @param player Le joueur
     * @param target La cible
     * @param isMarked Si la cible est marquée par le joueur
     * @param pointsCost Le coût en points (3 pour préparé, 5 normal)
     * @return Les dégâts infligés (0 si échec)
     */
    public double executeExecutionWithCost(Player player, LivingEntity target, boolean isMarked, int pointsCost) {
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // Vérifier les points
        if (!hasEnoughPoints(uuid, pointsCost)) {
            player.sendMessage("§c§l[OMBRE] §7Pas assez de Points d'Ombre! (§c" +
                getShadowPoints(uuid) + "§7/§5" + pointsCost + "§7)");
            return 0;
        }

        // Consommer le nombre de points requis
        int currentPoints = getShadowPoints(uuid);
        shadowPoints.put(uuid, Math.max(0, currentPoints - pointsCost));

        Player playerEntity = Bukkit.getPlayer(uuid);
        if (playerEntity != null && pointsCost > 0) {
            playerEntity.playSound(playerEntity.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.2f);
        }

        // Calculer les dégâts avec les stats ZombieZ complètes
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double multiplier = isMarked ? 4.0 : 2.5; // 400% si marqué, 250% sinon

        // Récupérer les stats ZombieZ du joueur
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        var skillManager = plugin.getSkillTreeManager();

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        baseDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        baseDamage *= (1 + (damagePercent + skillDamageBonus) / 100.0);

        double finalDamage = baseDamage * multiplier;

        // Effets visuels (plus intenses si marqué)
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.5, 0.5, 0.5, 0);
        target.getWorld().spawnParticle(Particle.CRIT, loc, isMarked ? 50 : 30, 0.5, 0.5, 0.5, 0.3);
        target.getWorld().spawnParticle(Particle.WITCH, loc, isMarked ? 35 : 20, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.8f);
        target.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, isMarked ? 0.8f : 0.5f, 1.5f);

        // Appliquer les dégâts
        target.damage(finalDamage, player);

        // Retirer la marque si présente
        if (isMarked) {
            removeMark(targetUuid);
        }

        // Message avec info sur le bonus
        String bonusInfo = isMarked ? "§d§l[MARQUÉ] " : "";
        player.sendMessage("§5§l[EXÉCUTION] " + bonusInfo + "§7Dégâts: §c" + String.format("%.0f", finalDamage) +
            " §7(" + (int)(multiplier * 100) + "%)");

        return finalDamage;
    }

    // ==================== POISON INSIDIEUX ====================

    /**
     * Applique un stack de poison (max 5)
     */
    public void applyPoisonStack(Player owner, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();
        int stacks = poisonStacks.getOrDefault(targetUuid, 0);

        if (stacks < 5) {
            stacks++;
            poisonStacks.put(targetUuid, stacks);
            poisonExpiry.put(targetUuid, System.currentTimeMillis() + 3000); // 3s

            // Appliquer l'effet poison Minecraft
            int poisonLevel = Math.min(stacks - 1, 2); // Max Poison III
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, poisonLevel, false, true));

            // Particules vertes
            target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                target.getLocation().add(0, 1, 0), 5 + stacks * 2, 0.3, 0.3, 0.3, 0);
        }
    }

    public int getPoisonStacks(UUID targetUuid) {
        return poisonStacks.getOrDefault(targetUuid, 0);
    }

    // ==================== DANSE MACABRE ====================

    /**
     * Active la Danse Macabre (REFONTE)
     * - Cascade de Mort: marque tous les ennemis proches
     * - Frénésie d'Ombre: +80% vitesse, +30% attack speed
     * - Exécution Préparée: prochaine exécution coûte 3 points
     * - Reset Pas de l'Ombre
     * - +2 Points d'Ombre
     *
     * @param player Le joueur
     * @param talent Le talent pour récupérer les valeurs
     * @param killLocation L'emplacement du kill (centre de la cascade)
     */
    public void activateDanseMacabre(Player player, Talent talent, Location killLocation) {
        UUID uuid = player.getUniqueId();

        // Récupérer les valeurs du talent
        double[] values = talent != null ? talent.getValues() : new double[]{8.0, 5000, 5000, 0.80, 0.30, 6000, 3, 2};
        double cascadeRadius = values[0];           // 8 blocs
        long markDurationMs = (long) values[1];     // 5000ms
        long frenzyDurationMs = (long) values[2];   // 5000ms
        double speedBonus = values[3];              // 0.80 (+80%)
        double attackSpeedBonus = values[4];        // 0.30 (+30%)
        long preparedExecDurationMs = (long) values[5]; // 6000ms
        int preparedExecCost = (int) values[6];     // 3 points
        int pointsGained = (int) values[7];         // 2 points

        long now = System.currentTimeMillis();
        int enemiesMarked = 0;

        // === CASCADE DE MORT - Marquer tous les ennemis proches ===
        for (Entity entity : killLocation.getWorld().getNearbyEntities(killLocation, cascadeRadius, cascadeRadius, cascadeRadius)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                // Ne pas remarquer si déjà marqué
                if (!isMarked(monster.getUniqueId())) {
                    // Marque avec durée réduite (5s au lieu de 8s)
                    applyDeathMarkWithDuration(player, monster, markDurationMs);
                    enemiesMarked++;

                    // Effet visuel de propagation de marque
                    monster.getWorld().spawnParticle(Particle.WITCH,
                        monster.getLocation().add(0, monster.getHeight() / 2, 0), 10, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        // === FRÉNÉSIE D'OMBRE - Buffs de vitesse ===
        int frenzyTicks = (int) (frenzyDurationMs / 50);

        // Speed III (+60% vitesse de déplacement)
        // Speed I = +20%, II = +40%, III = +60%
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, frenzyTicks, 2, false, true, true));

        // +30% attack speed via AttributeModifier
        applyDanseAttackSpeedBonus(player, attackSpeedBonus);

        // Tracking de la frénésie
        danceMacabreEnd.put(uuid, now + frenzyDurationMs);

        // === EXÉCUTION PRÉPARÉE ===
        preparedExecutionEnd.put(uuid, now + preparedExecDurationMs);
        preparedExecutionCost.put(uuid, preparedExecCost);

        // === RESET PAS DE L'OMBRE ===
        resetShadowStepCooldown(uuid);

        // === POINTS D'OMBRE ===
        addShadowPoints(uuid, pointsGained);

        // === EFFETS VISUELS ÉPIQUES ===
        Location loc = player.getLocation();

        // Explosion de particules d'ombre au centre
        killLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, killLocation.add(0, 1, 0), 60, 1.5, 0.5, 1.5, 0.1);
        killLocation.getWorld().spawnParticle(Particle.WITCH, killLocation, 40, cascadeRadius / 2, 0.5, cascadeRadius / 2, 0);

        // Cercle de particules montrant la zone de cascade
        for (double angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * cascadeRadius;
            double z = Math.sin(rad) * cascadeRadius;
            killLocation.getWorld().spawnParticle(Particle.DUST, killLocation.clone().add(x, 0.5, z), 2, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(128, 0, 180), 1.2f));
        }

        // Sons
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.8f);

        // === MESSAGE FEEDBACK ===
        StringBuilder msg = new StringBuilder("§5§l[DANSE MACABRE] ");
        if (enemiesMarked > 0) {
            msg.append("§d").append(enemiesMarked).append(" ennemis marqués§7! ");
        }
        msg.append("§a+Vitesse §7+ §c+AS §7+ §eExéc. 3pts§7!");
        player.sendMessage(msg.toString());

        // Aura visuelle pendant la frénésie
        startDanseMacabreAura(uuid, frenzyTicks);
    }

    /**
     * Applique une marque avec durée personnalisée (pour Danse Macabre)
     */
    private void applyDeathMarkWithDuration(Player owner, LivingEntity target, long durationMs) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = owner.getUniqueId();

        deathMarks.put(targetUuid, System.currentTimeMillis() + durationMs);
        markOwners.put(targetUuid, ownerUuid);

        if (deathMarkTeam != null) {
            deathMarkTeam.addEntity(target);
        }
        target.setGlowing(true);
    }

    /**
     * Aura visuelle pendant Danse Macabre
     */
    private void startDanseMacabreAura(UUID playerUuid, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !danceMacabreEnd.containsKey(playerUuid)) {
                    cancel();
                    return;
                }

                Player player = Bukkit.getPlayer(playerUuid);
                if (player == null) {
                    cancel();
                    return;
                }

                // Particules autour du joueur (spirale ascendante)
                if (ticks % 4 == 0) {
                    double angle = (ticks * 20) % 360;
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * 0.8;
                    double z = Math.sin(rad) * 0.8;
                    double y = (ticks % 40) / 40.0 * 2;

                    Location loc = player.getLocation().add(x, y, z);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 0, 200), 0.8f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Applique le bonus de vitesse d'attaque de Danse Macabre
     */
    private void applyDanseAttackSpeedBonus(Player player, double bonus) {
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        // Retirer l'ancien modifier s'il existe
        removeDanseAttackSpeedBonus(player);

        // Ajouter le nouveau modifier
        AttributeModifier modifier = new AttributeModifier(
            DANSE_ATTACK_SPEED_KEY,
            bonus,
            AttributeModifier.Operation.ADD_SCALAR
        );
        attackSpeed.addModifier(modifier);
    }

    /**
     * Retire le bonus de vitesse d'attaque de Danse Macabre
     */
    private void removeDanseAttackSpeedBonus(Player player) {
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        for (var modifier : attackSpeed.getModifiers()) {
            if (modifier.getKey().equals(DANSE_ATTACK_SPEED_KEY)) {
                attackSpeed.removeModifier(modifier);
                break;
            }
        }
    }

    /**
     * Vérifie si le joueur a une Exécution Préparée active
     * @return Le coût réduit (3) ou -1 si pas actif
     */
    public int getPreparedExecutionCost(UUID playerUuid) {
        Long endTime = preparedExecutionEnd.get(playerUuid);
        if (endTime != null && System.currentTimeMillis() < endTime) {
            return preparedExecutionCost.getOrDefault(playerUuid, -1);
        }
        return -1;
    }

    /**
     * Consomme le buff d'Exécution Préparée
     */
    public void consumePreparedExecution(UUID playerUuid) {
        preparedExecutionEnd.remove(playerUuid);
        preparedExecutionCost.remove(playerUuid);
    }

    /**
     * Vérifie si Danse Macabre est active (pour l'ActionBar)
     */
    public boolean isDanseMacabreActive(UUID playerUuid) {
        Long endTime = danceMacabreEnd.get(playerUuid);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    // ==================== LAMES SPECTRALES (Style Vampire Survivors) ====================

    /**
     * Invoque les Lames Spectrales autour du joueur
     * Style Vampire Survivors: lames orbitales qui frappent automatiquement
     *
     * @param owner Le joueur
     * @param talent Le talent pour récupérer les valeurs
     */
    public void summonSpectralBlades(Player owner, Talent talent) {
        UUID ownerUuid = owner.getUniqueId();

        // Récupérer les valeurs du talent
        double[] values = talent != null ? talent.getValues() : new double[]{5, 8000, 5, 3.0, 0.35, 2000};
        long duration = (long) values[1];           // 8000ms
        int bladeCount = (int) values[2];           // 5 lames
        double orbitRadius = values[3];             // 3.0 blocs
        double damagePercent = values[4];           // 0.35 (35%)
        long rotationPeriod = (long) values[5];     // 2000ms pour un tour complet

        // Si déjà des lames actives, les refresh
        if (activeBlades.containsKey(ownerUuid)) {
            removeSpectralBlades(ownerUuid);
        }

        // Créer les nouvelles lames
        SpectralBladesData bladesData = new SpectralBladesData(
            ownerUuid, duration, bladeCount, orbitRadius, damagePercent, rotationPeriod
        );
        activeBlades.put(ownerUuid, bladesData);

        // Initialiser les cooldowns de frappe
        bladeHitCooldowns.put(ownerUuid, new ConcurrentHashMap<>());

        // Effets d'invocation
        Location loc = owner.getLocation();
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 1, 0), 50, 1.5, 0.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 30, 1.0, 0.5, 1.0, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

        // Message
        owner.sendMessage("§5§l[OMBRE] §dLames Spectrales §7invoquées! (§e" + (duration / 1000) + "s§7)");
    }

    /**
     * Version simplifiée sans talent (utilise valeurs par défaut)
     */
    public void summonSpectralBlades(Player owner) {
        summonSpectralBlades(owner, null);
    }

    /**
     * Met à jour toutes les Lames Spectrales actives
     * - Fait tourner les lames autour des joueurs
     * - Vérifie les collisions avec les ennemis
     * - Affiche les particules
     */
    private void updateSpectralBlades() {
        long now = System.currentTimeMillis();

        // Nettoyer les lames expirées
        activeBlades.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                removeSpectralBlades(entry.getKey());
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage("§5§l[OMBRE] §7Lames Spectrales §cdissipées§7.");
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.2f);
                }
                return true;
            }
            return false;
        });

        // Mettre à jour chaque joueur avec des lames actives
        for (Map.Entry<UUID, SpectralBladesData> entry : activeBlades.entrySet()) {
            UUID ownerUuid = entry.getKey();
            SpectralBladesData data = entry.getValue();
            Player owner = Bukkit.getPlayer(ownerUuid);

            if (owner == null || !owner.isOnline()) {
                continue;
            }

            Location ownerLoc = owner.getLocation().add(0, 1.0, 0); // Hauteur des lames

            // Calculer l'angle de rotation (360° en rotationPeriod ms)
            double rotationSpeed = 360.0 / data.rotationPeriod * 100; // degrés par 100ms (tick rate)
            data.currentAngle = (data.currentAngle + rotationSpeed) % 360;

            // Pour chaque lame
            double angleStep = 360.0 / data.bladeCount;
            for (int i = 0; i < data.bladeCount; i++) {
                double bladeAngle = data.currentAngle + (i * angleStep);
                double rad = Math.toRadians(bladeAngle);

                // Position de la lame
                double x = Math.cos(rad) * data.orbitRadius;
                double z = Math.sin(rad) * data.orbitRadius;
                Location bladeLoc = ownerLoc.clone().add(x, 0, z);

                // Particules de la lame (forme allongée comme une épée)
                spawnBladeParticles(bladeLoc, bladeAngle, owner.getWorld());

                // Vérifier les collisions avec les ennemis
                checkBladeCollisions(owner, bladeLoc, data);
            }
        }
    }

    /**
     * Affiche les particules d'une lame spectrale
     */
    private void spawnBladeParticles(Location center, double angle, org.bukkit.World world) {
        // Direction de la lame (perpendiculaire au mouvement)
        double rad = Math.toRadians(angle + 90); // Perpendiculaire
        Vector bladeDir = new Vector(Math.cos(rad), 0, Math.sin(rad)).normalize();

        // Dessiner la lame (3 points formant une ligne)
        for (double offset = -0.5; offset <= 0.5; offset += 0.25) {
            Location point = center.clone().add(bladeDir.clone().multiply(offset));

            // Particule principale (violet brillant)
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(148, 0, 211), 1.2f));

            // Trainée de la lame
            world.spawnParticle(Particle.DUST, point.clone().add(0, -0.1, 0), 1, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(Color.fromRGB(75, 0, 130), 0.8f));
        }

        // Pointe de la lame (plus brillante)
        Location tip = center.clone().add(bladeDir.clone().multiply(0.6));
        world.spawnParticle(Particle.END_ROD, tip, 1, 0, 0, 0, 0);
    }

    /**
     * Vérifie les collisions de la lame avec les ennemis
     */
    private void checkBladeCollisions(Player owner, Location bladeLoc, SpectralBladesData data) {
        UUID ownerUuid = owner.getUniqueId();
        Map<UUID, Long> hitCooldowns = bladeHitCooldowns.get(ownerUuid);
        if (hitCooldowns == null) return;

        long now = System.currentTimeMillis();

        // Chercher les ennemis proches de la lame
        for (Entity entity : bladeLoc.getWorld().getNearbyEntities(bladeLoc, BLADE_HIT_RADIUS, BLADE_HIT_RADIUS, BLADE_HIT_RADIUS)) {
            if (!(entity instanceof Monster monster)) continue;
            if (!monster.isValid() || monster.isDead()) continue;

            UUID targetUuid = monster.getUniqueId();

            // Vérifier le cooldown de frappe sur cette cible
            Long lastHit = hitCooldowns.get(targetUuid);
            if (lastHit != null && (now - lastHit) < BLADE_HIT_COOLDOWN_MS) {
                continue; // En cooldown
            }

            // Marquer la frappe
            hitCooldowns.put(targetUuid, now);

            // Infliger les dégâts
            applyBladeDamage(owner, monster, data.damagePercent);
        }
    }

    /**
     * Applique les dégâts d'une lame spectrale
     */
    private void applyBladeDamage(Player owner, LivingEntity target, double damagePercent) {
        if (target.isDead()) return;

        UUID ownerUuid = owner.getUniqueId();

        // ============ CALCUL DES DÉGÂTS ============

        // Dégâts de base du joueur
        double baseDamage = owner.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        baseDamage *= damagePercent; // 35% par défaut
        double finalDamage = baseDamage;
        boolean isCritical = false;

        // Stats d'équipement ZombieZ
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(owner);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus * damagePercent;

        // Bonus de dégâts en pourcentage
        double damagePct = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePct / 100.0);

        // Skill Tree bonuses
        var skillManager = plugin.getSkillTreeManager();
        double skillDamageBonus = skillManager.getSkillBonus(owner, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // Système de critique
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(owner, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (Math.random() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0;
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(owner, SkillBonus.CRIT_DAMAGE);
            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // Bonus si cible marquée (+50% pour les lames)
        boolean isMarkedTarget = isMarked(target.getUniqueId());
        if (isMarkedTarget) {
            finalDamage *= 1.50;
        }

        // Momentum System
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(owner);
        finalDamage *= momentumMultiplier;

        // ============ APPLICATION DES DÉGÂTS ============

        // Metadata pour l'indicateur de dégâts
        target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
        target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
        target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, ownerUuid.toString()));

        // Attribution du loot
        if (plugin.getZombieManager().isZombieZMob(target)) {
            target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, ownerUuid.toString()));
        }

        // Appliquer les dégâts
        final double damage = Math.max(1.0, finalDamage);
        target.damage(damage, owner);

        // Mettre à jour l'affichage de vie
        if (plugin.getZombieManager().isZombieZMob(target)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(target);
                }
            });
        }

        // ============ EFFETS VISUELS ============

        Location loc = target.getLocation().add(0, 1, 0);

        // Son de frappe
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);

        // Particules de frappe
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
        target.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(Color.fromRGB(148, 0, 211), 1.0f));

        // Effet critique
        if (isCritical) {
            target.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.3f);
        }

        // Effet marqué
        if (isMarkedTarget) {
            target.getWorld().spawnParticle(Particle.WITCH, loc, 5, 0.2, 0.2, 0.2, 0);
        }

        // 15% de chance de marquer la cible
        if (!target.isDead() && !isMarked(target.getUniqueId()) && Math.random() < 0.15) {
            applyDeathMark(owner, target);
        }

        // Synergie Avatar: kill peut déclencher Shadow Storm
        if (target.isDead() && hasTalent(owner, Talent.TalentEffectType.SHADOW_STORM)) {
            triggerShadowStorm(owner, target.getLocation(), damage);
            owner.sendMessage("§5§l[LAMES] §7Kill déclenche §d§lTempête d'Ombre§7!");
        }
    }

    /**
     * Supprime les lames spectrales d'un joueur
     */
    private void removeSpectralBlades(UUID ownerUuid) {
        activeBlades.remove(ownerUuid);
        bladeHitCooldowns.remove(ownerUuid);
    }

    /**
     * Vérifie si le joueur a des lames spectrales actives
     */
    public boolean hasActiveBlades(UUID ownerUuid) {
        SpectralBladesData data = activeBlades.get(ownerUuid);
        return data != null && !data.isExpired();
    }

    /**
     * Récupère le temps restant des lames (pour ActionBar)
     */
    public long getBladesRemainingTime(UUID ownerUuid) {
        SpectralBladesData data = activeBlades.get(ownerUuid);
        return data != null ? data.getRemainingTime() : 0;
    }

    // ==================== TEMPÊTE D'OMBRES ====================

    /**
     * Déclenche l'explosion AoE de Tempête d'Ombres
     * @param owner Le joueur propriétaire
     * @param center Le centre de l'explosion
     * @param baseDamage Les dégâts de base (de l'Exécution)
     * @param radius Le rayon de l'explosion (valeur du talent)
     * @param damageMult Le multiplicateur de dégâts (valeur du talent, ex: 1.50 = 150%)
     */
    public void triggerShadowStorm(Player owner, Location center, double baseDamage, double radius, double damageMult) {
        // Explosion visuelle (proportionnelle au rayon)
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, (int)(50 * radius / 5), radius, 1, radius, 0.1);
        center.getWorld().spawnParticle(Particle.WITCH, center, (int)(40 * radius / 5), radius, 1, radius, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 1.0f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Cercle de particules montrant la zone
        for (double angle = 0; angle < 360; angle += 20) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.5, z), 2, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.2f));
        }

        // Dégâts AoE avec multiplicateur
        double aoeDamage = baseDamage * damageMult;
        int enemiesHit = 0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius / 2, radius)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                // Metadata pour l'indicateur de dégâts
                monster.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
                monster.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

                monster.damage(aoeDamage, owner);
                applyDeathMark(owner, monster); // Marquer tous
                enemiesHit++;

                // Knockback
                Vector knockback = monster.getLocation().toVector()
                    .subtract(center.toVector()).normalize().multiply(0.8);
                knockback.setY(0.4);
                monster.setVelocity(knockback);

                // Particule d'impact sur chaque ennemi
                monster.getWorld().spawnParticle(Particle.CRIT, monster.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Bonus Points d'Ombre (+1 par ennemi)
        addShadowPoints(owner.getUniqueId(), enemiesHit);

        if (enemiesHit > 0) {
            owner.sendMessage("§5§l[TEMPÊTE] §7" + enemiesHit + " ennemis touchés! §d+" +
                enemiesHit + " Points d'Ombre §7(§c" + String.format("%.0f", aoeDamage) + " dmg§7)");
        }
    }

    /**
     * Version simplifiée pour les appels internes (lame kills)
     */
    public void triggerShadowStorm(Player owner, Location center, double baseDamage) {
        triggerShadowStorm(owner, center, baseDamage, 6.0, 1.50);
    }

    // ==================== AVATAR DES OMBRES ====================

    /**
     * Active l'Avatar des Ombres (ultime)
     * @param player Le joueur
     * @param talent Le talent pour récupérer les valeurs (optionnel)
     */
    public boolean activateAvatar(Player player, Talent talent) {
        UUID uuid = player.getUniqueId();

        // Récupérer les valeurs du talent
        // values: duration_ms, blade_multiplier, point_interval_ms, damage_bonus, cooldown_ms
        double[] values = talent != null ? talent.getValues() : new double[]{15000, 2, 1000, 0.40, 45000};
        long durationMs = (long) values[0];     // 15000ms
        int bladeMultiplier = (int) values[1];  // 2x lames (10 lames au total)
        double damageBonus = values[3];         // 0.40 (+40%)
        long cooldownMs = values.length > 4 ? (long) values[4] : 45000L; // 45s

        // Vérifier cooldown
        if (isOnCooldown(avatarCooldown, uuid)) {
            long remaining = getRemainingCooldown(avatarCooldown, uuid);
            player.sendMessage("§c§l[OMBRE] §7Avatar en cooldown: §c" +
                String.format("%.0f", remaining / 1000.0) + "s");
            return false;
        }

        // Activer l'Avatar avec durée du talent
        avatarActive.put(uuid, System.currentTimeMillis() + durationMs);
        avatarCooldown.put(uuid, System.currentTimeMillis() + cooldownMs);

        // Invoquer les Lames Spectrales améliorées (plus de lames, plus longues)
        // Avatar double le nombre de lames et leur durée
        int baseBlades = 5;
        int avatarBlades = baseBlades * bladeMultiplier; // 10 lames avec Avatar
        activeBlades.put(uuid, new SpectralBladesData(
            uuid, durationMs, avatarBlades, 3.5, 0.45, 1500 // Plus de dégâts, rotation plus rapide
        ));
        bladeHitCooldowns.put(uuid, new ConcurrentHashMap<>());

        // Remplir les Points d'Ombre
        shadowPoints.put(uuid, MAX_SHADOW_POINTS);

        // Effet semi-transparent (invisibilité partielle)
        int durationTicks = (int) (durationMs / 50);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false, true));

        // Ajouter glowing violet pour être visible malgré l'invisibilité
        player.setGlowing(true);
        if (deathMarkTeam != null) {
            deathMarkTeam.addEntity(player);
        }

        // Effets visuels épiques
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 100, 2, 2, 2, 0.2);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 60, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);

        // Aura continue pendant l'Avatar
        startAvatarAura(uuid, durationTicks);

        player.sendMessage("§5§l[AVATAR DES OMBRES] §dActivé! §7" + (durationMs / 1000) + "s de puissance absolue! §c+" +
            (int)(damageBonus * 100) + "% dégâts");

        return true;
    }

    /**
     * Version sans talent (utilise valeurs par défaut)
     */
    public boolean activateAvatar(Player player) {
        return activateAvatar(player, null);
    }

    /**
     * Aura visuelle pendant l'Avatar
     */
    private void startAvatarAura(UUID playerUuid, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !avatarActive.containsKey(playerUuid)) {
                    // Fin de l'Avatar - retirer les effets visuels
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null) {
                        player.setGlowing(false);
                        if (deathMarkTeam != null) {
                            deathMarkTeam.removeEntity(player);
                        }
                    }
                    cancel();
                    return;
                }

                Player player = Bukkit.getPlayer(playerUuid);
                if (player == null) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                double time = System.currentTimeMillis() / 200.0;

                // Cercle de particules autour du joueur
                for (int i = 0; i < 8; i++) {
                    double angle = time + (i * 45);
                    double x = Math.cos(Math.toRadians(angle)) * 1.5;
                    double z = Math.sin(Math.toRadians(angle)) * 1.5;

                    loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.0f));
                }

                // Particules d'ombre sur le joueur
                if (ticks % 10 == 0) {
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Vérifie si l'Avatar est actif
     */
    public boolean isAvatarActive(UUID playerUuid) {
        Long endTime = avatarActive.get(playerUuid);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    // ==================== UTILITAIRES ====================

    private boolean isOnCooldown(Map<UUID, Long> cooldownMap, UUID uuid) {
        Long cd = cooldownMap.get(uuid);
        return cd != null && System.currentTimeMillis() < cd;
    }

    private long getRemainingCooldown(Map<UUID, Long> cooldownMap, UUID uuid) {
        Long cd = cooldownMap.get(uuid);
        if (cd == null) return 0;
        return Math.max(0, cd - System.currentTimeMillis());
    }

    public boolean hasTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType) != null;
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID playerUuid) {
        shadowPoints.remove(playerUuid);
        shadowStepCooldown.remove(playerUuid);
        avatarActive.remove(playerUuid);
        avatarCooldown.remove(playerUuid);
        danceMacabreEnd.remove(playerUuid);
        preparedExecutionEnd.remove(playerUuid);
        preparedExecutionCost.remove(playerUuid);
        removeSpectralBlades(playerUuid);
        activeShadowPlayers.remove(playerUuid);

        // Retirer les modifiers d'attributs et effets visuels
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            removeDanseAttackSpeedBonus(player);

            // Retirer les effets de l'Avatar
            player.setGlowing(false);
            if (deathMarkTeam != null) {
                deathMarkTeam.removeEntity(player);
            }
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        // Désenregistrer de l'ActionBarManager
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(playerUuid);
        }
    }
}
