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

    private static final String TITLE = "§8§l💎 Boutique Pet";
    private static final int SIZE = 54;

    // Sections du shop
    private static final int[] HOT_DEALS_SLOTS = {10, 11, 12};
    private static final int[] FIRST_PURCHASE_SLOTS = {14, 15, 16};
    private static final int[] EGGS_SLOTS = {28, 29, 30, 31, 32, 33, 34};
    private static final int[] FRAGMENTS_SLOTS = {37, 38, 39};
    private static final int SLOT_DAILY = 43;
    private static final int SLOT_BACK = 49;

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
        // Fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordures décoratives
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        // Titres de section
        inventory.setItem(1, new ItemBuilder(Material.FIRE_CHARGE)
            .name("§c§l🔥 OFFRES FLASH")
            .lore(List.of("§7Offres limitées dans le temps!", "§7Changent toutes les 8h"))
            .build());

        inventory.setItem(4, new ItemBuilder(Material.DIAMOND)
            .name("§b§l💎 PACKS EXCLUSIFS")
            .lore(List.of("§7Offres uniques!", "§e§lUNE SEULE FOIS par compte"))
            .build());

        inventory.setItem(19, new ItemBuilder(Material.EGG)
            .name("§f§l🥚 OEUFS")
            .lore(List.of("§7Achetez des oeufs de pet"))
            .build());

        inventory.setItem(46, new ItemBuilder(Material.GOLD_NUGGET)
            .name("§e§l✧ FRAGMENTS")
            .lore(List.of("§7Achetez des fragments"))
            .build());

        // Afficher le solde du joueur
        int fragments = petData != null ? petData.getFragments() : 0;
        inventory.setItem(8, new ItemBuilder(Material.SUNFLOWER)
            .name("§6§lVotre Solde")
            .lore(List.of(
                "",
                "§7Fragments: §e" + fragments,
                "",
                "§8Utilisez les fragments pour",
                "§8acheter dans la boutique"
            ))
            .build());

        // Hot Deals (offres temporaires)
        List<TimedOffer> timedOffers = shopSystem.getTimedOffers();
        for (int i = 0; i < timedOffers.size() && i < HOT_DEALS_SLOTS.length; i++) {
            inventory.setItem(HOT_DEALS_SLOTS[i], createTimedOfferItem(timedOffers.get(i)));
        }

        // First Purchase (une seule fois)
        List<FirstPurchaseOffer> firstOffers = shopSystem.getFirstPurchaseOffers();
        for (int i = 0; i < firstOffers.size() && i < FIRST_PURCHASE_SLOTS.length; i++) {
            inventory.setItem(FIRST_PURCHASE_SLOTS[i], createFirstPurchaseItem(firstOffers.get(i)));
        }

        // Oeufs permanents
        List<ShopOffer> eggOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() != null)
            .toList();
        for (int i = 0; i < eggOffers.size() && i < EGGS_SLOTS.length; i++) {
            inventory.setItem(EGGS_SLOTS[i], createPermanentOfferItem(eggOffers.get(i)));
        }

        // Fragments permanents
        List<ShopOffer> fragmentOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() == null && o.fragments() > 0)
            .toList();
        for (int i = 0; i < fragmentOffers.size() && i < FRAGMENTS_SLOTS.length; i++) {
            inventory.setItem(FRAGMENTS_SLOTS[i], createPermanentOfferItem(fragmentOffers.get(i)));
        }

        // Récompense quotidienne
        boolean canClaimDaily = plugin.getDailyRewardSystem() != null &&
            plugin.getDailyRewardSystem().canClaim(player);
        inventory.setItem(SLOT_DAILY, new ItemBuilder(canClaimDaily ? Material.CHEST : Material.ENDER_CHEST)
            .name(canClaimDaily ? "§a§l🎁 Récompense Quotidienne!" : "§7🎁 Récompense Quotidienne")
            .lore(canClaimDaily ?
                List.of("", "§a§lCLIQUEZ POUR RÉCLAMER!", "", "§7Streak: §e" + getStreak() + " jour(s)") :
                List.of("", "§8Déjà réclamée aujourd'hui", "", "§7Streak: §e" + getStreak() + " jour(s)", "§7Revenez demain!"))
            .glow(canClaimDaily)
            .build());

        // Retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());
    }

    private int getStreak() {
        if (plugin.getDailyRewardSystem() == null) return 0;
        return plugin.getDailyRewardSystem().getStreak(player);
    }

    private ItemStack createTimedOfferItem(TimedOffer offer) {
        Duration remaining = shopSystem.getTimeRemaining(offer.id());
        String timeStr = formatDuration(remaining);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(List.of(offer.description().split("\n")));
        lore.add("");

        if (offer.discountPercent() > 0) {
            lore.add("§c§m" + (offer.price() * 100 / (100 - offer.discountPercent())) + "§r §a" + offer.price() + " fragments");
            lore.add("§a-" + offer.discountPercent() + "% de réduction!");
        } else {
            lore.add("§ePrix: §f" + offer.price() + " fragments");
        }

        lore.add("");
        lore.add("§c⏱ Expire dans: §f" + timeStr);
        lore.add("");
        lore.add("§eCliquez pour acheter!");

        Material icon = offer.eggType() != null ? offer.eggType().getIcon() : Material.CHEST;

        return new ItemBuilder(icon)
            .name(offer.name())
            .lore(lore)
            .glow(true)
            .build();
    }

    private ItemStack createFirstPurchaseItem(FirstPurchaseOffer offer) {
        boolean alreadyBought = shopSystem.hasUsedFirstPurchase(player.getUniqueId(), offer.id());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(List.of(offer.description().split("\n")));
        lore.add("");
        lore.add("§7Contenu:");

        for (RewardItem item : offer.rewards()) {
            if (item.eggType() != null) {
                lore.add("  §a• " + item.amount() + "x " + item.eggType().getColoredName());
            } else {
                lore.add("  §a• §e" + item.amount() + " §7fragments");
            }
        }

        lore.add("");
        if (offer.discountPercent() > 0) {
            int originalPrice = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§c§m" + originalPrice + "§r §a" + offer.price() + " fragments");
            lore.add("§a§l-" + offer.discountPercent() + "% §ade réduction!");
        } else {
            lore.add("§ePrix: §f" + offer.price() + " fragments");
        }

        lore.add("");
        if (alreadyBought) {
            lore.add("§c§l✗ DÉJÀ ACHETÉ");
        } else {
            lore.add("§a§lCliquez pour acheter!");
        }

        return new ItemBuilder(alreadyBought ? Material.GRAY_DYE : Material.NETHER_STAR)
            .name(offer.name())
            .lore(lore)
            .glow(!alreadyBought)
            .build();
    }

    private ItemStack createPermanentOfferItem(ShopOffer offer) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(List.of(offer.description().split("\n")));
        lore.add("");

        if (offer.discountPercent() > 0) {
            int originalPrice = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§c§m" + originalPrice + "§r §a" + offer.price() + " fragments");
            lore.add("§a-" + offer.discountPercent() + "% économisé!");
        } else {
            lore.add("§ePrix: §f" + offer.price() + " fragments");
        }

        int playerFragments = petData != null ? petData.getFragments() : 0;
        if (playerFragments >= offer.price()) {
            lore.add("");
            lore.add("§aCliquez pour acheter!");
        } else {
            lore.add("");
            lore.add("§cPas assez de fragments!");
            lore.add("§7Besoin: §c" + (offer.price() - playerFragments) + " §7de plus");
        }

        Material icon = offer.eggType() != null ? offer.eggType().getIcon() :
            (offer.fragments() > 1000 ? Material.GOLD_BLOCK : Material.GOLD_NUGGET);

        return new ItemBuilder(icon)
            .name(offer.name())
            .lore(lore)
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
                if (gui.plugin.getDailyRewardSystem() != null &&
                    gui.plugin.getDailyRewardSystem().canClaim(player)) {

                    var result = gui.plugin.getDailyRewardSystem().claimDailyReward(player);
                    if (result.success()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        player.sendMessage("");
                        player.sendMessage(result.message());
                        player.sendMessage("");
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
