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
 * GUI de la boutique de Pets - Layout clair et centré
 *
 * Structure:
 * - Header: Soldes Points | Fragments
 * - Section OEUFS (points): Achats classiques
 * - Section PROMOS (fragments): Offres spéciales mises en avant
 * - Footer: Daily + Retour
 */
public class PetShopGUI implements InventoryHolder {

    private static final String TITLE = "§0\u2800\u2800\u2800\u2800\u2800\u2800\u2800💎 Boutique Pet";
    private static final int SIZE = 54;

    // === LAYOUT CENTRÉ ===
    // Header (ligne 0)
    private static final int SLOT_POINTS = 2;
    private static final int SLOT_FRAGMENTS = 6;

    // Oeufs - Ligne 1 centrée (slots 10-16 = 7 oeufs)
    private static final int[] EGGS_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    // Conversion - Ligne 2 centrée (slots 20-22-24 = 3 conversions espacées)
    private static final int[] CONVERT_SLOTS = {20, 22, 24};

    // PROMOS - Ligne 3 avec fond spécial (offres flash + packs)
    private static final int[] PROMO_SLOTS = {28, 29, 30, 32, 33, 34}; // 3 flash | 3 packs

    // Footer - Ligne 5
    private static final int SLOT_DAILY = 40;
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
        // === FOND GRIS PAR DÉFAUT ===
        ItemStack grayGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, grayGlass);
        }

        // === HEADER (ligne 0) - Fond violet ===
        ItemStack purpleGlass = ItemBuilder.placeholder(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, purpleGlass);
        }

        // Soldes
        long points = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            points = playerData.getPoints().get();
        }
        int fragments = petData != null ? petData.getFragments() : 0;

        inventory.setItem(SLOT_POINTS, new ItemBuilder(Material.GOLD_INGOT)
            .name("§e§l⚡ " + String.format("%,d", points))
            .lore(List.of(
                "§7Points",
                "",
                "§8Achetez oeufs",
                "§8et fragments"
            ))
            .glow(true)
            .build());

        inventory.setItem(SLOT_FRAGMENTS, new ItemBuilder(Material.AMETHYST_SHARD)
            .name("§d§l💎 " + String.format("%,d", fragments))
            .lore(List.of(
                "§7Fragments",
                "",
                "§8Offres exclusives",
                "§8et promos"
            ))
            .glow(true)
            .build());

        // === SECTION OEUFS (ligne 1) - Fond vert clair ===
        ItemStack limeGlass = ItemBuilder.placeholder(Material.LIME_STAINED_GLASS_PANE);
        inventory.setItem(9, limeGlass);
        inventory.setItem(17, limeGlass);

        List<ShopOffer> eggOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() != null)
            .toList();
        for (int i = 0; i < eggOffers.size() && i < EGGS_SLOTS.length; i++) {
            inventory.setItem(EGGS_SLOTS[i], createEggItem(eggOffers.get(i)));
        }

        // === SECTION CONVERSION (ligne 2) - Fond cyan ===
        ItemStack cyanGlass = ItemBuilder.placeholder(Material.CYAN_STAINED_GLASS_PANE);
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, cyanGlass);
        }

        List<ShopOffer> convertOffers = shopSystem.getPermanentOffers().stream()
            .filter(o -> o.eggType() == null && o.fragments() > 0)
            .toList();
        for (int i = 0; i < convertOffers.size() && i < CONVERT_SLOTS.length; i++) {
            inventory.setItem(CONVERT_SLOTS[i], createConvertItem(convertOffers.get(i)));
        }

        // === SECTION PROMOS (ligne 3) - Fond orange/magenta pour attirer l'oeil ===
        ItemStack orangeGlass = ItemBuilder.placeholder(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack magentaGlass = ItemBuilder.placeholder(Material.MAGENTA_STAINED_GLASS_PANE);

        // Fond orange à gauche (offres flash)
        for (int i = 27; i < 31; i++) {
            inventory.setItem(i, orangeGlass);
        }
        // Séparateur central
        inventory.setItem(31, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§8│").build());
        // Fond magenta à droite (packs exclusifs)
        for (int i = 32; i < 36; i++) {
            inventory.setItem(i, magentaGlass);
        }

        // Offres flash (fragments) - Gauche
        List<TimedOffer> timedOffers = shopSystem.getTimedOffers();
        for (int i = 0; i < timedOffers.size() && i < 3; i++) {
            inventory.setItem(PROMO_SLOTS[i], createTimedItem(timedOffers.get(i)));
        }

        // Packs exclusifs (fragments) - Droite
        List<FirstPurchaseOffer> firstOffers = shopSystem.getFirstPurchaseOffers();
        for (int i = 0; i < firstOffers.size() && i < 3; i++) {
            inventory.setItem(PROMO_SLOTS[i + 3], createFirstPurchaseItem(firstOffers.get(i)));
        }

        // === LIGNE 4 - Espace ===
        // Déjà gris par défaut

        // === FOOTER (ligne 5) ===
        // Daily reward au centre
        boolean canClaimDaily = plugin.getDailyRewardManager() != null &&
            plugin.getDailyRewardManager().canClaim(player);
        int streak = getStreak();

        inventory.setItem(SLOT_DAILY, new ItemBuilder(canClaimDaily ? Material.CHEST : Material.ENDER_CHEST)
            .name(canClaimDaily ? "§a§l🎁 RÉCOMPENSE!" : "§7🎁 Quotidienne")
            .lore(List.of(
                "",
                canClaimDaily ? "§aCliquez pour réclamer!" : "§8Déjà réclamée",
                "§7Streak: §e" + streak + "j"
            ))
            .glow(canClaimDaily)
            .build());

        // Bouton retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());
    }

    private int getStreak() {
        if (plugin.getDailyRewardManager() == null) return 0;
        return plugin.getDailyRewardManager().getStreak(player);
    }

    private ItemStack createEggItem(ShopOffer offer) {
        long playerPoints = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            playerPoints = playerData.getPoints().get();
        }
        boolean canAfford = playerPoints >= offer.price();

        List<String> lore = new ArrayList<>();
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }
        lore.add("");

        if (offer.discountPercent() > 0) {
            int original = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§c§m" + String.format("%,d", original) + "§r §a" + String.format("%,d", offer.price()) + " §epoints");
        } else {
            lore.add("§e" + String.format("%,d", offer.price()) + " points");
        }

        lore.add(canAfford ? "§a► Acheter" : "§c✗ Fonds insuffisants");

        return new ItemBuilder(offer.eggType() != null ? offer.eggType().getIcon() : Material.EGG)
            .name(offer.name())
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private ItemStack createConvertItem(ShopOffer offer) {
        long playerPoints = 0;
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            playerPoints = playerData.getPoints().get();
        }
        boolean canAfford = playerPoints >= offer.price();

        List<String> lore = new ArrayList<>();
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }
        lore.add("");
        lore.add("§e" + String.format("%,d", offer.price()) + " points");
        lore.add(canAfford ? "§a► Convertir" : "§c✗ Fonds insuffisants");

        Material icon = offer.fragments() >= 2000 ? Material.AMETHYST_BLOCK :
                       offer.fragments() >= 500 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_SHARD;

        return new ItemBuilder(icon)
            .name(offer.name())
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private ItemStack createTimedItem(TimedOffer offer) {
        Duration remaining = shopSystem.getTimeRemaining(offer.id());
        int playerFragments = petData != null ? petData.getFragments() : 0;
        boolean canAfford = playerFragments >= offer.price();

        List<String> lore = new ArrayList<>();
        lore.add("§c§l🔥 OFFRE FLASH");
        lore.add("");
        for (String line : offer.description().split("\n")) {
            lore.add("§7" + line);
        }
        lore.add("");

        if (offer.discountPercent() > 0) {
            int original = offer.price() * 100 / (100 - offer.discountPercent());
            lore.add("§c§m" + original + "§r §d" + offer.price() + " §dfragments");
            lore.add("§a-" + offer.discountPercent() + "%!");
        } else {
            lore.add("§d" + offer.price() + " fragments");
        }

        lore.add("");
        lore.add("§c⏱ " + formatDuration(remaining));
        lore.add(canAfford ? "§a► Acheter" : "§c✗ Fragments insuffisants");

        return new ItemBuilder(offer.eggType() != null ? offer.eggType().getIcon() : Material.FIRE_CHARGE)
            .name(offer.name())
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private ItemStack createFirstPurchaseItem(FirstPurchaseOffer offer) {
        boolean bought = shopSystem.hasUsedFirstPurchase(player.getUniqueId(), offer.id());
        int playerFragments = petData != null ? petData.getFragments() : 0;
        boolean canAfford = playerFragments >= offer.price();

        List<String> lore = new ArrayList<>();

        if (bought) {
            lore.add("§8DÉJÀ ACHETÉ");
        } else {
            lore.add("§d§l⭐ EXCLUSIF");
            lore.add("§e§lUNE SEULE FOIS");
            lore.add("");
            lore.add("§fContenu:");
            for (RewardItem item : offer.rewards()) {
                if (item.eggType() != null) {
                    lore.add("§a• " + item.amount() + "x " + item.eggType().getColoredName());
                }
            }
            lore.add("");

            if (offer.discountPercent() > 0) {
                int original = offer.price() * 100 / (100 - offer.discountPercent());
                lore.add("§c§m" + original + "§r §d" + offer.price() + " §dfragments");
                lore.add("§a-" + offer.discountPercent() + "%!");
            } else {
                lore.add("§d" + offer.price() + " fragments");
            }

            lore.add(canAfford ? "§a► Acheter" : "§c✗ Fragments insuffisants");
        }

        return new ItemBuilder(bought ? Material.GRAY_DYE : Material.NETHER_STAR)
            .name(bought ? "§8" + offer.name() : offer.name())
            .lore(lore)
            .glow(!bought && canAfford)
            .build();
    }

    private String formatDuration(Duration duration) {
        if (duration.isNegative() || duration.isZero()) return "Expiré!";
        long h = duration.toHours();
        long m = duration.toMinutesPart();
        return h > 0 ? h + "h" + m + "m" : m + "min";
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
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            // Daily
            if (slot == SLOT_DAILY && gui.plugin.getDailyRewardManager() != null) {
                if (gui.plugin.getDailyRewardManager().canClaim(player)) {
                    gui.plugin.getDailyRewardManager().claimDailyReward(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    new PetShopGUI(gui.plugin, player).open();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return;
            }

            PetShopSystem shop = gui.shopSystem;
            if (shop == null) return;

            // Oeufs
            List<ShopOffer> eggs = shop.getPermanentOffers().stream().filter(o -> o.eggType() != null).toList();
            for (int i = 0; i < eggs.size() && i < EGGS_SLOTS.length; i++) {
                if (slot == EGGS_SLOTS[i]) {
                    handleResult(player, shop.buyPermanentOffer(player.getUniqueId(), eggs.get(i).id()), gui);
                    return;
                }
            }

            // Conversion
            List<ShopOffer> convert = shop.getPermanentOffers().stream().filter(o -> o.eggType() == null && o.fragments() > 0).toList();
            for (int i = 0; i < convert.size() && i < CONVERT_SLOTS.length; i++) {
                if (slot == CONVERT_SLOTS[i]) {
                    handleResult(player, shop.buyPermanentOffer(player.getUniqueId(), convert.get(i).id()), gui);
                    return;
                }
            }

            // Offres flash
            List<TimedOffer> timed = shop.getTimedOffers();
            for (int i = 0; i < timed.size() && i < 3; i++) {
                if (slot == PROMO_SLOTS[i]) {
                    handleResult(player, shop.buyTimedOffer(player.getUniqueId(), timed.get(i).id()), gui);
                    return;
                }
            }

            // Packs exclusifs
            List<FirstPurchaseOffer> first = shop.getFirstPurchaseOffers();
            for (int i = 0; i < first.size() && i < 3; i++) {
                if (slot == PROMO_SLOTS[i + 3]) {
                    handleResult(player, shop.buyFirstPurchaseOffer(player.getUniqueId(), first.get(i).id()), gui);
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

        private void handleResult(Player player, PurchaseResult result, PetShopGUI gui) {
            if (result.success()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                player.sendMessage("§a§l✓ " + result.message());
                new PetShopGUI(gui.plugin, player).open();
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c" + result.message());
            }
        }
    }
}
