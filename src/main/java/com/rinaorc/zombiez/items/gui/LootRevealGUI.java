package com.rinaorc.zombiez.items.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI animé pour afficher le loot obtenu (crate, boss, etc.)
 */
public class LootRevealGUI implements InventoryHolder {

    private static final int SIZE = 27;
    private static final int[] ITEM_SLOTS = {10, 12, 14, 16};
    private static final int CENTER_SLOT = 13;
    
    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<ZombieZItem> items;
    private final String source;
    
    private boolean revealed = false;
    private int animationPhase = 0;

    public LootRevealGUI(ZombieZPlugin plugin, Player player, List<ZombieZItem> items, String source) {
        this.plugin = plugin;
        this.player = player;
        this.items = items;
        this.source = source;
        
        String title = "§8" + source + " - Loot";
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        
        setupInitialGUI();
    }

    private void setupInitialGUI() {
        // Fond sombre
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        
        // Items mystère avant reveal
        ItemStack mystery = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
            .name("§5§l???")
            .lore("§7Cliquez pour révéler!")
            .glow()
            .build();
        
        for (int slot : ITEM_SLOTS) {
            if (slot - 10 < items.size() * 2) { // Si on a un item pour ce slot
                inventory.setItem(slot, mystery);
            }
        }
        
        // Bouton reveal au centre
        inventory.setItem(CENTER_SLOT, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§l✦ RÉVÉLER LE LOOT ✦")
            .lore(
                "",
                "§7" + items.size() + " item(s) à découvrir!",
                "",
                "§e▶ Cliquez pour révéler"
            )
            .glow()
            .build());
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    /**
     * Lance l'animation de révélation
     */
    private void startRevealAnimation() {
        if (revealed) return;
        revealed = true;
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
        
        // Animation progressive
        new BukkitRunnable() {
            int index = 0;
            int tick = 0;
            
            @Override
            public void run() {
                tick++;
                
                // Animation de roulette
                if (tick % 2 == 0 && tick < 20) {
                    animateRoulette();
                }
                
                // Révéler les items un par un
                if (tick >= 20 && tick % 10 == 0 && index < items.size()) {
                    revealItem(index);
                    index++;
                }
                
                // Fin de l'animation
                if (index >= items.size() && tick > 20 + items.size() * 10) {
                    finalReveal();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void animateRoulette() {
        Material[] materials = {
            Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE
        };
        
        animationPhase = (animationPhase + 1) % materials.length;
        
        for (int slot : ITEM_SLOTS) {
            if (inventory.getItem(slot) != null && 
                inventory.getItem(slot).getType().name().contains("STAINED_GLASS_PANE")) {
                inventory.setItem(slot, ItemBuilder.placeholder(materials[animationPhase]));
            }
        }
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f + animationPhase * 0.1f);
    }

    private void revealItem(int index) {
        if (index >= items.size()) return;
        
        ZombieZItem item = items.get(index);
        int slot = ITEM_SLOTS[index % ITEM_SLOTS.length];
        
        // Mettre l'item réel
        inventory.setItem(slot, item.toItemStack());
        
        // Effet sonore selon rareté
        Sound sound = switch (item.getRarity()) {
            case EXALTED, MYTHIC -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case LEGENDARY -> Sound.ENTITY_PLAYER_LEVELUP;
            case EPIC -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> Sound.BLOCK_NOTE_BLOCK_PLING;
        };
        
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    private void finalReveal() {
        // Calculer les stats totales du loot
        int totalScore = items.stream().mapToInt(ZombieZItem::getItemScore).sum();
        Rarity bestRarity = items.stream()
            .map(ZombieZItem::getRarity)
            .max(Comparator.comparing(Rarity::ordinal))
            .orElse(Rarity.COMMON);
        
        // Mettre à jour le centre
        inventory.setItem(CENTER_SLOT, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§l✦ LOOT RÉVÉLÉ! ✦")
            .lore(
                "",
                "§7Items: §e" + items.size(),
                "§7Score total: §6" + totalScore,
                "§7Meilleure rareté: " + bestRarity.getColoredName(),
                "",
                "§a▶ Cliquez pour récupérer"
            )
            .glow()
            .build());
        
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
    }

    /**
     * Donne tous les items au joueur
     */
    private void collectAllItems() {
        for (ZombieZItem item : items) {
            plugin.getItemManager().giveItem(player, item);
        }
        
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Listener pour les clics
     */
    public static class LootGUIListener implements Listener {
        
        private final ZombieZPlugin plugin;
        
        public LootGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }
        
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof LootRevealGUI gui)) {
                return;
            }
            
            event.setCancelled(true);
            
            int slot = event.getRawSlot();
            
            if (slot == CENTER_SLOT) {
                if (!gui.revealed) {
                    gui.startRevealAnimation();
                } else {
                    gui.collectAllItems();
                }
            }
        }
        
        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof LootRevealGUI gui)) {
                return;
            }
            
            // Si fermé avant collection, donner quand même les items
            if (gui.revealed) {
                for (ZombieZItem item : gui.items) {
                    plugin.getItemManager().giveItem((Player) event.getPlayer(), item);
                }
            }
        }
    }
}
