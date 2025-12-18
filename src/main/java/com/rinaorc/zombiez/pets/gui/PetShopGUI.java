package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.gacha.PetShopSystem;
import com.rinaorc.zombiez.pets.gacha.PetShopSystem.*;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI de la boutique de Pets
 * Offres, bundles, deals limités
 */
public class PetShopGUI implements InventoryHolder {

    private static final String TITLE = "§0\u2800\u2800\u2800\u2800\u2800\u2800\u2800💎 Boutique Pet";
    private static final int SIZE = 54;

    // Layout réorganisé avec sections colorées et bien centrées
    private static final int SLOT_BALANCE = 4;                              // Solde centré en haut
    private static final int[] HOT_DEALS_SLOTS = {11, 12, 13};              // Ligne 1 : Offres flash (centrées)
    private static final int SLOT_DAILY = 16;                               // Récompense quotidienne (à droite)
    private static final int[] EGGS_SLOTS = {19, 20, 21, 22, 23, 24, 25};   // Ligne 2 : Oeufs (7 slots)
    private static final int[] FRAGMENTS_SLOTS = {29, 30, 31};              // Ligne 3 : Fragments (centrés)
    private static final int[] FIRST_PURCHASE_SLOTS = {39, 40, 41};         // Ligne 4 : Packs exclusifs (centrés)
    private static final int SLOT_BACK = 49;                                // Retour centré en bas

    // Séparateur visuel
    private static final String LORE_SEPARATOR = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PetShopSystem shopSystem;
    private final PlayerPetData petData;

    public PetShopGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.shopSystem = plugin.getPetShopSystem();
        this.petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        setupGUI();
    }

    private void setupGUI() {
        // === LIGNE 0 : HEADER VIOLET ===
        ItemStack headerGlass = ItemBuilder.placeholder(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, headerGlass);
        }

        // Solde du joueur centré
        int fragments = petData != null ? petData.getFragments() : 0;
        long points = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            points = playerData.getPoints().get();
        }
        inventory.setItem(SLOT_BALANCE, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§l💰 Votre Solde")
            .lore(List.of(
                "",
                "§e► §7Points: §a" + String.format("%,d", points),
                "§d► §7Fragments: §d" + String.format("%,d", fragments),
                "",
                LORE_SEPARATOR,
                "",
                "§8Points = acheter oeufs/fragments",
                "§8Fragments = acheter dans la boutique"
            ))
            .glow(true)
            .build());

        // === LIGNE 1 : OFFRES FLASH (fond orange) ===
        ItemStack orangeGlass = ItemBuilder.placeholder(Material.ORANGE_STAINED_GLASS_PANE);
        for (int i = 9; i < 18; i++) {
            inventory.setItem(i, orangeGlass);
        }

        // Label offres flash
        inventory.setItem(10, new ItemBuilder(Material.FIRE_CHARGE)
            .name("§c§l🔥 OFFRES FLASH")
            .lore(List.of(
                "",
                "§7Offres limitées!",
                "§7Changent toutes les §e8h"
            ))
            .build());

        // Offres flash centrées
        List<TimedOffer> timedOffers = shopSystem.getTimedOffers();
        for (int i = 0; i < timedOffers.size() && i < HOT_DEALS_SLOTS.length; i++) {
            inventory.setItem(HOT_DEALS_SLOTS[i], createTimedOfferItem(timedOffers.get(i)));
        }

        // Récompense quotidienne (à droite)
        boolean canClaimDaily = plugin.getDailyRewardManager() != null &&
            plugin.getDailyRewardManager().canClaim(player);
        int streak = getStreak();
        inventory.setItem(SLOT_DAILY, new ItemBuilder(canClaimDaily ? Material.CHEST : Material.ENDER_CHEST)
            .name(canClaimDaily ? "§a§l🎁 RÉCOMPENSE!" : "§8🎁 Quotidienne")
            .lore(canClaimDaily ?
                List.of(
                    "",
                    "§aVotre récompense est prête!",
                    "",
                    "§7Streak: §e" + streak + " jour" + (streak > 1 ? "s" : ""),
                    "",
                    "§a§l► Cliquez pour réclamer!"
                ) :
                List.of(
                    "",
                    "§8Déjà réclamée aujourd'hui",
                    "",
                    "§7Streak: §e" + streak + " jour" + (streak > 1 ? "s" : ""),
                    "",
                    "§7Revenez demain!"
                ))
            .glow(canClaimDaily)
            .build());

        // === LIGNE 2 : OEUFS (fond lime) ===
        ItemStack limeGlass = ItemBuilder.placeholder(Material.LIME_STAINED_GLASS_PANE);
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, limeGlass);
        }

        // Label oeufs
        inventory.setItem(18, new ItemBuilder(Material.TURTLE_EGG)
            .name("§a§l🥚 OEUFS")
            .lore(List.of(
                "",
                "§7Achetez des oeufs",
                "§7pour des compagnons!",
                "",
                "§eChaque oeuf = 1 pet aléatoire"
            ))
            .build());

        List<ShopOffer> eggOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() != null)
            .toList();
        for (int i = 0; i < eggOffers.size() && i < EGGS_SLOTS.length; i++) {
            inventory.setItem(EGGS_SLOTS[i], createEggOfferItem(eggOffers.get(i)));
        }

        // === LIGNE 3 : FRAGMENTS (fond cyan) ===
        ItemStack cyanGlass = ItemBuilder.placeholder(Material.CYAN_STAINED_GLASS_PANE);
        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, cyanGlass);
        }

        // Label fragments
        inventory.setItem(28, new ItemBuilder(Material.PRISMARINE_SHARD)
            .name("§b§l💎 FRAGMENTS")
            .lore(List.of(
                "",
                "§7Convertissez vos points",
                "§7de jeu en fragments!",
                "",
                "§dUtilisez les fragments pour",
                "§dacheter des packs exclusifs"
            ))
            .build());

        List<ShopOffer> fragmentOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() == null && o.fragments() > 0)
            .toList();
        for (int i = 0; i < fragmentOffers.size() && i < FRAGMENTS_SLOTS.length; i++) {
            inventory.setItem(FRAGMENTS_SLOTS[i], createFragmentOfferItem(fragmentOffers.get(i)));
        }

        // === LIGNE 4 : PACKS EXCLUSIFS (fond magenta) ===
        ItemStack magentaGlass = ItemBuilder.placeholder(Material.MAGENTA_STAINED_GLASS_PANE);
        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, magentaGlass);
        }

        // Label packs exclusifs
        inventory.setItem(38, new ItemBuilder(Material.NETHER_STAR)
            .name("§d§l⭐ PACKS EXCLUSIFS")
            .lore(List.of(
                "",
                "§7Offres uniques et avantageuses!",
                "",
                "§e§lUNE SEULE FOIS",
                "§eper compte"
            ))
            .glow(true)
            .build());

        List<FirstPurchaseOffer> firstOffers = shopSystem.getFirstPurchaseOffers();
        for (int i = 0; i < firstOffers.size() && i < FIRST_PURCHASE_SLOTS.length; i++) {
            inventory.setItem(FIRST_PURCHASE_SLOTS[i], createFirstPurchaseItem(firstOffers.get(i)));
        }

        // === LIGNE 5 : FOOTER GRIS ===
        ItemStack footerGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, footerGlass);
        }

        // Bouton retour centré
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c§l◄ Retour")
            .lore(List.of("", "§7Retourner au menu principal"))
            .build());
    }

    private int getStreak() {
        if (plugin.getDailyRewardManager() == null) return 0;
        return plugin.getDailyRewardManager().getStreak(player);
    }

    private ItemStack createTimedOfferItem(TimedOffer offer) {
        Duration remaining = shopSystem.getTimeRemaining(offer.id());
        String timeStr = formatDuration(remaining);
        int playerFragments = petData != null ? petData.getFragments() : 0;
        boolean canAfford = playerFragments >= offer.price();

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }

        lore.add("");
        lore.add(LORE_SEPARATOR);
        lore.add("");

        // Prix avec format homogène
        if (offer.discountPercent() > 0) {
            int originalPrice = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§7Prix: §c§m" + String.format("%,d", originalPrice) + "§r §a" + String.format("%,d", offer.price()) + " §dfragments");
            lore.add("§a§l-" + offer.discountPercent() + "% §ade réduction!");
        } else {
            lore.add("§7Prix: §f" + String.format("%,d", offer.price()) + " §dfragments");
        }

        lore.add("");
        lore.add("§c⏱ Expire dans: §f" + timeStr);
        lore.add("");

        // Statut d'achat
        if (canAfford) {
            lore.add("§a§l► Cliquez pour acheter!");
        } else {
            lore.add("§c✗ Fragments insuffisants");
            lore.add("§7  Manque: §c" + String.format("%,d", offer.price() - playerFragments));
        }

        Material icon = offer.eggType() != null ? offer.eggType().getIcon() : Material.CHEST;

        return new ItemBuilder(icon)
            .name("§c§l🔥 " + offer.name())
            .lore(lore)
            .glow(true)
            .build();
    }

    private ItemStack createFirstPurchaseItem(FirstPurchaseOffer offer) {
        boolean alreadyBought = shopSystem.hasUsedFirstPurchase(player.getUniqueId(), offer.id());
        int playerFragments = petData != null ? petData.getFragments() : 0;
        boolean canAfford = playerFragments >= offer.price();

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }

        lore.add("");
        lore.add("§e§lContenu du pack:");
        for (RewardItem item : offer.rewards()) {
            if (item.eggType() != null) {
                lore.add("§a  • §f" + item.amount() + "x " + item.eggType().getColoredName());
            } else {
                lore.add("§a  • §f" + String.format("%,d", item.amount()) + " §dfragments");
            }
        }

        lore.add("");
        lore.add(LORE_SEPARATOR);
        lore.add("");

        // Prix avec format homogène
        if (offer.discountPercent() > 0) {
            int originalPrice = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§7Prix: §c§m" + String.format("%,d", originalPrice) + "§r §a" + String.format("%,d", offer.price()) + " §dfragments");
            lore.add("§a§l-" + offer.discountPercent() + "% §ade réduction!");
        } else {
            lore.add("§7Prix: §f" + String.format("%,d", offer.price()) + " §dfragments");
        }

        lore.add("");

        // Statut d'achat
        if (alreadyBought) {
            lore.add("§8§l✗ DÉJÀ ACHETÉ");
        } else if (canAfford) {
            lore.add("§a§l► Cliquez pour acheter!");
        } else {
            lore.add("§c✗ Fragments insuffisants");
            lore.add("§7  Manque: §c" + String.format("%,d", offer.price() - playerFragments));
        }

        return new ItemBuilder(alreadyBought ? Material.GRAY_DYE : Material.DIAMOND)
            .name(alreadyBought ? "§8" + offer.name() : "§b§l⭐ " + offer.name())
            .lore(lore)
            .glow(!alreadyBought && canAfford)
            .build();
    }

    private ItemStack createEggOfferItem(ShopOffer offer) {
        long playerPoints = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            playerPoints = playerData.getPoints().get();
        }
        boolean canAfford = playerPoints >= offer.price();

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }

        // Info sur l'oeuf
        if (offer.eggType() != null) {
            lore.add("");
            lore.add("§e§lRaretés possibles:");
            lore.add("§7" + offer.eggType().getRarityInfo());
        }

        lore.add("");
        lore.add(LORE_SEPARATOR);
        lore.add("");

        // Prix avec format homogène (en POINTS pour les oeufs)
        if (offer.discountPercent() > 0) {
            int originalPrice = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§7Prix: §c§m" + String.format("%,d", originalPrice) + "§r §a" + String.format("%,d", offer.price()) + " §epoints");
            lore.add("§a§l-" + offer.discountPercent() + "% §ade réduction!");
        } else {
            lore.add("§7Prix: §f" + String.format("%,d", offer.price()) + " §epoints");
        }

        lore.add("");

        // Statut d'achat
        if (canAfford) {
            lore.add("§a§l► Cliquez pour acheter!");
        } else {
            lore.add("§c✗ Points insuffisants");
            lore.add("§7  Manque: §c" + String.format("%,d", offer.price() - playerPoints));
        }

        Material icon = offer.eggType() != null ? offer.eggType().getIcon() : Material.EGG;

        return new ItemBuilder(icon)
            .name(offer.name())
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private ItemStack createFragmentOfferItem(ShopOffer offer) {
        long playerPoints = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            playerPoints = playerData.getPoints().get();
        }
        boolean canAfford = playerPoints >= offer.price();

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }

        // Quantité de fragments
        lore.add("");
        lore.add("§d§lVous recevez:");
        lore.add("§f  ➤ §d" + String.format("%,d", offer.fragments()) + " fragments");

        // Bonus si applicable
        if (offer.discountPercent() > 0) {
            int bonus = offer.fragments() - (offer.fragments() * (100 - offer.discountPercent()) / 100);
            if (bonus > 0) {
                lore.add("§a  ➤ +" + String.format("%,d", bonus) + " bonus!");
            }
        }

        lore.add("");
        lore.add(LORE_SEPARATOR);
        lore.add("");

        // Prix avec format homogène (en POINTS)
        lore.add("§7Prix: §f" + String.format("%,d", offer.price()) + " §epoints");

        lore.add("");

        // Statut d'achat
        if (canAfford) {
            lore.add("§a§l► Cliquez pour acheter!");
        } else {
            lore.add("§c✗ Points insuffisants");
            lore.add("§7  Manque: §c" + String.format("%,d", offer.price() - playerPoints));
        }

        // Icône selon la quantité
        Material icon;
        if (offer.fragments() >= 2000) {
            icon = Material.DIAMOND_BLOCK;
        } else if (offer.fragments() >= 500) {
            icon = Material.GOLD_BLOCK;
        } else {
            icon = Material.PRISMARINE_SHARD;
        }

        return new ItemBuilder(icon)
            .name(offer.name())
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private String formatDuration(Duration duration) {
        if (duration.isNegative() || duration.isZero()) return "Expiré!";

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " minutes";
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Listener pour le shop
     */
    public static class ShopListener implements Listener {

        private final ZombieZPlugin plugin;

        public ShopListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof PetShopGUI gui)) return;
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Retour
            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            // Récompense quotidienne
            if (slot == SLOT_DAILY) {
                if (gui.plugin.getDailyRewardManager() != null &&
                    gui.plugin.getDailyRewardManager().canClaim(player)) {

                    boolean success = gui.plugin.getDailyRewardManager().claimDailyReward(player);
                    if (success) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    }
                    // Refresh le GUI
                    new PetShopGUI(gui.plugin, player).open();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }

            PetShopSystem shopSystem = gui.shopSystem;
            if (shopSystem == null) return;

            // Hot deals
            List<TimedOffer> timedOffers = shopSystem.getTimedOffers();
            for (int i = 0; i < timedOffers.size() && i < HOT_DEALS_SLOTS.length; i++) {
                if (slot == HOT_DEALS_SLOTS[i]) {
                    var result = shopSystem.buyTimedOffer(player.getUniqueId(), timedOffers.get(i).id());
                    handlePurchaseResult(player, result, gui);
                    return;
                }
            }

            // First purchase
            List<FirstPurchaseOffer> firstOffers = shopSystem.getFirstPurchaseOffers();
            for (int i = 0; i < firstOffers.size() && i < FIRST_PURCHASE_SLOTS.length; i++) {
                if (slot == FIRST_PURCHASE_SLOTS[i]) {
                    var result = shopSystem.buyFirstPurchaseOffer(player.getUniqueId(), firstOffers.get(i).id());
                    handlePurchaseResult(player, result, gui);
                    return;
                }
            }

            // Oeufs permanents
            List<ShopOffer> eggOffers = shopSystem.getPermanentOffers().stream()
                .filter(o -> o.eggType() != null)
                .toList();
            for (int i = 0; i < eggOffers.size() && i < EGGS_SLOTS.length; i++) {
                if (slot == EGGS_SLOTS[i]) {
                    var result = shopSystem.buyPermanentOffer(player.getUniqueId(), eggOffers.get(i).id());
                    handlePurchaseResult(player, result, gui);
                    return;
                }
            }

            // Fragments permanents
            List<ShopOffer> fragmentOffers = shopSystem.getPermanentOffers().stream()
                .filter(o -> o.eggType() == null && o.fragments() > 0)
                .toList();
            for (int i = 0; i < fragmentOffers.size() && i < FRAGMENTS_SLOTS.length; i++) {
                if (slot == FRAGMENTS_SLOTS[i]) {
                    var result = shopSystem.buyPermanentOffer(player.getUniqueId(), fragmentOffers.get(i).id());
                    handlePurchaseResult(player, result, gui);
                    return;
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof PetShopGUI) {
                event.setCancelled(true);
            }
        }

        private void handlePurchaseResult(Player player, PurchaseResult result, PetShopGUI gui) {
            if (result.success()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                player.sendMessage("");
                player.sendMessage("§a§l✓ ACHAT RÉUSSI!");
                player.sendMessage(result.message());
                // Refresh le GUI
                new PetShopGUI(gui.plugin, player).open();
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§c" + result.message());
            }
        }
    }
}
