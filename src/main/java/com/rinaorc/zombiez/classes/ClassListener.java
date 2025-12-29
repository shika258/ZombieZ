package com.rinaorc.zombiez.classes;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.gui.ClassSelectionGUI;
import com.rinaorc.zombiez.classes.mutations.DailyMutation;
import com.rinaorc.zombiez.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener pour integrer le systeme de classes avec le gameplay
 * - Modifie les degats selon la classe
 * - Gere l'XP de classe
 * - Applique les effets des mutations
 */
public class ClassListener implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;

    public ClassListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getClassManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * A la connexion, afficher les mutations et proposer la classe si necessaire
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ClassData data = classManager.getClassData(player);

            // Afficher les mutations
            if (classManager.getMutationManager().isSpecialDay()) {
                player.sendMessage("");
                player.sendMessage("§6§l+ JOUR SPECIAL! +");
                player.sendMessage("§7Des mutations rares sont actives aujourd'hui!");
                player.sendMessage("§8Utilisez /mutations pour voir les details.");
                player.sendMessage("");
            }

            // Proposer de choisir une classe si pas de classe
            if (!data.hasClass()) {
                PlayerData pData = plugin.getPlayerDataManager().getPlayer(player);
                if (pData != null && pData.getLevel().get() >= 5) {
                    player.sendMessage("");
                    player.sendMessage("§e§l+ SYSTEME DE CLASSES DISPONIBLE +");
                    player.sendMessage("§7Vous pouvez maintenant choisir une classe!");
                    player.sendMessage("§7Utilisez §e/class§7 pour commencer.");
                    player.sendMessage("");
                }
            }
        }, 60L); // 3 secondes apres connexion
    }

    /**
     * Modifie les degats infliges selon la classe
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        double damage = event.getDamage();

        // Multiplicateur de classe de base
        damage *= classManager.getClassDamageMultiplier(player);

        // Bonus specifiques selon le type de classe et l'arme
        ClassType classType = data.getSelectedClass();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        // Guerrier = bonus melee
        if (classType == ClassType.GUERRIER && isMeleeWeapon(weapon)) {
            damage *= 1.10; // 10% bonus melee supplementaire
        }

        // Stats de classe
        data.addDamageDealt((long) damage);

        event.setDamage(damage);
    }

    /**
     * Gere les degats recus selon la classe
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        double damage = event.getDamage();

        // Multiplicateur de sante applique inversement aux degats
        double healthMult = classManager.getClassHealthMultiplier(player);
        if (healthMult > 1.0) {
            // Plus de HP = moins de degats relatifs
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
     * Gère les stats de kill pour les classes
     * NOTE: L'XP de classe est maintenant donnée via EconomyManager.addXp() (30% de l'XP standard)
     * pour que TOUS les bonus d'XP (events, élites, assists, etc.) comptent pour les classes
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        // Vérifier si c'est un mob ZombieZ (zombie, squelette, etc.)
        if (!mob.hasMetadata("zombiez_type")) return;
        if (!(mob.getKiller() instanceof Player player)) return;

        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) return;

        // Stats de kill uniquement (l'XP est gérée par EconomyManager)
        data.addClassKill();
    }

    // ==================== UTILITAIRES ====================

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
