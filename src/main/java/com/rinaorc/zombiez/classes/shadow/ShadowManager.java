package com.rinaorc.zombiez.classes.shadow;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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

    // Clones d'Ombre actifs (player UUID -> clone entity UUID)
    private final Map<UUID, List<UUID>> activeClones = new ConcurrentHashMap<>();

    // Avatar des Ombres (player UUID -> end time)
    private final Map<UUID, Long> avatarActive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> avatarCooldown = new ConcurrentHashMap<>();

    // Danse Macabre - tracking invisibilité
    private final Map<UUID, Long> danceMacabreEnd = new ConcurrentHashMap<>();

    // Cache des joueurs Ombre actifs
    private final Set<UUID> activeShadowPlayers = ConcurrentHashMap.newKeySet();

    public ShadowManager(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        startTasks();
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

                // Cleanup Danse Macabre
                danceMacabreEnd.entrySet().removeIf(entry -> now > entry.getValue());

                // Régénération Avatar (1 point/s)
                avatarActive.forEach((uuid, endTime) -> {
                    if (now < endTime) {
                        addShadowPoints(uuid, 1);
                    }
                });

                // Cleanup Avatar expiré
                avatarActive.entrySet().removeIf(entry -> {
                    if (now > entry.getValue()) {
                        // Supprimer les clones d'Avatar
                        removeAllClones(entry.getKey());
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
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

        // Tâche clones (toutes les 20 ticks = 1s)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateClones();
            }
        }.runTaskTimer(plugin, 20L, 20L);
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

        // Danse Macabre (invisibilité)
        if (danceMacabreEnd.containsKey(uuid)) {
            long remaining = danceMacabreEnd.get(uuid) - System.currentTimeMillis();
            if (remaining > 0) {
                bar.append("  §8§lFURTIF");
            }
        }

        return bar.toString();
    }

    // ==================== PAS DE L'OMBRE ====================

    /**
     * Exécute le Pas de l'Ombre (téléportation derrière la cible)
     */
    public boolean executeShadowStep(Player player, LivingEntity target) {
        UUID uuid = player.getUniqueId();

        // Vérifier cooldown
        if (isOnCooldown(shadowStepCooldown, uuid)) {
            return false;
        }

        // Calculer position derrière la cible
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().normalize().multiply(-1.5); // Derrière
        Location destination = targetLoc.add(direction);
        destination.setY(targetLoc.getY());
        destination.setYaw(targetLoc.getYaw() + 180); // Face à la cible
        destination.setPitch(0);

        // Vérifier que la destination est safe
        if (!destination.getBlock().isPassable()) {
            destination = targetLoc.clone().add(0, 0.5, 0);
        }

        // Effets visuels au départ
        Location start = player.getLocation();
        start.getWorld().spawnParticle(Particle.LARGE_SMOKE, start.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

        // Téléportation
        player.teleport(destination);

        // Effets visuels à l'arrivée
        destination.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination.add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.05);
        destination.getWorld().spawnParticle(Particle.WITCH, destination, 15, 0.3, 0.3, 0.3, 0);
        destination.getWorld().playSound(destination, Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.2f);

        // Ajouter 2 Points d'Ombre
        addShadowPoints(uuid, 2);

        // Mettre en cooldown
        shadowStepCooldown.put(uuid, System.currentTimeMillis() + SHADOW_STEP_COOLDOWN);

        return true;
    }

    /**
     * Reset le cooldown du Pas de l'Ombre (appelé par Danse Macabre)
     */
    public void resetShadowStepCooldown(UUID playerUuid) {
        shadowStepCooldown.remove(playerUuid);
    }

    // ==================== MARQUE MORTELLE ====================

    /**
     * Marque une cible
     */
    public void applyDeathMark(Player owner, LivingEntity target) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = owner.getUniqueId();

        // Appliquer la marque
        deathMarks.put(targetUuid, System.currentTimeMillis() + MARK_DURATION);
        markOwners.put(targetUuid, ownerUuid);

        // Effet Glowing
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
        }
    }

    // ==================== EXÉCUTION ====================

    /**
     * Exécute une Exécution sur une cible marquée
     * @return Les dégâts infligés (0 si échec)
     */
    public double executeExecution(Player player, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // Vérifier que la cible est marquée par ce joueur
        if (!isMarkedBy(targetUuid, uuid)) {
            player.sendMessage("§c§l[OMBRE] §7Cette cible n'est pas marquée!");
            return 0;
        }

        // Vérifier les points
        if (!hasEnoughPoints(uuid, MAX_SHADOW_POINTS)) {
            player.sendMessage("§c§l[OMBRE] §7Pas assez de Points d'Ombre! (§c" +
                getShadowPoints(uuid) + "§7/§55§7)");
            return 0;
        }

        // Consommer les points
        consumeAllShadowPoints(uuid);

        // Calculer les dégâts
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double healthPercent = target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue();

        double multiplier = 2.5; // 250% de base (équilibré)
        if (healthPercent < 0.30) {
            multiplier = 4.0; // 400% si < 30% PV (équilibré)
        }

        double finalDamage = baseDamage * multiplier;

        // Effets visuels AVANT les dégâts
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.5, 0.5, 0.5, 0);
        target.getWorld().spawnParticle(Particle.CRIT, loc, 30, 0.5, 0.5, 0.5, 0.3);
        target.getWorld().spawnParticle(Particle.WITCH, loc, 20, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.8f);
        target.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.5f, 1.5f);

        // Appliquer les dégâts
        target.damage(finalDamage, player);

        // Retirer la marque
        removeMark(targetUuid);

        // Message
        player.sendMessage("§5§l[EXÉCUTION] §7Dégâts: §c" + String.format("%.0f", finalDamage) +
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
     * Active Danse Macabre (invisibilité + reset Pas + vitesse)
     */
    public void activateDanseMacabre(Player player) {
        UUID uuid = player.getUniqueId();

        // 2s d'invisibilité
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
            (int) DANCE_MACABRE_INVIS_DURATION, 0, false, false));

        // +50% vitesse
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
            (int) DANCE_MACABRE_INVIS_DURATION, 1, false, false));

        // Reset Pas de l'Ombre
        resetShadowStepCooldown(uuid);

        // Tracking
        danceMacabreEnd.put(uuid, System.currentTimeMillis() + (DANCE_MACABRE_INVIS_DURATION * 50));

        // Effets
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

        player.sendMessage("§8§l[OMBRE] §7Danse Macabre! §aInvisible §7+ §bPas reset");
    }

    // ==================== CLONE D'OMBRE ====================

    /**
     * Invoque un Clone d'Ombre
     */
    public void summonShadowClone(Player owner) {
        UUID ownerUuid = owner.getUniqueId();

        // Limiter à 1 clone (ou 2 avec Avatar)
        int maxClones = avatarActive.containsKey(ownerUuid) ? 2 : 1;
        List<UUID> clones = activeClones.getOrDefault(ownerUuid, new ArrayList<>());

        if (clones.size() >= maxClones) {
            // Supprimer le plus vieux clone
            if (!clones.isEmpty()) {
                UUID oldClone = clones.remove(0);
                Entity entity = Bukkit.getEntity(oldClone);
                if (entity != null) entity.remove();
            }
        }

        // Créer le clone (ArmorStand invisible avec particules)
        Location spawnLoc = owner.getLocation().add(
            (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);

        ArmorStand clone = owner.getWorld().spawn(spawnLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setMetadata("shadow_clone_owner", new FixedMetadataValue(plugin, ownerUuid.toString()));
            stand.customName(Component.text("Clone de " + owner.getName(), NamedTextColor.DARK_PURPLE));
            stand.setCustomNameVisible(false);
        });

        clones.add(clone.getUniqueId());
        activeClones.put(ownerUuid, clones);

        // Effets d'apparition
        spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);

        // Particules continues pour le clone
        startCloneParticles(clone.getUniqueId(), ownerUuid);

        // Auto-destruction après CLONE_DURATION
        new BukkitRunnable() {
            @Override
            public void run() {
                if (clone.isValid() && !clone.isDead()) {
                    removeClone(ownerUuid, clone.getUniqueId());
                }
            }
        }.runTaskLater(plugin, CLONE_DURATION / 50); // Convertir ms en ticks

        owner.sendMessage("§5§l[OMBRE] §7Clone d'Ombre invoqué! (§d10s§7)");
    }

    /**
     * Particules continues pour un clone
     */
    private void startCloneParticles(UUID cloneUuid, UUID ownerUuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Entity clone = Bukkit.getEntity(cloneUuid);
                if (clone == null || !clone.isValid()) {
                    cancel();
                    return;
                }

                // Forme humanoïde en particules
                Location loc = clone.getLocation();
                // Corps
                for (double y = 0; y < 1.8; y += 0.3) {
                    loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(0, y, 0), 1, 0.15, 0.05, 0.15, 0,
                        new Particle.DustOptions(Color.fromRGB(30, 0, 50), 1.2f));
                }
                // Yeux
                loc.getWorld().spawnParticle(Particle.DUST,
                    loc.clone().add(0.1, 1.5, 0.2), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 200), 0.5f));
                loc.getWorld().spawnParticle(Particle.DUST,
                    loc.clone().add(-0.1, 1.5, 0.2), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 200), 0.5f));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Fait attaquer les clones la cible
     */
    public void clonesAttack(Player owner, LivingEntity target, double ownerDamage) {
        UUID ownerUuid = owner.getUniqueId();
        List<UUID> clones = activeClones.get(ownerUuid);
        if (clones == null || clones.isEmpty()) return;

        double cloneDamage = ownerDamage * 0.4; // 40% des dégâts (équilibré)

        for (UUID cloneUuid : clones) {
            Entity clone = Bukkit.getEntity(cloneUuid);
            if (clone == null) continue;

            // Téléporter le clone vers la cible
            Location cloneLoc = clone.getLocation();
            Location targetLoc = target.getLocation();
            Vector dir = targetLoc.toVector().subtract(cloneLoc.toVector()).normalize();

            // Animation d'attaque
            new BukkitRunnable() {
                int step = 0;
                @Override
                public void run() {
                    if (step >= 5 || target.isDead()) {
                        cancel();
                        return;
                    }

                    // Déplacer vers la cible
                    Location newLoc = clone.getLocation().add(dir.clone().multiply(0.5));
                    clone.teleport(newLoc);

                    if (step == 3) {
                        // Impact
                        target.damage(cloneDamage, owner);
                        target.getWorld().spawnParticle(Particle.CRIT,
                            target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                        target.getWorld().playSound(target.getLocation(),
                            Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);

                        // Le clone peut marquer (Tier 7)
                        if (hasTalent(owner, Talent.TalentEffectType.SHADOW_CLONE) && !isMarked(target.getUniqueId())) {
                            if (Math.random() < 0.25) { // 25% chance
                                applyDeathMark(owner, target);
                            }
                        }
                    }

                    step++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }

    /**
     * Supprime un clone
     */
    private void removeClone(UUID ownerUuid, UUID cloneUuid) {
        Entity clone = Bukkit.getEntity(cloneUuid);
        if (clone != null) {
            clone.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                clone.getLocation().add(0, 1, 0), 15, 0.2, 0.4, 0.2, 0.02);
            clone.remove();
        }

        List<UUID> clones = activeClones.get(ownerUuid);
        if (clones != null) {
            clones.remove(cloneUuid);
        }
    }

    /**
     * Supprime tous les clones d'un joueur
     */
    public void removeAllClones(UUID ownerUuid) {
        List<UUID> clones = activeClones.remove(ownerUuid);
        if (clones != null) {
            for (UUID cloneUuid : clones) {
                Entity clone = Bukkit.getEntity(cloneUuid);
                if (clone != null) clone.remove();
            }
        }
    }

    /**
     * Met à jour les clones (les fait suivre le joueur)
     */
    private void updateClones() {
        for (Map.Entry<UUID, List<UUID>> entry : activeClones.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null) continue;

            Location ownerLoc = owner.getLocation();
            int index = 0;
            for (UUID cloneUuid : entry.getValue()) {
                Entity clone = Bukkit.getEntity(cloneUuid);
                if (clone == null) continue;

                // Positionner en cercle autour du joueur
                double angle = (index * 180) + (System.currentTimeMillis() / 50.0 % 360);
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * 2;
                double z = Math.sin(rad) * 2;

                Location targetLoc = ownerLoc.clone().add(x, 0, z);
                targetLoc.setYaw(ownerLoc.getYaw());

                // Mouvement fluide
                Location cloneLoc = clone.getLocation();
                double dx = (targetLoc.getX() - cloneLoc.getX()) * 0.2;
                double dz = (targetLoc.getZ() - cloneLoc.getZ()) * 0.2;
                clone.teleport(cloneLoc.add(dx, 0, dz));

                index++;
            }
        }
    }

    // ==================== TEMPÊTE D'OMBRES ====================

    /**
     * Déclenche l'explosion AoE de Tempête d'Ombres
     */
    public void triggerShadowStorm(Player owner, Location center, double baseDamage) {
        // Explosion visuelle
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 50, 2, 1, 2, 0.1);
        center.getWorld().spawnParticle(Particle.WITCH, center, 40, 2, 1, 2, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 1.0f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Dégâts AoE
        double aoeDamage = baseDamage; // 100% des dégâts de base
        int enemiesHit = 0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                monster.damage(aoeDamage, owner);
                applyDeathMark(owner, monster); // Marquer tous
                enemiesHit++;

                // Knockback
                Vector knockback = monster.getLocation().toVector()
                    .subtract(center.toVector()).normalize().multiply(0.8);
                knockback.setY(0.4);
                monster.setVelocity(knockback);
            }
        }

        // Bonus Points d'Ombre (+1 par ennemi)
        addShadowPoints(owner.getUniqueId(), enemiesHit);

        if (enemiesHit > 0) {
            owner.sendMessage("§5§l[TEMPÊTE] §7" + enemiesHit + " ennemis touchés! §d+" +
                enemiesHit + " Points d'Ombre");
        }
    }

    // ==================== AVATAR DES OMBRES ====================

    /**
     * Active l'Avatar des Ombres (ultime)
     */
    public boolean activateAvatar(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier cooldown
        if (isOnCooldown(avatarCooldown, uuid)) {
            long remaining = getRemainingCooldown(avatarCooldown, uuid);
            player.sendMessage("§c§l[OMBRE] §7Avatar en cooldown: §c" +
                String.format("%.0f", remaining / 1000.0) + "s");
            return false;
        }

        // Activer l'Avatar
        avatarActive.put(uuid, System.currentTimeMillis() + AVATAR_DURATION);
        avatarCooldown.put(uuid, System.currentTimeMillis() + AVATAR_COOLDOWN);

        // Invoquer 2 clones
        summonShadowClone(player);
        summonShadowClone(player);

        // Remplir les Points d'Ombre
        shadowPoints.put(uuid, MAX_SHADOW_POINTS);

        // Effets visuels épiques
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 100, 2, 2, 2, 0.2);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 60, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);

        // Aura continue pendant l'Avatar
        startAvatarAura(uuid);

        player.sendMessage("§5§l[AVATAR DES OMBRES] §dActivé! §715s de puissance absolue!");

        return true;
    }

    /**
     * Aura visuelle pendant l'Avatar
     */
    private void startAvatarAura(UUID playerUuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!avatarActive.containsKey(playerUuid)) {
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
        removeAllClones(playerUuid);
        activeShadowPlayers.remove(playerUuid);

        // Désenregistrer de l'ActionBarManager
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(playerUuid);
        }
    }
}
