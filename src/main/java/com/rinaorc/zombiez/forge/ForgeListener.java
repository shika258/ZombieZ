package com.rinaorc.zombiez.forge;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener pour les événements de la GUI de Forge
 */
public class ForgeListener implements Listener {

    private final ZombieZPlugin plugin;

    public ForgeListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeGUI gui)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        // Si clic dans l'inventaire du joueur
        if (slot >= 54) {
            // Permettre de prendre un item pour le placer dans la forge
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && plugin.getForgeManager().canBeForged(clickedItem)) {
                // Si shift-clic, placer directement dans la forge
                if (event.isShiftClick()) {
                    event.setCancelled(true);

                    // Si un item est déjà dans la forge, le rendre d'abord
                    if (gui.getItemToForge() != null) {
                        player.getInventory().addItem(gui.retrieveItem());
                    }

                    // Placer le nouvel item
                    gui.setItem(clickedItem.clone());
                    clickedItem.setAmount(0);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
                }
            }
            return;
        }

        // Clic dans la GUI - toujours annuler par défaut
        event.setCancelled(true);

        // Slot de l'item à forger
        if (slot == ForgeGUI.getSlotItem()) {
            ItemStack cursorItem = event.getCursor();
            ItemStack forgeItem = gui.getItemToForge();

            if (cursorItem != null && !cursorItem.getType().isAir()) {
                // Placer un item
                if (plugin.getForgeManager().canBeForged(cursorItem)) {
                    // Échanger si nécessaire
                    if (forgeItem != null) {
                        event.getWhoClicked().setItemOnCursor(forgeItem);
                    } else {
                        event.getWhoClicked().setItemOnCursor(null);
                    }
                    gui.setItem(cursorItem);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
                } else {
                    player.sendMessage("§c[Forge] §7Cet item ne peut pas être forgé!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            } else if (forgeItem != null) {
                // Récupérer l'item
                event.getWhoClicked().setItemOnCursor(gui.retrieveItem());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
            }
            return;
        }

        // Bouton Pierre de Protection
        if (slot == ForgeGUI.getSlotProtection()) {
            gui.toggleProtection();
            return;
        }

        // Bouton Pierre Bénie
        if (slot == ForgeGUI.getSlotBlessed()) {
            gui.toggleBlessed();
            return;
        }

        // Bouton Pierre de Chance
        if (slot == ForgeGUI.getSlotChance()) {
            gui.toggleChance();
            return;
        }

        // Bouton Forger
        if (slot == ForgeGUI.getSlotForge()) {
            gui.attemptForge();
            return;
        }

        // Bouton Fermer
        if (slot == ForgeGUI.getSlotClose()) {
            // Rendre l'item s'il y en a un
            if (gui.getItemToForge() != null) {
                player.getInventory().addItem(gui.retrieveItem());
            }
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ForgeGUI) {
            // Annuler le drag dans la GUI (sauf inventaire joueur)
            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeGUI gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // Rendre l'item s'il y en a un et nettoyer l'état
        ItemStack item = gui.retrieveItem();
        if (item != null) {
            player.getInventory().addItem(item);
        }
    }
}
