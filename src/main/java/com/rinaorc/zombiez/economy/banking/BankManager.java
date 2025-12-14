package com.rinaorc.zombiez.economy.banking;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de banque personnelle
 * Stockage d'items sécurisé + intérêts sur les points
 */
public class BankManager implements Listener {

    private final ZombieZPlugin plugin;
    
    // Coffres de banque par joueur (UUID -> Liste d'items)
    private final Map<UUID, List<ItemStack>> bankVaults;
    
    // Configuration
    private static final int BASE_VAULT_SIZE = 27; // 27 slots de base
    private static final int MAX_VAULT_SIZE = 54; // Max 54 slots
    private static final int VAULT_UPGRADE_COST = 5000; // Coût par upgrade
    private static final double DAILY_INTEREST_RATE = 0.001; // 0.1% par jour
    private static final int MAX_INTEREST_POINTS = 100000; // Max 100K de points pour les intérêts

    public BankManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.bankVaults = new ConcurrentHashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startInterestTask();
    }

    /**
     * Ouvre le coffre de banque d'un joueur
     */
    public void openVault(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cErreur: données non chargées.");
            return;
        }

        int vaultSize = getVaultSize(player.getUniqueId());
        String title = "§6Coffre de Banque §7[" + vaultSize + " slots]";
        Inventory inv = Bukkit.createInventory(null, vaultSize, title);

        // Charger les items
        List<ItemStack> items = bankVaults.getOrDefault(player.getUniqueId(), new ArrayList<>());
        for (int i = 0; i < Math.min(items.size(), vaultSize); i++) {
            inv.setItem(i, items.get(i));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }

    /**
     * Ouvre l'interface principale de la banque
     */
    public void openBankMenu(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lBanque ZombieZ");

        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Coffre personnel
        int vaultSize = getVaultSize(player.getUniqueId());
        int usedSlots = bankVaults.getOrDefault(player.getUniqueId(), new ArrayList<>()).size();
        inv.setItem(11, new ItemBuilder(Material.CHEST)
            .name("§6Coffre Personnel")
            .lore(
                "§7Stockez vos items précieux",
                "§7en toute sécurité!",
                "",
                "§7Capacité: §e" + usedSlots + "§7/§e" + vaultSize + " §7slots",
                "",
                "§aClic pour ouvrir"
            )
            .build());

        // Dépôt de points
        inv.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
            .name("§eGestion des Points")
            .lore(
                "§7Solde actuel: §6" + data.getPoints().get() + " pts",
                "§7Points en banque: §6" + data.getBankPoints() + " pts",
                "",
                "§7Intérêt journalier: §a+" + String.format("%.1f", DAILY_INTEREST_RATE * 100) + "%",
                "§7(Max " + MAX_INTEREST_POINTS + " pts)",
                "",
                "§aClic gauche §7= Déposer 1000",
                "§cClic droit §7= Retirer 1000",
                "§eShift §7= x10"
            )
            .build());

        // Upgrade du coffre
        int upgradeSlots = vaultSize + 9;
        boolean canUpgrade = upgradeSlots <= MAX_VAULT_SIZE;
        inv.setItem(15, new ItemBuilder(canUpgrade ? Material.ANVIL : Material.BARRIER)
            .name(canUpgrade ? "§dAgrandir le Coffre" : "§cCoffre Maximum")
            .lore(
                canUpgrade ? List.of(
                    "§7Ajouter 9 slots",
                    "§7" + vaultSize + " → " + upgradeSlots + " slots",
                    "",
                    "§7Coût: §6" + VAULT_UPGRADE_COST + " pts",
                    "",
                    "§aClic pour agrandir"
                ) : List.of(
                    "§7Votre coffre est à",
                    "§7sa capacité maximale!",
                    "",
                    "§7Capacité: §e" + vaultSize + " slots"
                )
            )
            .build());

        player.openInventory(inv);
    }

    /**
     * Dépose des points en banque
     */
    public boolean depositPoints(Player player, int amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null || data.getPoints().get() < amount) return false;

        data.removePoints(amount);
        data.addBankPoints(amount);
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    /**
     * Retire des points de la banque
     */
    public boolean withdrawPoints(Player player, int amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null || data.getBankPoints() < amount) return false;

        data.removeBankPoints(amount);
        data.addPoints(amount);
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
        return true;
    }

    /**
     * Agrandit le coffre
     */
    public boolean upgradeVault(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return false;

        int currentSize = getVaultSize(player.getUniqueId());
        if (currentSize >= MAX_VAULT_SIZE) return false;
        if (data.getPoints().get() < VAULT_UPGRADE_COST) return false;

        data.removePoints(VAULT_UPGRADE_COST);
        data.setVaultSize(currentSize + 9);
        
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        return true;
    }

    /**
     * Obtient la taille du coffre d'un joueur
     */
    public int getVaultSize(UUID playerId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(playerId);
        if (data == null) return BASE_VAULT_SIZE;
        return Math.max(BASE_VAULT_SIZE, data.getVaultSize());
    }

    /**
     * Sauvegarde le contenu du coffre
     */
    private void saveVault(UUID playerId, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            items.add(item != null ? item.clone() : null);
        }
        bankVaults.put(playerId, items);
    }

    /**
     * Démarre la tâche d'intérêts
     */
    private void startInterestTask() {
        // Toutes les 24000 ticks Minecraft (~20 minutes réel = 1 jour MC)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (data == null) continue;

                int bankPoints = data.getBankPoints();
                if (bankPoints > 0) {
                    int interestBase = Math.min(bankPoints, MAX_INTEREST_POINTS);
                    int interest = (int) (interestBase * DAILY_INTEREST_RATE);
                    
                    if (interest > 0) {
                        data.addBankPoints(interest);
                        player.sendMessage("§a[Banque] §7Intérêts reçus: §6+" + interest + " pts");
                    }
                }
            }
        }, 24000L, 24000L);
    }

    // ==================== LISTENERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Menu principal de la banque
        if (title.equals("§6§lBanque ZombieZ")) {
            event.setCancelled(true);
            
            switch (event.getRawSlot()) {
                case 11 -> { // Coffre
                    player.closeInventory();
                    openVault(player);
                }
                case 13 -> { // Points
                    int amount = event.isShiftClick() ? 10000 : 1000;
                    if (event.isLeftClick()) {
                        if (depositPoints(player, amount)) {
                            player.sendMessage("§a+" + amount + " pts déposés en banque");
                        } else {
                            player.sendMessage("§cPoints insuffisants!");
                        }
                    } else if (event.isRightClick()) {
                        if (withdrawPoints(player, amount)) {
                            player.sendMessage("§a+" + amount + " pts retirés de la banque");
                        } else {
                            player.sendMessage("§cSolde bancaire insuffisant!");
                        }
                    }
                    // Rafraîchir le menu
                    openBankMenu(player);
                }
                case 15 -> { // Upgrade
                    if (upgradeVault(player)) {
                        player.sendMessage("§aCoffre agrandi! Nouvelle capacité: " + getVaultSize(player.getUniqueId()) + " slots");
                        openBankMenu(player);
                    } else {
                        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                        if (data != null && data.getPoints().get() < VAULT_UPGRADE_COST) {
                            player.sendMessage("§cVous avez besoin de " + VAULT_UPGRADE_COST + " points!");
                        }
                    }
                }
            }
            return;
        }
        
        // Coffre de banque - permettre les interactions
        if (title.startsWith("§6Coffre de Banque")) {
            // On laisse le joueur interagir normalement
            // La sauvegarde se fait à la fermeture
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (title.startsWith("§6Coffre de Banque")) {
            // Sauvegarder le contenu
            saveVault(player.getUniqueId(), event.getInventory());
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1f);
        }
    }

    /**
     * Charge les données de coffre depuis la BDD
     */
    public void loadVault(UUID playerId, List<ItemStack> items) {
        if (items != null) {
            bankVaults.put(playerId, new ArrayList<>(items));
        }
    }

    /**
     * Obtient les items du coffre pour sauvegarde
     */
    public List<ItemStack> getVaultContents(UUID playerId) {
        return bankVaults.getOrDefault(playerId, new ArrayList<>());
    }
}
