package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Refuge;
import com.rinaorc.zombiez.zones.gui.RefugeGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Listener pour les interactions des joueurs
 * Gère les interactions avec les blocs spéciaux, PNJs, etc.
 */
public class InteractListener implements Listener {

    private final ZombieZPlugin plugin;

    public InteractListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère les interactions avec les blocs
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignorer la main secondaire pour éviter les doubles événements
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        // Clic droit sur un bloc
        if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            handleBlockInteraction(player, block, event);
        }

        // Clic droit avec un item en main
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null) {
                handleItemInteraction(player, item, event);
            }
        }
    }

    /**
     * Gère les interactions avec les blocs spéciaux
     */
    private void handleBlockInteraction(Player player, Block block, PlayerInteractEvent event) {
        Material type = block.getType();

        switch (type) {
            case ENDER_CHEST -> {
                // Coffre de checkpoint/stockage personnel
                handleCheckpointChest(player, block, event);
            }
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> {
                // Enclume pour réparation/amélioration
                handleAnvil(player, block, event);
            }
            case ENCHANTING_TABLE -> {
                // Table d'enchantement custom
                handleEnchantingTable(player, block, event);
            }
            case BEACON -> {
                // Beacon de refuge (activation checkpoint)
                handleRefugeBeacon(player, block, event);
            }
            case LODESTONE -> {
                // Pierre de téléportation
                handleLodestone(player, block, event);
            }
            default -> {
                // Autres blocs - comportement par défaut
            }
        }
    }

    /**
     * Gère l'interaction avec un coffre de checkpoint
     */
    private void handleCheckpointChest(Player player, Block block, PlayerInteractEvent event) {
        // Pour l'instant, comportement par défaut
        // TODO: Implémenter le stockage personnel par zone
    }

    /**
     * Gère l'interaction avec une enclume (forge)
     */
    private void handleAnvil(Player player, Block block, PlayerInteractEvent event) {
        // TODO: Ouvrir le menu de forge custom
        // event.setCancelled(true);
        // openForgeMenu(player);
    }

    /**
     * Gère l'interaction avec une table d'enchantement
     */
    private void handleEnchantingTable(Player player, Block block, PlayerInteractEvent event) {
        // TODO: Ouvrir le menu d'enchantement custom (manipulation d'affixes)
        // event.setCancelled(true);
        // openEnchantMenu(player);
    }

    /**
     * Gère l'interaction avec un beacon de refuge
     * Affiche les informations du refuge, débloque si possible, et ouvre le menu
     */
    private void handleRefugeBeacon(Player player, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Trouver le refuge par la position exacte du beacon
        var refugeManager = plugin.getRefugeManager();
        Refuge refuge = refugeManager != null ? refugeManager.getRefugeAtBeacon(block.getLocation()) : null;

        if (refuge == null) {
            // Fallback: utiliser l'ancien système basé sur la zone
            var zone = plugin.getZoneManager().getZoneAt(block.getLocation());
            if (zone != null && zone.getRefugeId() > 0) {
                refuge = refugeManager != null ? refugeManager.getRefugeById(zone.getRefugeId()) : null;
            }
        }

        if (refuge == null) {
            MessageUtils.send(player, "§7Ce beacon n'est pas configuré comme refuge.");
            return;
        }

        // Tenter de débloquer le refuge si ce n'est pas déjà fait
        boolean justUnlocked = tryUnlockRefuge(player, data, refuge);

        // Afficher les informations du refuge dans le chat
        sendRefugeInfo(player, data, refuge);

        // Ouvrir le menu refuge après un court délai
        final Refuge finalRefuge = refuge;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new RefugeGUI(plugin, player).open();
        }, justUnlocked ? 20L : 5L); // Délai plus long si débloqué pour laisser le temps au joueur de voir le message
    }

    /**
     * Tente de débloquer un refuge si le joueur remplit toutes les conditions
     * @return true si le refuge vient d'être débloqué
     */
    private boolean tryUnlockRefuge(Player player, PlayerData data, Refuge refuge) {
        int currentCheckpoint = data.getCurrentCheckpoint().get();
        int refugeId = refuge.getId();
        int playerLevel = data.getLevel().get();
        long playerPoints = data.getPoints().get();

        // Déjà débloqué ?
        if (currentCheckpoint >= refugeId) {
            return false;
        }

        // Vérifier la progression séquentielle : on ne peut débloquer que le prochain refuge
        // Le refuge 1 peut être débloqué si currentCheckpoint == 0
        // Le refuge N peut être débloqué si currentCheckpoint == N-1
        if (currentCheckpoint != refugeId - 1) {
            MessageUtils.send(player, "§c✖ Vous devez d'abord débloquer les refuges précédents!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return false;
        }

        // Vérifier le niveau requis
        if (playerLevel < refuge.getRequiredLevel()) {
            MessageUtils.send(player, "§c✖ Niveau insuffisant!");
            MessageUtils.send(player, "§7Niveau requis: §e" + refuge.getRequiredLevel() + " §7(Vous: §c" + playerLevel + "§7)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return false;
        }

        // Vérifier les points requis
        long cost = refuge.getCost();
        if (playerPoints < cost) {
            MessageUtils.send(player, "§c✖ Points insuffisants!");
            MessageUtils.send(player, "§7Coût: §6" + EconomyManager.formatPoints(cost) + " §7(Vous: §c" + EconomyManager.formatPoints(playerPoints) + "§7)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return false;
        }

        // Toutes les conditions sont remplies : débloquer le refuge !
        data.removePoints(cost);
        data.setCheckpoint(refugeId);

        // Feedback visuel et sonore
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        // Particules autour du beacon
        player.getWorld().spawnParticle(
            org.bukkit.Particle.TOTEM_OF_UNDYING,
            player.getLocation().add(0, 1, 0),
            50, 0.5, 1, 0.5, 0.1
        );

        // Messages de confirmation
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§a§l═════════════════════════");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §a§l✓ REFUGE DÉBLOQUÉ!");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §e§l🏠 " + refuge.getName());
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §7Checkpoint activé! Vous réapparaîtrez");
        MessageUtils.sendRaw(player, "  §7ici en cas de mort.");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §6-" + EconomyManager.formatPoints(cost) + " Points");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§a§l═════════════════════════");
        MessageUtils.sendRaw(player, "");

        // Title pour plus d'impact
        MessageUtils.sendTitle(player, "§a§l✓ REFUGE DÉBLOQUÉ", "§e" + refuge.getName(), 10, 40, 10);

        return true;
    }

    /**
     * Affiche les informations détaillées d'un refuge dans le chat
     */
    private void sendRefugeInfo(Player player, PlayerData data, Refuge refuge) {
        int currentCheckpoint = data.getCurrentCheckpoint().get();
        int playerLevel = data.getLevel().get();
        long playerPoints = data.getPoints().get();

        boolean isUnlocked = currentCheckpoint >= refuge.getId();
        boolean isCurrentCheckpoint = currentCheckpoint == refuge.getId();
        boolean canUnlock = playerLevel >= refuge.getRequiredLevel();
        boolean canAfford = playerPoints >= refuge.getCost();

        // Son selon le statut
        if (isUnlocked) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.8f);
        }

        // Header
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§e§l═════════════════════════");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §6§l🏠 " + refuge.getName().toUpperCase());

        // Description
        if (refuge.getDescription() != null && !refuge.getDescription().isEmpty()) {
            MessageUtils.sendRaw(player, "  §7§o\"" + refuge.getDescription() + "\"");
        }
        MessageUtils.sendRaw(player, "");

        // Statut de déverrouillage
        if (isCurrentCheckpoint) {
            MessageUtils.sendRaw(player, "  §a§l✓ CHECKPOINT ACTIF");
            MessageUtils.sendRaw(player, "  §7Vous réapparaîtrez ici en cas de mort.");
        } else if (isUnlocked) {
            MessageUtils.sendRaw(player, "  §a✓ Débloqué");
            MessageUtils.sendRaw(player, "  §7Vous pouvez vous téléporter ici.");
        } else {
            MessageUtils.sendRaw(player, "  §c✖ Verrouillé");

            // Raison du verrouillage
            if (!canUnlock) {
                MessageUtils.sendRaw(player, "  §c⚠ Niveau requis: §e" + refuge.getRequiredLevel() + " §7(Vous: §c" + playerLevel + "§7)");
            } else if (!canAfford) {
                MessageUtils.sendRaw(player, "  §c⚠ Points requis: §6" + EconomyManager.formatPoints(refuge.getCost()));
                MessageUtils.sendRaw(player, "  §7Vos points: §c" + EconomyManager.formatPoints(playerPoints));
            } else {
                MessageUtils.sendRaw(player, "  §7Coût: §6" + EconomyManager.formatPoints(refuge.getCost()) + " Points");
                MessageUtils.sendRaw(player, "  §a➤ Utilisez le menu pour débloquer!");
            }
        }

        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "  §7Coût d'activation: §6" + EconomyManager.formatPoints(refuge.getCost()) + " Points");
        MessageUtils.sendRaw(player, "  §7Niveau minimum: §e" + refuge.getRequiredLevel());
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§e§l═════════════════════════");
        MessageUtils.sendRaw(player, "  §8§oOuverture du menu des refuges...");
        MessageUtils.sendRaw(player, "");
    }

    /**
     * Obtient le coût d'activation d'un checkpoint (fallback)
     */
    private long getCheckpointCost(int refugeId) {
        // Coût croissant selon le refuge (utilisé si le refuge n'est pas dans refuges.yml)
        return switch (refugeId) {
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 200;
            case 4 -> 350;
            case 5 -> 500;
            case 6 -> 750;
            case 7 -> 1000;
            case 8 -> 1500;
            case 9 -> 2000;
            case 10 -> 3000;
            default -> refugeId * 100L;
        };
    }

    /**
     * Gère l'interaction avec une lodestone (téléportation)
     */
    private void handleLodestone(Player player, Block block, PlayerInteractEvent event) {
        // TODO: Ouvrir le menu de téléportation rapide
        event.setCancelled(true);
        MessageUtils.send(player, "§7Fonctionnalité de téléportation §eBientôt disponible!");
    }

    /**
     * Gère les interactions avec des items spéciaux
     */
    private void handleItemInteraction(Player player, ItemStack item, PlayerInteractEvent event) {
        // Vérifier si c'est un item custom (via NBT/PersistentData)
        if (!item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        // TODO: Vérifier les items custom (consommables, etc.)
        // Exemple: Bandage, Kit médical, etc.
    }

    /**
     * Gère les interactions avec les entités (PNJs)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Interaction avec un villageois (marchand)
        if (entity instanceof Villager villager) {
            handleVillagerInteraction(player, villager, event);
        }
    }

    /**
     * Gère l'interaction avec un villageois/marchand
     */
    private void handleVillagerInteraction(Player player, Villager villager, PlayerInteractEntityEvent event) {
        // Vérifier si c'est un PNJ custom via les métadonnées
        if (villager.hasMetadata("zombiez_npc")) {
            event.setCancelled(true);
            
            String npcType = villager.getMetadata("zombiez_npc").get(0).asString();
            
            switch (npcType) {
                case "merchant" -> openMerchantMenu(player);
                case "blacksmith" -> openBlacksmithMenu(player);
                case "enchanter" -> openEnchanterMenu(player);
                case "quest_giver" -> openQuestMenu(player);
                default -> MessageUtils.send(player, "§7Ce PNJ n'a rien à dire...");
            }
        }
    }

    /**
     * Ouvre le menu du marchand
     */
    private void openMerchantMenu(Player player) {
        // TODO: Implémenter le menu marchand
        MessageUtils.send(player, "§eMarchand: §7\"Bienvenue, survivant! Que puis-je faire pour toi?\"");
    }

    /**
     * Ouvre le menu du forgeron
     */
    private void openBlacksmithMenu(Player player) {
        // TODO: Implémenter le menu forgeron
        MessageUtils.send(player, "§6Forgeron: §7\"Je peux réparer ton équipement!\"");
    }

    /**
     * Ouvre le menu de l'enchanteur
     */
    private void openEnchanterMenu(Player player) {
        // TODO: Implémenter le menu enchanteur
        MessageUtils.send(player, "§5Enchanteur: §7\"Je peux améliorer les affixes de tes items...\"");
    }

    /**
     * Ouvre le menu des quêtes
     */
    private void openQuestMenu(Player player) {
        // TODO: Implémenter le menu quêtes
        MessageUtils.send(player, "§bDonneur de quêtes: §7\"J'ai des missions pour toi!\"");
    }
}
