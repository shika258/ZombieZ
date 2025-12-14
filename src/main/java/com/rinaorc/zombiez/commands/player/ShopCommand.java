package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.economy.shops.Shop;
import com.rinaorc.zombiez.economy.shops.ShopManager;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /shop pour accéder aux magasins
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ShopCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande nécessite d'être un joueur.");
            return true;
        }

        ShopManager shopManager = plugin.getShopManager();
        if (shopManager == null) {
            player.sendMessage("§cLe système de magasins n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            // Ouvrir le shop de la zone actuelle
            Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
            int zoneId = zone != null ? zone.getId() : 0;
            
            Shop zoneShop = shopManager.getZoneShop(zoneId);
            if (zoneShop != null) {
                shopManager.openShop(player, zoneShop);
            } else {
                shopManager.openShop(player, "general");
            }
            return true;
        }

        String shopId = args[0].toLowerCase();
        
        switch (shopId) {
            case "list" -> {
                sendShopList(player);
            }
            case "general", "weapons", "armor", "consumables", "upgrades", "black_market" -> {
                shopManager.openShop(player, shopId);
            }
            case "zone" -> {
                if (args.length > 1) {
                    try {
                        int zoneId = Integer.parseInt(args[1]);
                        Shop zoneShop = shopManager.getZoneShop(zoneId);
                        if (zoneShop != null) {
                            shopManager.openShop(player, zoneShop);
                        } else {
                            player.sendMessage("§cShop de zone " + zoneId + " introuvable.");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cNuméro de zone invalide.");
                    }
                } else {
                    Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
                    int zoneId = zone != null ? zone.getId() : 0;
                    Shop zoneShop = shopManager.getZoneShop(zoneId);
                    if (zoneShop != null) {
                        shopManager.openShop(player, zoneShop);
                    }
                }
            }
            default -> {
                // Essayer d'ouvrir directement
                Shop shop = shopManager.getShop(shopId);
                if (shop != null) {
                    shopManager.openShop(player, shop);
                } else {
                    player.sendMessage("§cShop introuvable: " + shopId);
                    sendShopList(player);
                }
            }
        }

        return true;
    }

    private void sendShopList(Player player) {
        player.sendMessage("§6=== Magasins Disponibles ===");
        player.sendMessage("§e/shop §7- Magasin de zone actuelle");
        player.sendMessage("§e/shop general §7- Magasin général");
        player.sendMessage("§e/shop weapons §7- Armurerie (Armes)");
        player.sendMessage("§e/shop armor §7- Armurerie (Armures)");
        player.sendMessage("§e/shop consumables §7- Alchimiste");
        player.sendMessage("§e/shop upgrades §7- Forge Mystique");
        player.sendMessage("§e/shop black_market §7- Marché Noir");
        player.sendMessage("§e/shop zone <num> §7- Marchand de zone");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("list", "general", "weapons", "armor", "consumables", "upgrades", "black_market", "zone"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("zone")) {
            for (int i = 0; i <= 11; i++) {
                completions.add(String.valueOf(i));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
