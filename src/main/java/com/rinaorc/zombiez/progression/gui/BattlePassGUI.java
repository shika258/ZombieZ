package com.rinaorc.zombiez.progression.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.BattlePassManager;
import com.rinaorc.zombiez.progression.BattlePassManager.*;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI du Battle Pass
 */
public class BattlePassGUI {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final BattlePassManager battlePassManager;
    
    private Inventory inventory;
    private int currentPage = 0;
    private final int levelsPerPage = 9;
    
    // Mapping slot -> niveau
    private final Map<Integer, Integer> slotToLevel;

    public BattlePassGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.battlePassManager = plugin.getBattlePassManager();
        this.slotToLevel = new HashMap<>();
    }

    /**
     * Ouvre le GUI
     */
    public void open() {
        createInventory();
        player.openInventory(inventory);
    }

    /**
     * Crée l'inventaire
     */
    private void createInventory() {
        Season season = battlePassManager.getCurrentSeason();
        PlayerBattlePass pass = battlePassManager.getPlayerPass(player.getUniqueId());
        
        String title = "§6Battle Pass §7- §eSaison " + season.getId();
        inventory = Bukkit.createInventory(null, 54, title);
        slotToLevel.clear();
        
        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Info saison
        inventory.setItem(4, new ItemBuilder(season.getIcon())
            .name(season.getTheme() + "§lSaison " + season.getId() + ": " + season.getName())
            .lore(
                "§7" + season.getDescription(),
                "",
                "§7Votre niveau: §e" + pass.getLevel(),
                "§7XP: §b" + pass.getCurrentLevelXp() + "§7/" + pass.getXpForNextLevel(),
                createProgressBar(pass.getProgressPercent()),
                "",
                "§7Jours restants: §e" + battlePassManager.getDaysRemaining()
            )
            .build());
        
        // Afficher les niveaux
        int startLevel = currentPage * levelsPerPage + 1;
        int endLevel = Math.min(startLevel + levelsPerPage - 1, 100);
        
        for (int i = 0; i < levelsPerPage; i++) {
            int level = startLevel + i;
            if (level > 100) break;
            
            BattlePassLevel bpLevel = season.getLevels().get(level - 1);
            
            // Récompense gratuite (ligne du haut)
            int freeSlot = 10 + i;
            if (bpLevel.getFreeReward() != null) {
                inventory.setItem(freeSlot, createRewardItem(bpLevel.getFreeReward(), level, pass, false));
                slotToLevel.put(freeSlot, level * 10); // *10 = gratuit
            }
            
            // Numéro du niveau (ligne du milieu)
            int levelSlot = 19 + i;
            inventory.setItem(levelSlot, createLevelMarker(level, pass));
            
            // Récompense premium (ligne du bas)
            int premiumSlot = 28 + i;
            if (bpLevel.getPremiumReward() != null) {
                inventory.setItem(premiumSlot, createRewardItem(bpLevel.getPremiumReward(), level, pass, true));
                slotToLevel.put(premiumSlot, level * 10 + 1); // *10+1 = premium
            }
        }
        
        // Labels
        inventory.setItem(9, new ItemBuilder(Material.LIME_DYE)
            .name("§aRécompenses Gratuites")
            .lore("§7Disponibles pour tous")
            .build());
        
        inventory.setItem(27, new ItemBuilder(Material.PURPLE_DYE)
            .name("§dRécompenses Premium")
            .lore(
                "§7Nécessite le Pass Premium",
                "",
                pass.isPremium() ? "§a✓ Vous avez le Premium!" : "§cNon activé"
            )
            .build());
        
        // Navigation
        if (currentPage > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .name("§a◄ Page Précédente")
                .build());
        }
        
        int maxPages = (int) Math.ceil(100.0 / levelsPerPage);
        if (currentPage < maxPages - 1) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                .name("§a► Page Suivante")
                .build());
        }
        
        // Acheter Premium
        if (!pass.isPremium()) {
            inventory.setItem(49, new ItemBuilder(Material.DIAMOND)
                .name("§d§lAcheter Pass Premium")
                .lore(
                    "§7Débloquez toutes les",
                    "§7récompenses premium!",
                    "",
                    "§7Prix: §d1000 Gemmes",
                    "",
                    "§eCliquez pour acheter!"
                )
                .glow(true)
                .build());
        } else {
            inventory.setItem(49, new ItemBuilder(Material.NETHER_STAR)
                .name("§d§l★ Pass Premium ★")
                .lore("§a✓ Activé!")
                .glow(true)
                .build());
        }
        
        // Page info
        inventory.setItem(50, new ItemBuilder(Material.PAPER)
            .name("§ePage " + (currentPage + 1) + "/" + maxPages)
            .lore("§7Niveaux " + startLevel + "-" + endLevel)
            .build());
    }

    /**
     * Crée l'item d'une récompense
     */
    private ItemStack createRewardItem(BattlePassReward reward, int level, PlayerBattlePass pass, boolean premium) {
        boolean unlocked = pass.getLevel() >= level;
        boolean claimed = premium ? pass.hasClaimedPremium(level) : pass.hasClaimedFree(level);
        boolean canClaim = unlocked && !claimed && (!premium || pass.isPremium());
        
        Material mat;
        if (claimed) {
            mat = Material.GRAY_DYE;
        } else if (canClaim) {
            mat = reward.getIcon();
        } else {
            mat = Material.BARRIER;
        }
        
        ItemBuilder builder = new ItemBuilder(mat);
        
        String prefix = premium ? "§d[Premium] " : "§a[Gratuit] ";
        String name = claimed ? "§8" + reward.getName() : 
                     canClaim ? prefix + reward.getName() :
                     "§c✗ " + reward.getName();
        
        builder.name(name);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Niveau " + level);
        
        if (!reward.getDescription().isEmpty()) {
            lore.add("");
            lore.add("§7" + reward.getDescription());
        }
        
        lore.add("");
        if (claimed) {
            lore.add("§8✓ Déjà réclamée");
        } else if (canClaim) {
            lore.add("§aCliquez pour réclamer!");
        } else if (!unlocked) {
            lore.add("§cNiveau " + level + " requis");
        } else if (premium && !pass.isPremium()) {
            lore.add("§cPass Premium requis");
        }
        
        builder.lore(lore);
        
        if (canClaim) {
            builder.glow(true);
        }
        
        return builder.build();
    }

    /**
     * Crée le marqueur de niveau
     */
    private ItemStack createLevelMarker(int level, PlayerBattlePass pass) {
        boolean reached = pass.getLevel() >= level;
        boolean current = pass.getLevel() == level;
        
        Material mat = current ? Material.PLAYER_HEAD :
                      reached ? Material.LIME_STAINED_GLASS_PANE :
                      Material.RED_STAINED_GLASS_PANE;
        
        String color = current ? "§e" : reached ? "§a" : "§c";
        
        return new ItemBuilder(mat)
            .name(color + "Niveau " + level)
            .lore(
                current ? "§e← Niveau actuel" :
                reached ? "§a✓ Atteint" :
                "§c✗ Non atteint"
            )
            .build();
    }

    /**
     * Gère un clic
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        PlayerBattlePass pass = battlePassManager.getPlayerPass(player.getUniqueId());
        
        // Navigation
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }
        
        if (slot == 53) {
            currentPage++;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }
        
        // Acheter Premium
        if (slot == 49 && !pass.isPremium()) {
            if (battlePassManager.purchasePremium(player)) {
                createInventory();
                player.openInventory(inventory);
            }
            return;
        }
        
        // Réclamer récompense
        Integer levelCode = slotToLevel.get(slot);
        if (levelCode != null) {
            int level = levelCode / 10;
            boolean premium = levelCode % 10 == 1;
            
            if (battlePassManager.claimReward(player, pass, level, premium)) {
                createInventory();
                player.openInventory(inventory);
            }
        }
    }

    private String createProgressBar(double percent) {
        int filled = (int) (percent / 5);
        String bar = "§a" + "█".repeat(filled) + "§7" + "░".repeat(20 - filled);
        return bar + " §e" + String.format("%.1f", percent) + "%";
    }

    /**
     * Listener pour les événements de GUI
     */
    public static class BattlePassGUIListener implements Listener {
        
        private final ZombieZPlugin plugin;
        
        public BattlePassGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }
        
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            
            String title = event.getView().getTitle();
            if (!title.contains("Battle Pass")) return;
            
            BattlePassGUI gui = new BattlePassGUI(plugin, player);
            gui.inventory = event.getInventory();
            gui.handleClick(event);
        }
    }
}
