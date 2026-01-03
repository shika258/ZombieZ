package com.rinaorc.zombiez.ascension;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.gui.AscensionGUI;
import com.rinaorc.zombiez.items.types.StatType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Gestionnaire principal du système d'Ascension
 * Gère les mutations, les stades, et les effets
 */
public class AscensionManager {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<UUID, AscensionData> playerAscensions = new ConcurrentHashMap<>();

    // BossBars pour les timers de choix
    private final Map<UUID, BossBar> choiceBossBars = new ConcurrentHashMap<>();

    // Task de mise à jour des stacks et timers
    private BukkitTask updateTask;

    // ==================== CONSTRUCTEUR ====================

    public AscensionManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    // ==================== INITIALISATION ====================

    /**
     * Démarre la tâche de mise à jour périodique
     */
    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Map.Entry<UUID, AscensionData> entry : playerAscensions.entrySet()) {
                UUID playerId = entry.getKey();
                AscensionData data = entry.getValue();
                Player player = Bukkit.getPlayer(playerId);

                if (player == null || !player.isOnline()) continue;

                // Mettre à jour les stacks temporaires
                data.updateStacks();

                // Vérifier le timeout de choix
                if (data.isChoicePending()) {
                    int remaining = data.getChoiceTimeRemaining();

                    // Mettre à jour la BossBar
                    BossBar bar = choiceBossBars.get(playerId);
                    if (bar != null) {
                        bar.setProgress(Math.max(0, remaining / 30.0));
                        bar.setTitle("§6§l⬆ ASCENSION §8- §7/asc pour muter §8- §c" + remaining + "s");
                    }

                    // Timeout : choix aléatoire
                    if (remaining <= 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            forceRandomChoice(player);
                        });
                    }
                }

                // ============ FLÉAU AMBULANT (DAMAGE_AURA) ============
                // 1% HP/s aux mobs dans un rayon de 5 blocs
                if (data.hasMutation(Mutation.FLEAU_AMBULANT)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        applyDamageAura(player);
                    });
                }

                // ============ VENT DE LA MORT (PHASE_THROUGH) ============
                // Sprint = traverse les mobs (simulé par speed boost + knockback resistance)
                if (data.hasMutation(Mutation.VENT_DE_LA_MORT)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        applyPhaseThrough(player);
                    });
                }
            }
        }, 20L, 20L); // Toutes les secondes
    }

    /**
     * Applique l'effet Vent de la Mort (traverser les mobs en sprintant)
     */
    private void applyPhaseThrough(Player player) {
        if (!player.isOnline()) return;

        if (player.isSprinting()) {
            // Effet de vitesse burst
            if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 25, 1, false, false, false
                ));
            }

            // Repousser les mobs proches (simule le passage à travers)
            for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity instanceof Player) continue;
                if (!entity.hasMetadata("zombiez_mob")) continue;

                // Pousser le mob sur le côté
                org.bukkit.util.Vector push = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(0.5);
                push.setY(0.1);
                entity.setVelocity(push);
            }

            // Particules de vent légères
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 2, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /**
     * Applique l'aura de dégâts du Fléau Ambulant
     * 1% HP/s aux mobs ZombieZ dans un rayon de 5 blocs
     */
    private void applyDamageAura(Player player) {
        if (!player.isOnline()) return;

        Location loc = player.getLocation();
        boolean hitAny = false;

        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity instanceof Player) continue;
            if (!entity.hasMetadata("zombiez_mob")) continue;

            // 1% de la vie max du mob par seconde
            double damage = living.getMaxHealth() * 0.01;
            living.damage(damage, player);
            hitAny = true;
        }

        // Effet visuel léger si on touche des mobs
        if (hitAny) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 1, 0), 8, 2, 1, 2, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 0, 0), 0.8f));
        }
    }

    /**
     * Arrête la tâche de mise à jour
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        // Nettoyer les BossBars
        choiceBossBars.values().forEach(BossBar::removeAll);
        choiceBossBars.clear();
        playerAscensions.clear();
    }

    // ==================== GESTION DES JOUEURS ====================

    /**
     * Obtient ou crée les données d'un joueur
     */
    public AscensionData getOrCreateData(Player player) {
        return playerAscensions.computeIfAbsent(player.getUniqueId(),
            id -> new AscensionData(id));
    }

    /**
     * Obtient les données d'un joueur (peut être null)
     */
    public AscensionData getData(Player player) {
        return playerAscensions.get(player.getUniqueId());
    }

    /**
     * Supprime les données d'un joueur
     */
    public void removeData(Player player) {
        playerAscensions.remove(player.getUniqueId());
        BossBar bar = choiceBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    // ==================== GESTION DES KILLS ====================

    /**
     * Enregistre un kill et vérifie les stades
     */
    public void registerKill(Player player) {
        AscensionData data = getOrCreateData(player);
        int newStage = data.registerKill();

        if (newStage > 0) {
            // Nouveau stade atteint !
            triggerStageUp(player, data, newStage);
        }

        // Incrémenter les compteurs spéciaux
        updateKillCounters(player, data);
    }

    /**
     * Met à jour les compteurs de kills pour les effets spéciaux
     */
    private void updateKillCounters(Player player, AscensionData data) {
        // Nova Mortelle : explosion tous les 25 kills
        if (data.hasMutation(Mutation.NOVA_MORTELLE)) {
            int count = data.getNovaKillCounter().incrementAndGet();
            if (count >= 25) {
                data.getNovaKillCounter().set(0);
                triggerNovaExplosion(player);
            }
        }

        // Économiste : +50 pts tous les 10 kills
        if (data.hasMutation(Mutation.ECONOMISTE)) {
            int count = data.getMilestoneKillCounter().incrementAndGet();
            if (count >= 10) {
                data.getMilestoneKillCounter().set(0);
                plugin.getEconomyManager().addPoints(player, 50);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }

        // Favori de la Chance : item Rare+ tous les 50 kills
        if (data.hasMutation(Mutation.FAVORI_DE_LA_CHANCE)) {
            int count = data.getGuaranteedRareCounter().incrementAndGet();
            if (count >= 50) {
                data.getGuaranteedRareCounter().set(0);
                // Marquer pour drop garanti au prochain kill
                // Géré dans le loot system
            }
        }

        // Stacks sur kill
        if (data.hasMutation(Mutation.SOIF_INSATIABLE)) {
            data.addStack(AscensionData.StackType.LIFESTEAL);
        }
        if (data.hasMutation(Mutation.VELOCITE)) {
            data.addStack(AscensionData.StackType.SPEED);
        }
        if (data.hasMutation(Mutation.DANSE_MACABRE)) {
            data.addStack(AscensionData.StackType.CRIT);
        }
        if (data.hasMutation(Mutation.CASCADE_SANGLANTE)) {
            data.addStack(AscensionData.StackType.CASCADE);
        }
    }

    // ==================== GESTION DES STADES ====================

    /**
     * Déclenche un passage de stade
     */
    private void triggerStageUp(Player player, AscensionData data, int newStage) {
        // Générer les 3 mutations proposées
        List<Mutation> choices = generateMutationChoices(data, newStage);
        data.setChoicePending(true, choices);

        // Notification sonore
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);

        // Créer la BossBar de timer
        BossBar bar = Bukkit.createBossBar(
            "§6§l⬆ ASCENSION PRÊTE §8- §7/asc pour muter §8- §c30s",
            BarColor.YELLOW,
            BarStyle.SOLID
        );
        bar.addPlayer(player);
        choiceBossBars.put(player.getUniqueId(), bar);

        // Particules légères
        player.getWorld().spawnParticle(
            Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0),
            10, 0.5, 0.5, 0.5, 0
        );
    }

    /**
     * Génère 3 mutations aléatoires pour le choix
     */
    private List<Mutation> generateMutationChoices(AscensionData data, int stage) {
        List<Mutation> available = new ArrayList<>();
        Set<Mutation> alreadyHas = new HashSet<>(data.getActiveMutations());

        // Collecter les mutations disponibles pour ce stade
        for (Mutation mutation : Mutation.values()) {
            if (alreadyHas.contains(mutation)) continue;
            if (mutation.getTier().isAvailableAtStage(stage)) {
                available.add(mutation);
            }
        }

        // Mélanger et prendre 3
        Collections.shuffle(available);

        // Essayer d'avoir une mutation de chaque souche si possible
        List<Mutation> choices = new ArrayList<>();
        Map<MutationStrain, List<Mutation>> byStrain = available.stream()
            .collect(Collectors.groupingBy(Mutation::getStrain));

        // Une de chaque souche si possible
        for (MutationStrain strain : MutationStrain.values()) {
            List<Mutation> strainMutations = byStrain.getOrDefault(strain, Collections.emptyList());
            if (!strainMutations.isEmpty() && choices.size() < 3) {
                choices.add(strainMutations.get(0));
            }
        }

        // Compléter avec des aléatoires si on n'a pas 3
        for (Mutation m : available) {
            if (choices.size() >= 3) break;
            if (!choices.contains(m)) {
                choices.add(m);
            }
        }

        return choices;
    }

    /**
     * Force un choix aléatoire (timeout)
     */
    private void forceRandomChoice(Player player) {
        AscensionData data = getData(player);
        if (data == null || !data.isChoicePending()) return;

        List<Mutation> choices = data.getPendingChoices();
        if (choices == null || choices.isEmpty()) return;

        // Choisir aléatoirement
        Mutation chosen = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        selectMutation(player, chosen, true);
    }

    // ==================== SÉLECTION DE MUTATION ====================

    /**
     * Sélectionne une mutation pour un joueur
     */
    public void selectMutation(Player player, Mutation mutation, boolean forced) {
        AscensionData data = getData(player);
        if (data == null) return;

        // Vérifier que le choix est valide
        if (!data.isChoicePending()) return;
        List<Mutation> choices = data.getPendingChoices();
        if (choices == null || !choices.contains(mutation)) return;

        // Ajouter la mutation
        data.addMutation(mutation);

        // Retirer la BossBar
        BossBar bar = choiceBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }

        // Feedback
        MutationStrain strain = mutation.getStrain();
        player.playSound(player.getLocation(), strain.getSelectionSound(), 0.8f, strain.getSoundPitch());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);

        // Particules légères
        player.getWorld().spawnParticle(
            Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0),
            15, 0.5, 0.5, 0.5, 0
        );

        // Message dans le chat
        if (forced) {
            player.sendMessage("§8[§6Ascension§8] §7Temps écoulé ! Mutation aléatoire: " + mutation.getFormattedName());
        } else {
            player.sendMessage("§8[§6Ascension§8] §aMutation acquise: " + mutation.getFormattedName());
        }
    }

    // ==================== EFFETS SPÉCIAUX ====================

    /**
     * Déclenche une explosion de Nova Mortelle
     */
    private void triggerNovaExplosion(Player player) {
        Location loc = player.getLocation();

        // Son
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Particules
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 2, 1, 2, 0);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 50, 4, 2, 4, 0.1);

        // Dégâts AoE
        double damage = 50.0; // Dégâts fixes
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                // Vérifier que c'est un mob ZombieZ
                if (entity.hasMetadata("zombiez_mob")) {
                    living.damage(damage, player);
                }
            }
        }

        player.sendMessage("§c§l💀 NOVA MORTELLE! §7Explosion dévastatrice!");
    }

    /**
     * Déclenche une chaîne critique (Lame Fantomatique)
     * 20% chance que le crit chaîne à 1 ennemi proche
     */
    public void triggerCritChain(Player player, LivingEntity originalTarget, double damage) {
        AscensionData data = getData(player);
        if (data == null) return;

        // Cooldown 300ms pour éviter les chaînes infinies
        if (!data.canTriggerEffect("crit_chain", 300)) return;

        // 20% chance
        if (ThreadLocalRandom.current().nextDouble() >= 0.20) return;

        // Trouver un ennemi proche (dans 4 blocs)
        LivingEntity chainTarget = null;
        double closestDist = 16; // 4 blocs au carré

        for (Entity entity : originalTarget.getNearbyEntities(4, 4, 4)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity instanceof Player) continue;
            if (!entity.hasMetadata("zombiez_mob")) continue;
            if (entity.equals(originalTarget)) continue;

            double dist = entity.getLocation().distanceSquared(originalTarget.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                chainTarget = living;
            }
        }

        if (chainTarget != null) {
            // Appliquer 50% des dégâts au mob chaîné
            double chainDamage = damage * 0.5;
            chainTarget.damage(chainDamage, player);

            // Effet visuel de chaîne
            Location from = originalTarget.getEyeLocation();
            Location to = chainTarget.getEyeLocation();
            drawChainParticles(from, to);

            // Son
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 2.0f);
        }
    }

    /**
     * Dessine des particules entre deux points (pour crit chain)
     */
    private void drawChainParticles(Location from, Location to) {
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Déclenche une explosion d'Éclats d'Os
     */
    public void triggerBoneShardsExplosion(Player player, Location loc, double damage) {
        AscensionData data = getData(player);
        if (data == null) return;

        // Cooldown 500ms
        if (!data.canTriggerEffect("bone_shards", 500)) return;

        // Son léger
        player.getWorld().playSound(loc, Sound.BLOCK_BONE_BLOCK_BREAK, 0.6f, 1.5f);

        // Particules
        player.getWorld().spawnParticle(Particle.BLOCK, loc, 20, 1, 1, 1, 0,
            org.bukkit.Material.BONE_BLOCK.createBlockData());

        // Dégâts AoE (3 blocs)
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                if (entity.hasMetadata("zombiez_mob")) {
                    living.damage(damage * 0.3, player);
                }
            }
        }
    }

    // ==================== STATS ET BONUS ====================

    /**
     * Obtient les bonus de stats d'un joueur (pour merge avec équipement)
     */
    public Map<StatType, Double> getStatBonuses(Player player) {
        AscensionData data = getData(player);
        if (data == null) return Collections.emptyMap();

        Map<StatType, Double> bonuses = new EnumMap<>(data.getCachedStatBonuses());

        // Ajouter les bonus de stacks dynamiques
        double stackLifesteal = data.getStackingLifesteal().get() * 5.0;
        if (stackLifesteal > 0) {
            bonuses.merge(StatType.LIFESTEAL, stackLifesteal, Double::sum);
        }

        double stackSpeed = data.getStackingSpeed().get() * 3.0;
        if (stackSpeed > 0) {
            bonuses.merge(StatType.MOVEMENT_SPEED, stackSpeed, Double::sum);
        }

        double stackCrit = data.getStackingCrit().get() * 2.0;
        if (stackCrit > 0) {
            bonuses.merge(StatType.CRIT_CHANCE, stackCrit, Double::sum);
        }

        // Spectre de Guerre: <30% HP = +25% Speed (esquive gérée dans CombatListener)
        if (data.hasMutation(Mutation.SPECTRE_DE_GUERRE)) {
            double playerHpPercent = player.getHealth() / player.getMaxHealth() * 100;
            if (playerHpPercent < 30) {
                bonuses.merge(StatType.MOVEMENT_SPEED, 25.0, Double::sum);
            }
        }

        // Fureur du Dernier Souffle: <20% HP = +20% Speed
        if (data.hasMutation(Mutation.FUREUR_DERNIER_SOUFFLE)) {
            double playerHpPercent = player.getHealth() / player.getMaxHealth() * 100;
            if (playerHpPercent < 20) {
                bonuses.merge(StatType.MOVEMENT_SPEED, 20.0, Double::sum);
            }
        }

        return bonuses;
    }

    /**
     * Vérifie si un joueur a une mutation
     */
    public boolean hasMutation(Player player, Mutation mutation) {
        AscensionData data = getData(player);
        return data != null && data.hasMutation(mutation);
    }

    /**
     * Vérifie si un joueur a un effet
     */
    public boolean hasEffect(Player player, Mutation.MutationEffect effect) {
        AscensionData data = getData(player);
        return data != null && data.hasEffect(effect);
    }

    // ==================== RESET ET MORT ====================

    /**
     * Reset les données d'un joueur (mort)
     */
    public void resetPlayer(Player player) {
        AscensionData data = getData(player);
        if (data != null) {
            data.reset();
        }

        // Retirer la BossBar
        BossBar bar = choiceBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * Assure une mutation (payée en gemmes)
     */
    public boolean insureMutation(Player player, Mutation mutation) {
        AscensionData data = getData(player);
        if (data == null) return false;

        // Vérifier que le joueur a la mutation
        if (!data.hasMutation(mutation)) return false;

        // Vérifier le coût
        int cost = mutation.getInsuranceCost();
        if (!plugin.getEconomyManager().hasGems(player, cost)) return false;

        // Payer et assurer
        plugin.getEconomyManager().removeGems(player, cost);
        data.setInsuredMutation(mutation);

        player.sendMessage("§8[§6Ascension§8] §a" + mutation.getFormattedName() +
            " §7assurée pour §e" + cost + " 💎");
        return true;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Ouvre le GUI d'Ascension
     */
    public void openAscensionGUI(Player player) {
        AscensionData data = getOrCreateData(player);
        AscensionGUI.open(plugin, player, data);
    }

    /**
     * Obtient la progression pour l'ActionBar
     */
    public String getActionBarProgress(Player player) {
        AscensionData data = getData(player);
        if (data == null) {
            return "§8⬆0/50";
        }

        int kills = data.getSessionKills().get();
        int nextStage = data.getKillsForNextStage();

        if (data.isChoicePending()) {
            return "§a§l⬆PRÊT";
        }

        if (data.getCurrentStage() >= 10) {
            return "§6⬆MAX";
        }

        return "§7⬆" + kills + "/" + nextStage;
    }
}
