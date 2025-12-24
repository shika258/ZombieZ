package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Material;
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
     */
    private void handleRefugeBeacon(Player player, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Trouver le refuge par la position exacte du beacon
        var refugeManager = plugin.getRefugeManager();
        var refuge = refugeManager != null ? refugeManager.getRefugeAtBeacon(block.getLocation()) : null;

        if (refuge != null) {
            int refugeId = refuge.getId();
            int currentCheckpoint = data.getCurrentCheckpoint().get();

            if (currentCheckpoint == refugeId) {
                MessageUtils.send(player, "§7Ce checkpoint est déjà activé!");
                return;
            }

            // Vérifier le niveau requis
            int playerLevel = data.getLevel().get();
            if (playerLevel < refuge.getRequiredLevel()) {
                MessageUtils.send(player, "§cNiveau insuffisant! §7(Requis: niveau " + refuge.getRequiredLevel() + ")");
                MessageUtils.playSoundError(player);
                return;
            }

            // Coût d'activation depuis la config du refuge
            long cost = refuge.getCost();

            if (data.hasPoints(cost)) {
                data.removePoints(cost);
                data.setCheckpoint(refugeId);

                MessageUtils.sendTitle(player, "§a§lCHECKPOINT", "§e" + refuge.getName() + " §7activé!", 10, 40, 10);
                MessageUtils.send(player, "§aCheckpoint §e" + refuge.getName() + " §aactivé! §7(-" +
                    com.rinaorc.zombiez.managers.EconomyManager.formatPoints(cost) + " Points)");
                MessageUtils.playSoundSuccess(player);
            } else {
                MessageUtils.send(player, "§cPoints insuffisants! §7(Requis: " +
                    com.rinaorc.zombiez.managers.EconomyManager.formatPoints(cost) + ")");
                MessageUtils.playSoundError(player);
            }
        } else {
            // Fallback: utiliser l'ancien système basé sur la zone
            var zone = plugin.getZoneManager().getZoneAt(block.getLocation());

            if (zone != null && zone.getRefugeId() > 0) {
                int refugeId = zone.getRefugeId();
                int currentCheckpoint = data.getCurrentCheckpoint().get();

                if (currentCheckpoint == refugeId) {
                    MessageUtils.send(player, "§7Ce checkpoint est déjà activé!");
                    return;
                }

                long cost = getCheckpointCost(refugeId);

                if (data.hasPoints(cost)) {
                    data.removePoints(cost);
                    data.setCheckpoint(refugeId);

                    MessageUtils.sendTitle(player, "§a§lCHECKPOINT", "§7Refuge " + refugeId + " activé!", 10, 40, 10);
                    MessageUtils.send(player, "§aCheckpoint activé! §7(-" +
                        com.rinaorc.zombiez.managers.EconomyManager.formatPoints(cost) + " Points)");
                    MessageUtils.playSoundSuccess(player);
                } else {
                    MessageUtils.send(player, "§cPoints insuffisants! §7(Requis: " +
                        com.rinaorc.zombiez.managers.EconomyManager.formatPoints(cost) + ")");
                    MessageUtils.playSoundError(player);
                }
            }
        }
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
