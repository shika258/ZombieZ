package com.rinaorc.zombiez.classes.beasts;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

/**
 * Listener pour les événements liés aux bêtes de la Voie des Bêtes.
 * Gère les dégâts, les morts, les connexions et les interactions.
 */
public class BeastListener implements Listener {

    private final ZombieZPlugin plugin;
    private final BeastManager beastManager;

    public BeastListener(ZombieZPlugin plugin, BeastManager beastManager) {
        this.plugin = plugin;
        this.beastManager = beastManager;
    }

    // === GESTION DES DÉGÂTS ===

    /**
     * Empêche les bêtes invincibles de prendre des dégâts.
     * L'ours est la seule bête qui peut prendre des dégâts (tank pour le joueur).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBeastDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!beastManager.isBeast(event.getEntity())) return;

        UUID ownerUuid = beastManager.getBeastOwner(entity);
        if (ownerUuid == null) return;

        // Vérifier le type de bête
        if (entity.hasMetadata(BeastManager.BEAST_TYPE_KEY)) {
            String typeStr = entity.getMetadata(BeastManager.BEAST_TYPE_KEY).get(0).asString();
            BeastType type = BeastType.valueOf(typeStr);

            // Toutes les bêtes invincibles (toutes sauf l'ours)
            if (type.isInvincible()) {
                event.setCancelled(true);
                return;
            }

            // L'ours prend les dégâts normalement (tank pour protéger le joueur)
            // Pas de partage de dégâts - l'ours absorbe tout
        }
    }

    /**
     * Empêche les bêtes d'attaquer leur propriétaire.
     * Empêche les bêtes du même propriétaire de s'attaquer entre elles.
     * Applique les dégâts calculés basés sur les stats du joueur.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBeastAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        // Cas 1: Une bête attaque quelqu'un
        if (beastManager.isBeast(damager)) {
            UUID ownerUuid = beastManager.getBeastOwner(damager);
            if (ownerUuid == null) return;

            // Ne pas attaquer le propriétaire
            if (target instanceof Player player && player.getUniqueId().equals(ownerUuid)) {
                event.setCancelled(true);
                return;
            }

            // Ne pas attaquer les autres bêtes du même propriétaire
            if (beastManager.isBeast(target)) {
                UUID targetOwner = beastManager.getBeastOwner(target);
                if (ownerUuid.equals(targetOwner)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Appliquer les dégâts et effets de la bête
            if (damager instanceof LivingEntity && target instanceof LivingEntity livingTarget) {
                applyBeastDamageAndEffects(event, damager, ownerUuid, livingTarget);
            }
        }

        // Cas 2: Un joueur attaque une cible - mettre à jour le focus pour les bêtes
        if (damager instanceof Player player && target instanceof LivingEntity living && !beastManager.isBeast(target)) {
            ClassData data = plugin.getClassManager().getClassData(player);
            if (data.hasClass() && data.getSelectedClass() == ClassType.CHASSEUR) {
                String branchId = data.getSelectedBranchId();
                if (branchId != null && branchId.contains("betes")) {
                    beastManager.setFocusTarget(player, living);
                }
            }
        }
    }

    /**
     * Applique les dégâts calculés et les effets spéciaux des attaques de bêtes.
     * Les dégâts sont basés sur les stats du joueur propriétaire.
     * Utilise la même logique que les serviteurs de l'Occultiste pour l'affichage des dégâts.
     */
    private void applyBeastDamageAndEffects(EntityDamageByEntityEvent event, Entity beast, UUID ownerUuid, LivingEntity target) {
        if (!beast.hasMetadata(BeastManager.BEAST_TYPE_KEY)) return;
        String typeStr = beast.getMetadata(BeastManager.BEAST_TYPE_KEY).get(0).asString();
        BeastType type = BeastType.valueOf(typeStr);

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) return;

        // Calculer les dégâts basés sur les stats du joueur
        double calculatedDamage = beastManager.calculateBeastDamage(owner, type);

        // Appliquer les dégâts calculés à l'événement
        event.setDamage(calculatedDamage);

        // === METADATA POUR L'INDICATEUR DE DÉGÂTS (comme les serviteurs Occultiste) ===
        // Configurer les metadata pour que CombatListener MONITOR affiche l'indicateur
        target.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
        target.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, false));
        target.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

        // === ATTRIBUTION DU LOOT AU PROPRIÉTAIRE ===
        // Enregistrer le propriétaire pour que le loot lui revienne
        if (plugin.getZombieManager().isZombieZMob(target)) {
            target.setMetadata("last_damage_player", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

            // Mettre à jour l'affichage de vie du zombie
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(target);
                }
            });
        }

        // Appliquer les effets spéciaux selon le type de bête
        // Note: Le loup gère ses dégâts et bleed via applyWolfBite dans executeWolfAbility
        switch (type) {
            case BEAR -> {
                // L'ours inflige des dégâts lourds + knockback
                target.setVelocity(target.getLocation().toVector()
                    .subtract(beast.getLocation().toVector())
                    .normalize()
                    .multiply(0.5)
                    .setY(0.3));
            }
            // Autres effets gérés dans BeastManager (lama crachat, abeille piqûre, etc.)
        }
    }

    // === GESTION DES MORTS ===

    /**
     * Gère la mort d'une bête
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBeastDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!beastManager.isBeast(entity)) return;

        beastManager.handleBeastDeath(entity);

        // Pas de drops pour les bêtes
        event.getDrops().clear();
        event.setDroppedExp(0);
    }


    // === GESTION DES PROJECTILES (Lama) ===

    /**
     * Gère l'impact du crachat du lama
     */
    @EventHandler
    public void onLlamaSpitHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof LlamaSpit spit)) return;
        if (!spit.hasMetadata("llama_owner")) return;

        String ownerUuidStr = spit.getMetadata("llama_owner").get(0).asString();
        UUID ownerUuid = UUID.fromString(ownerUuidStr);
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) return;

        if (event.getHitEntity() instanceof LivingEntity target && !beastManager.isBeast(target) && target != owner) {
            beastManager.applyLlamaSpit(owner, target);
        }

        spit.remove();
    }

    // === GESTION DU CIBLAGE ===

    /**
     * Empêche les bêtes de cibler leur propriétaire
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBeastTarget(EntityTargetEvent event) {
        if (!beastManager.isBeast(event.getEntity())) return;

        Entity target = event.getTarget();
        if (target == null) return;

        UUID ownerUuid = beastManager.getBeastOwner(event.getEntity());
        if (ownerUuid == null) return;

        // Ne pas cibler le propriétaire
        if (target instanceof Player player && player.getUniqueId().equals(ownerUuid)) {
            event.setCancelled(true);
            return;
        }

        // Ne pas cibler les autres bêtes du même propriétaire
        if (beastManager.isBeast(target)) {
            UUID targetOwner = beastManager.getBeastOwner(target);
            if (ownerUuid.equals(targetOwner)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Empêche les mobs de cibler les bêtes invincibles.
     * Les mobs ne peuvent cibler que l'ours (le tank).
     * Si l'ours est mort, les mobs peuvent cibler le joueur.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobTargetBeast(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target == null) return;

        // On ne gère que le cas où un mob cible une bête
        if (!beastManager.isBeast(target)) return;

        // Vérifier si c'est un mob hostile qui cible
        if (!(event.getEntity() instanceof org.bukkit.entity.Monster)) return;

        // Obtenir le type de bête ciblée
        if (!target.hasMetadata(BeastManager.BEAST_TYPE_KEY)) return;
        String typeStr = target.getMetadata(BeastManager.BEAST_TYPE_KEY).get(0).asString();
        BeastType type = BeastType.valueOf(typeStr);

        // Si la bête est invincible (toutes sauf l'ours), empêcher le ciblage
        if (type.isInvincible()) {
            event.setCancelled(true);

            // Essayer de rediriger vers l'ours du propriétaire
            UUID ownerUuid = beastManager.getBeastOwner(target);
            if (ownerUuid != null) {
                LivingEntity bear = beastManager.getPlayerBear(ownerUuid);
                if (bear != null && event.getEntity() instanceof org.bukkit.entity.Mob mob) {
                    // Rediriger vers l'ours
                    Bukkit.getScheduler().runTask(plugin, () -> mob.setTarget(bear));
                }
            }
        }
        // L'ours peut être ciblé normalement (il est le tank)
    }

    // === GESTION DES CONNEXIONS/DÉCONNEXIONS ===

    /**
     * Réinvoque les bêtes quand un joueur se connecte
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Délai pour laisser le temps de charger les données
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ClassData data = plugin.getClassManager().getClassData(player);
            if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

            String branchId = data.getSelectedBranchId();
            if (branchId == null || !branchId.contains("betes")) return;

            beastManager.summonBeastsForPlayer(player);
        }, 40L); // 2 secondes
    }

    /**
     * Retire les bêtes quand un joueur se déconnecte
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        beastManager.despawnAllBeasts(event.getPlayer());
    }

    /**
     * Réinvoque les bêtes quand un joueur change de monde
     */
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        String branchId = data.getSelectedBranchId();
        if (branchId == null || !branchId.contains("betes")) return;

        // Retirer les anciennes bêtes (elles sont dans l'ancien monde)
        beastManager.despawnAllBeasts(player);

        // Réinvoquer dans le nouveau monde
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            beastManager.summonBeastsForPlayer(player);
        }, 20L); // 1 seconde
    }

    // === FRÉNÉSIE DE LA RUCHE ===

    /**
     * Gère le double-sneak pour activer la Frénésie de la Ruche
     */
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        boolean isSneaking = event.isSneaking();

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != ClassType.CHASSEUR) return;

        String branchId = data.getSelectedBranchId();
        if (branchId == null || !branchId.contains("betes")) return;

        beastManager.handleSneak(player, isSneaking);
    }
}
