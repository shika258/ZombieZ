package com.rinaorc.zombiez.zones.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.scaling.ZoneScaling;
import com.rinaorc.zombiez.utils.ItemBuilder;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GUI Wiki des zones - Affiche toutes les zones avec leurs informations
 * Les joueurs peuvent consulter, seuls les admins peuvent se teleporter
 */
public class ZoneWikiGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int ZONES_PER_PAGE = 28; // 7x4 grid

    // Slots de navigation
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_CLOSE = 47;
    private static final int SLOT_NEXT = 53;

    // Zone d'affichage des zones: slots pour une grille 7x4
    private static final int[] ZONE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    // Set pour lookup rapide O(1)
    private static final Set<Integer> ZONE_SLOTS_SET = Set.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    // Filtres par acte (ligne du haut)
    private static final int SLOT_FILTER_ALL = 0;
    private static final int[] SLOT_FILTERS = {1, 2, 3, 4, 5}; // 5 actes

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final int page;
    private final int filterAct; // 0 = tous, 1-5 = acte specifique
    private final List<Zone> filteredZones;

    public ZoneWikiGUI(ZombieZPlugin plugin, Player player, int page, int filterAct) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.filterAct = filterAct;

        // Filtrer les zones par acte
        this.filteredZones = new ArrayList<>();
        for (Zone zone : plugin.getZoneManager().getZonesSorted()) {
            if (filterAct == 0 || getActForZone(zone.getId()) == filterAct) {
                filteredZones.add(zone);
            }
        }

        String title = "§8§l\uD83D\uDDFA Wiki des Zones" + (filterAct > 0 ? " §7[Acte " + filterAct + "]" : "");
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        setupGUI();
    }

    public ZoneWikiGUI(ZombieZPlugin plugin, Player player, int page) {
        this(plugin, player, page, 0);
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordures latérales
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }

        // Filtres par acte
        setupFilters();

        // Afficher les zones
        displayZones();

        // Navigation
        setupNavigation();
    }

    private void setupFilters() {
        // Bouton "Toutes"
        boolean allSelected = filterAct == 0;
        inventory.setItem(SLOT_FILTER_ALL, new ItemBuilder(allSelected ? Material.NETHER_STAR : Material.GRAY_DYE)
            .name((allSelected ? "§a» " : "§7") + "Toutes les Zones")
            .lore(
                "",
                "§7Afficher toutes les 50 zones",
                "",
                allSelected ? "§a✓ Selectionne" : "§eCliquez pour afficher"
            )
            .glow(allSelected)
            .build());

        // Filtres par acte
        String[] actNames = {
            "Les Derniers Jours", // Acte I (1-10)
            "La Contamination",   // Acte II (11-20)
            "Le Chaos",           // Acte III (21-30)
            "L'Extinction",       // Acte IV (31-40)
            "L'Origine du Mal"    // Acte V (41-50)
        };

        Material[] actMaterials = {
            Material.OAK_SAPLING,     // Acte I - Debut
            Material.ROTTEN_FLESH,    // Acte II - Contamination
            Material.BLAZE_POWDER,    // Acte III - Chaos
            Material.BLUE_ICE,        // Acte IV - Extinction
            Material.WITHER_ROSE      // Acte V - Origine
        };

        String[] actColors = {"§a", "§2", "§c", "§b", "§5"};

        for (int i = 0; i < 5; i++) {
            int act = i + 1;
            boolean selected = filterAct == act;
            int startZone = (i * 10) + 1;
            int endZone = (i + 1) * 10;

            inventory.setItem(SLOT_FILTERS[i], new ItemBuilder(actMaterials[i])
                .name((selected ? "§a» " : "") + actColors[i] + "Acte " + toRoman(act) + " - " + actNames[i])
                .lore(
                    "",
                    "§7Zones §e" + startZone + " §7a §e" + endZone,
                    "§7Difficulte: " + getDifficultyRange(act),
                    "",
                    selected ? "§a✓ Selectionne" : "§eCliquez pour filtrer"
                )
                .glow(selected)
                .build());
        }

        // Legende (slot 8)
        inventory.setItem(8, new ItemBuilder(Material.BOOK)
            .name("§6\uD83D\uDCD6 Legende")
            .lore(
                "",
                "§7[Clic Gauche] §fVoir les details",
                "",
                "§c[Admin] Clic Droit §fTeleportation",
                "",
                "§7═══ Couleurs ═══",
                "§a✦ §7Zone facile",
                "§e✦ §7Zone moderee",
                "§c✦ §7Zone difficile",
                "§5✦ §7Zone extreme",
                "",
                "§7═══ Icones ═══",
                "§4☠ §7Zone PvP",
                "§6\uD83D\uDC51 §7Zone Boss",
                "§a♥ §7Zone Safe"
            )
            .build());
    }

    private void displayZones() {
        int startIndex = page * ZONES_PER_PAGE;
        int endIndex = Math.min(startIndex + ZONES_PER_PAGE, filteredZones.size());

        for (int i = 0; i < ZONES_PER_PAGE; i++) {
            int zoneIndex = startIndex + i;
            int slot = ZONE_SLOTS[i];

            if (zoneIndex < endIndex) {
                inventory.setItem(slot, createZoneItem(filteredZones.get(zoneIndex)));
            } else {
                inventory.setItem(slot, ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    private void setupNavigation() {
        int totalPages = Math.max(1, (int) Math.ceil(filteredZones.size() / (double) ZONES_PER_PAGE));

        // Page precedente
        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW)
                .name("§e◄ Page " + page)
                .lore("", "§7Page actuelle: §a" + (page + 1) + "§7/§e" + totalPages)
                .build());
        } else {
            inventory.setItem(SLOT_PREV, ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Info generale
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Fermer
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Fermer")
            .build());

        // Page suivante
        if ((page + 1) * ZONES_PER_PAGE < filteredZones.size()) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                .name("§ePage " + (page + 2) + " ►")
                .lore("", "§7Page actuelle: §a" + (page + 1) + "§7/§e" + totalPages)
                .build());
        } else {
            inventory.setItem(SLOT_NEXT, ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE));
        }
    }

    private ItemStack createZoneItem(Zone zone) {
        Material material = getZoneMaterial(zone);
        List<String> lore = new ArrayList<>();
        int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());

        lore.add("");
        lore.add("§7§o\"" + zone.getDescription() + "\"");
        lore.add("");

        // Difficulte et infos rapides
        lore.add("§7Difficulte: " + zone.getStarsDisplay());
        if (zone.getMinZombieLevel() > 0) {
            lore.add("§7Niveaux Mobs: §c" + zone.getMinZombieLevel() + "-" + zone.getMaxZombieLevel());
        }

        // Item Score moyen
        int avgScore = ZoneScaling.getBaseScoreForZone(zone.getId());
        lore.add("§7Item Score: §6" + formatNumber(avgScore));

        // Multiplicateurs compacts
        lore.add("");
        lore.add("§a+" + formatBonus(zone.getXpMultiplier()) + " XP §8| " +
                 "§6+" + formatBonus(zone.getLootMultiplier()) + " Loot");

        // Effets environnementaux
        if (!zone.getEnvironmentalEffect().equals("NONE")) {
            lore.add("");
            lore.add(getEnvironmentDisplay(zone.getEnvironmentalEffect()) +
                     " §8(§c" + zone.getEnvironmentalDamage() + "/s§8)");
        }

        // Flags speciaux
        if (zone.isPvpEnabled() || zone.isBossZone() || zone.isSafeZone() || zone.getRefugeId() > 0) {
            lore.add("");
            if (zone.isSafeZone()) lore.add("§a♥ Zone Securisee");
            if (zone.isPvpEnabled()) lore.add("§4☠ PvP Active");
            if (zone.isBossZone()) lore.add("§6👑 Boss Final");
            if (zone.getRefugeId() > 0) lore.add("§e🏠 Refuge #" + zone.getRefugeId());
        }

        // Joueurs presents
        if (playersInZone > 0) {
            lore.add("");
            lore.add("§7Joueurs: §a" + playersInZone + " §7present" + (playersInZone > 1 ? "s" : ""));
        }

        lore.add("");
        lore.add("§e⚲ Clic gauche §8→ §7Details");
        if (player.hasPermission("zombiez.admin")) {
            lore.add("§d⌖ Clic droit §8→ §7Teleporter");
        }

        String prefix = "";
        if (zone.isSafeZone()) prefix = "§a♥ ";
        else if (zone.isPvpEnabled()) prefix = "§4☠ ";
        else if (zone.isBossZone()) prefix = "§6👑 ";

        return new ItemBuilder(material)
            .name(prefix + zone.getColor() + "Zone " + zone.getId() + " §8- " + zone.getColor() + zone.getDisplayName())
            .lore(lore)
            .glow(zone.isBossZone() || zone.isPvpEnabled())
            .hideAttributes()
            .build();
    }

    private String formatBonus(double multiplier) {
        int bonus = (int) ((multiplier - 1) * 100);
        return bonus + "%";
    }

    private ItemStack createInfoItem() {
        Zone currentZone = plugin.getZoneManager().getPlayerZone(player);
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int highestZone = data != null ? data.getMaxZone().get() : 1;
        int playersOnline = Bukkit.getOnlinePlayers().size();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Zone actuelle: " + currentZone.getColoredName());
        lore.add("§7Position Z: §e" + player.getLocation().getBlockZ());
        lore.add("");
        lore.add("§7═══ Progression ═══");
        lore.add("§7Zone max atteinte: §a" + highestZone + "§7/§e50");
        lore.add(createProgressBar(highestZone, 50));
        lore.add("");
        lore.add("§7═══ Serveur ═══");
        lore.add("§7Joueurs en ligne: §a" + playersOnline);
        lore.add("");
        lore.add("§7═══ Conseils ═══");
        lore.add("§7• Progressez vers le §cNord §7(Z-)");
        lore.add("§7• Chaque zone fait §e200 blocs");
        lore.add("§7• Les refuges sont vos allies!");

        return new ItemBuilder(Material.COMPASS)
            .name("§6\uD83D\uDDFA Votre Progression")
            .lore(lore)
            .glow(highestZone >= 50)
            .build();
    }

    private String createProgressBar(int current, int max) {
        int filled = (int) ((current / (double) max) * 20);
        int empty = 20 - filled;
        return "§8[§a" + "▌".repeat(filled) + "§7" + "▌".repeat(empty) + "§8] §e" +
               String.format("%.0f%%", (current / (double) max) * 100);
    }

    private Material getZoneMaterial(Zone zone) {
        if (zone.isSafeZone()) return Material.EMERALD;
        if (zone.isBossZone()) return Material.NETHER_STAR;
        if (zone.isPvpEnabled()) return Material.IRON_SWORD;

        // Par theme/environnement
        return switch (zone.getEnvironmentalEffect()) {
            case "HEAT", "FIRE" -> Material.MAGMA_CREAM;
            case "COLD" -> Material.PACKED_ICE;
            case "TOXIC" -> Material.SLIME_BALL;
            case "RADIATION" -> Material.GLOWSTONE_DUST;
            case "DARKNESS" -> Material.ECHO_SHARD;
            default -> getMaterialForDifficulty(zone.getDifficulty());
        };
    }

    private Material getMaterialForDifficulty(int difficulty) {
        return switch (difficulty) {
            case 1, 2 -> Material.LIME_TERRACOTTA;
            case 3, 4 -> Material.YELLOW_TERRACOTTA;
            case 5, 6 -> Material.ORANGE_TERRACOTTA;
            case 7, 8 -> Material.RED_TERRACOTTA;
            case 9, 10 -> Material.PURPLE_TERRACOTTA;
            default -> Material.WHITE_TERRACOTTA;
        };
    }

    private String getEnvironmentDisplay(String effect) {
        return switch (effect) {
            case "HEAT" -> "§c\uD83D\uDD25 Chaleur";
            case "FIRE" -> "§4\uD83D\uDD25 Feu";
            case "COLD" -> "§b❄ Froid";
            case "TOXIC" -> "§2☠ Toxique";
            case "RADIATION" -> "§e☢ Radiation";
            case "DARKNESS" -> "§8\uD83C\uDF19 Tenebres";
            default -> "§7Aucun";
        };
    }

    private String formatMultiplier(double value) {
        return String.format("x%.1f", value);
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private int getActForZone(int zoneId) {
        if (zoneId == 0) return 1; // Spawn = Acte I
        return ((zoneId - 1) / 10) + 1;
    }

    private String getDifficultyRange(int act) {
        int startDiff = Math.min(10, (((act - 1) * 10) / 5) + 1);
        int endDiff = Math.min(10, ((act * 10) / 5) + 1);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < startDiff; i++) stars.append("§e★");
        stars.append(" §7- ");
        for (int i = 0; i < endDiff; i++) stars.append("§e★");
        return stars.toString();
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // Getters pour le listener
    public int getPage() { return page; }
    public int getFilterAct() { return filterAct; }
    public List<Zone> getFilteredZones() { return filteredZones; }
    public Player getPlayer() { return player; }
    public ZombieZPlugin getPlugin() { return plugin; }

    /**
     * Gestionnaire d'evenements
     */
    public static class GUIListener implements Listener {

        private final ZombieZPlugin plugin;

        public GUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof ZoneWikiGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Filtre "Tous"
            if (slot == SLOT_FILTER_ALL) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneWikiGUI(gui.getPlugin(), player, 0, 0).open();
                return;
            }

            // Filtres par acte
            for (int i = 0; i < SLOT_FILTERS.length; i++) {
                if (slot == SLOT_FILTERS[i]) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new ZoneWikiGUI(gui.getPlugin(), player, 0, i + 1).open();
                    return;
                }
            }

            // Navigation
            if (slot == SLOT_PREV && gui.getPage() > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneWikiGUI(gui.getPlugin(), player, gui.getPage() - 1, gui.getFilterAct()).open();
                return;
            }

            if (slot == SLOT_CLOSE) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }

            if (slot == SLOT_NEXT) {
                int maxPage = Math.max(0, (gui.getFilteredZones().size() - 1) / ZONES_PER_PAGE);
                if (gui.getPage() < maxPage) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new ZoneWikiGUI(gui.getPlugin(), player, gui.getPage() + 1, gui.getFilterAct()).open();
                }
                return;
            }

            // Clic sur une zone - lookup O(1) avec Set
            if (ZONE_SLOTS_SET.contains(slot)) {
                int slotIndex = getSlotIndex(slot);
                if (slotIndex == -1) return;

                int index = gui.getPage() * ZONES_PER_PAGE + slotIndex;
                if (index >= gui.getFilteredZones().size()) return;

                Zone zone = gui.getFilteredZones().get(index);

                if (event.isLeftClick()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    new ZoneDetailGUI(gui.getPlugin(), player, zone, gui.getPage(), gui.getFilterAct()).open();
                } else if (event.isRightClick() && player.hasPermission("zombiez.admin")) {
                    teleportToZone(player, zone);
                } else if (event.isRightClick()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    player.sendMessage("§c§l[ZombieZ] §cReserve aux administrateurs!");
                }
            }
        }

        private int getSlotIndex(int slot) {
            for (int i = 0; i < ZONE_SLOTS.length; i++) {
                if (ZONE_SLOTS[i] == slot) return i;
            }
            return -1;
        }

        private void teleportToZone(Player player, Zone zone) {
            // Particules avant TP
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);

            // Teleporter au debut de la zone (maxZ - 10)
            int targetZ = zone.getMaxZ() - 10;
            org.bukkit.Location loc = player.getLocation().clone();
            loc.setZ(targetZ);
            loc.setY(player.getWorld().getHighestBlockYAt(loc) + 1);

            player.closeInventory();
            player.teleport(loc);

            // Effets apres TP
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
            player.sendMessage("§a§l[ZombieZ] §aTeleporte vers " + zone.getColor() + zone.getDisplayName() + "§a!");
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof ZoneWikiGUI) {
                event.setCancelled(true);
            }
        }
    }
}
