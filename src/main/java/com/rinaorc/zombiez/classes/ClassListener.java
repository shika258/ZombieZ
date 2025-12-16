package com.rinaorc.zombiez.classes;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.gui.ClassSelectionGUI;
import com.rinaorc.zombiez.classes.mutations.DailyMutation;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener pour intégrer le système de classes avec le gameplay
 * - Modifie les dégâts selon la classe
 * - Gère l'XP de classe
 * - Vérifie les restrictions d'armes
 * - Applique les effets des mutations
 */
public class ClassListener implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final NamespacedKey classWeaponKey;

    public ClassListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getClassManager();
        this.classWeaponKey = new NamespacedKey(plugin, "class_weapon_id");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * À la connexion, afficher les mutations et proposer la classe si nécessaire
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ClassData data = classManager.getClassData(player);

            // Afficher les mutations
            if (classManager.getMutationManager().isSpecialDay()) {
                player.sendMessage("");
                player.sendMessage("§6§l✦ JOUR SPÉCIAL! ✦");
                player.sendMessage("§7Des mutations rares sont actives aujourd'hui!");
                player.sendMessage("§8Utilisez /mutations pour voir les détails.");
                player.sendMessage("");
            }

            // Proposer de choisir une classe si pas de classe
            if (!data.hasClass()) {
                PlayerData pData = plugin.getPlayerDataManager().getPlayer(player);
                if (pData != null && pData.getLevel().get() >= 5) {
                    player.sendMessage("");
                    player.sendMessage("§e§l✦ SYSTÈME DE CLASSES DISPONIBLE ✦");
                    player.sendMessage("§7Vous pouvez maintenant choisir une classe!");
                    player.sendMessage("§7Utilisez §e/class§7 pour commencer.");
                    player.sendMessage("");
                }
            }
        }, 60L); // 3 secondes après connexion
    }

    /**
     * Modifie les dégâts infligés selon la classe et les talents
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        double damage = event.getDamage();
        double originalDamage = damage;

        // Multiplicateur de classe de base
        damage *= classManager.getTotalDamageMultiplier(player);

        // Bonus spécifiques selon le type de classe et l'arme
        ClassType classType = data.getSelectedClass();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        // Berserker = bonus mêlée
        if (classType == ClassType.BERSERKER && isMeleeWeapon(weapon)) {
            damage *= 1.10; // 10% bonus mêlée supplémentaire
        }

        // Sniper = bonus headshot (géré ailleurs, mais on ajoute un bonus de base)
        if (classType == ClassType.SNIPER && isRangedWeapon(weapon)) {
            // Le bonus headshot est appliqué dans CombatListener
        }

        // Appliquer les mutations
        if (event.getEntity() instanceof Zombie) {
            // Mutation player damage s'applique déjà dans getTotalDamageMultiplier
        }

        // Stats de classe
        data.addDamageDealt((long) damage);

        event.setDamage(damage);
    }

    /**
     * Gère les dégâts reçus selon la classe
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        double damage = event.getDamage();

        // Multiplicateur de santé appliqué inversement aux dégâts
        double healthMult = classManager.getTotalHealthMultiplier(player);
        if (healthMult > 1.0) {
            // Plus de HP = moins de dégâts relatifs
            damage *= (1.0 / healthMult);
        }

        // Mutations
        double zombieDamageMult = classManager.getMutationManager()
            .getMultiplier(DailyMutation.MutationEffect.ZOMBIE_DAMAGE);
        damage *= zombieDamageMult;

        // Stats
        data.addDamageReceived((long) damage);

        event.setDamage(damage);
    }

    /**
     * Gère l'XP de classe et les rewards sur kill
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(zombie.getKiller() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        // XP de classe basé sur l'XP normal
        int baseXp = event.getDroppedExp();
        long classXp = (long) (baseXp * 0.5); // 50% de l'XP normal

        // Bonus pour élites/boss (vérifier via les métadonnées ou le nom)
        String zombieName = zombie.getCustomName();
        if (zombieName != null) {
            if (zombieName.contains("Élite") || zombieName.contains("Elite")) {
                classXp *= 2;
            } else if (zombieName.contains("Boss") || zombieName.contains("BOSS")) {
                classXp *= 4;
            }
        }

        // Mutation XP bonus
        double xpMult = classManager.getMutationManager()
            .getMultiplier(DailyMutation.MutationEffect.XP_GAIN);
        classXp = (long) (classXp * xpMult);

        // Ajouter l'XP et vérifier level up
        if (data.addClassXp(classXp)) {
            classManager.handleClassLevelUp(player);
        }

        // Stats
        data.addClassKill();

        // Régénérer un peu d'énergie
        data.regenerateEnergy(10);
    }

    /**
     * Vérifie si le joueur peut utiliser une arme de classe
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem == null || newItem.getType().isAir()) return;

        // Vérifier si c'est une arme de classe
        String weaponId = getClassWeaponId(newItem);
        if (weaponId == null) return;

        // Vérifier si le joueur peut l'utiliser
        String restriction = classManager.getWeaponRestrictionMessage(player, weaponId);
        if (restriction != null) {
            player.sendMessage(restriction);
            // Ne pas annuler l'event, mais l'arme sera inutilisable en combat
        }
    }

    /**
     * Bloque l'utilisation d'armes de classe non autorisées
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeaponUse(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        String weaponId = getClassWeaponId(weapon);

        if (weaponId != null && !classManager.canUseClassWeapon(player, weaponId)) {
            event.setCancelled(true);
            player.sendMessage(classManager.getWeaponRestrictionMessage(player, weaponId));
        }
    }

    /**
     * Gestion des raccourcis de compétences (touches 1-3 avec item spécial)
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Vérifier si c'est un item de compétence
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey skillKey = new NamespacedKey(plugin, "skill_slot");

        if (pdc.has(skillKey, PersistentDataType.STRING)) {
            String slot = pdc.get(skillKey, PersistentDataType.STRING);
            if (slot != null) {
                event.setCancelled(true);
                classManager.useSkill(player, slot);
            }
        }
    }

    // ==================== UTILITAIRES ====================

    private String getClassWeaponId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(classWeaponKey, PersistentDataType.STRING);
    }

    private boolean isMeleeWeapon(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().name();
        return type.contains("SWORD") || type.contains("AXE") || type.contains("MACE");
    }

    private boolean isRangedWeapon(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().name();
        return type.contains("BOW") || type.contains("CROSSBOW") || type.equals("TRIDENT");
    }
}
