package com.rinaorc.zombiez.zones.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.ItemBuilder;
import com.rinaorc.zombiez.zones.Zone;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Menu Wiki des Zones - Liste paginée de toutes les zones
 * Permet aux joueurs de consulter les infos et aux admins de se téléporter
 */
public class ZoneWikiGUI implements InventoryHolder {

    private static final String TITLE = "§8§l🗺 Wiki des Zones";
    private static final int SIZE = 54; // 6 lignes

    // Pagination
    private static final int ZONES_PER_PAGE = 28; // 4 lignes de 7 zones
    private static final int[] ZONE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Ligne 2
        19, 20, 21, 22, 23, 24, 25,  // Ligne 3
        28, 29, 30, 31, 32, 33, 34,  // Ligne 4
        37, 38, 39, 40, 41, 42, 43   // Ligne 5
    };

    // Navigation
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT_PAGE = 53;
    private static final int SLOT_FILTER_ACT = 47;
    private static final int SLOT_SEARCH = 51;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final int page;
    private final int actFilter; // 0 = tous, 1-5 = acte spécifique

    public ZoneWikiGUI(ZombieZPlugin plugin, Player player) {
        this(plugin, player, 0, 0);
    }

    public ZoneWikiGUI(ZombieZPlugin plugin, Player player, int page, int actFilter) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.actFilter = actFilter;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE + (actFilter > 0 ? " - Acte " + actFilter : "") + " §7(" + (page + 1) + ")");

        setupGUI();
    }

    private void setupGUI() {
        // Bordure noire en haut
        ItemStack headerGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, headerGlass);
        }

        // Bordures latérales grises
        ItemStack sideGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        int[] sideslots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideslots) {
            inventory.setItem(slot, sideGlass);
        }

        // Bordure noire en bas
        ItemStack footerGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, footerGlass);
        }

        // Remplir les slots de zones avec du verre gris par défaut
        ItemStack emptySlot = ItemBuilder.placeholder(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int slot : ZONE_SLOTS) {
            inventory.setItem(slot, emptySlot);
        }

        // Obtenir les zones filtrées
        List<Zone> zones = getFilteredZones();
        int startIndex = page * ZONES_PER_PAGE;
        int endIndex = Math.min(startIndex + ZONES_PER_PAGE, zones.size());

        // Données du joueur
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int maxZoneReached = data != null ? data.getMaxZone().get() : 1;
        int currentZone = data != null ? data.getCurrentZone().get() : 0;
        boolean isAdmin = player.hasPermission("zombiez.admin");

        // Afficher les zones
        for (int i = startIndex; i < endIndex; i++) {
            Zone zone = zones.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < ZONE_SLOTS.length) {
                inventory.setItem(ZONE_SLOTS[slotIndex], createZoneItem(zone, maxZoneReached, currentZone, isAdmin));
            }
        }

        // Navigation
        int totalPages = (int) Math.ceil((double) zones.size() / ZONES_PER_PAGE);

        // Bouton page précédente
        if (page > 0) {
            inventory.setItem(SLOT_PREV_PAGE, new ItemBuilder(Material.ARROW)
                .name("§e◀ Page précédente")
                .lore(
                    "",
                    "§7Page actuelle: §f" + (page + 1) + "§7/§f" + totalPages,
                    "",
                    "§eCliquez pour revenir"
                )
                .build());
        } else {
            inventory.setItem(SLOT_PREV_PAGE, headerGlass);
        }

        // Bouton page suivante
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT_PAGE, new ItemBuilder(Material.ARROW)
                .name("§ePage suivante ▶")
                .lore(
                    "",
                    "§7Page actuelle: §f" + (page + 1) + "§7/§f" + totalPages,
                    "",
                    "§eCliquez pour continuer"
                )
                .build());
        } else {
            inventory.setItem(SLOT_NEXT_PAGE, headerGlass);
        }

        // Filtre par acte
        inventory.setItem(SLOT_FILTER_ACT, createActFilterItem());

        // Infos générales
        inventory.setItem(SLOT_INFO, createInfoItem(zones.size(), maxZoneReached));

        // Recherche (placeholder pour future feature)
        inventory.setItem(SLOT_SEARCH, new ItemBuilder(Material.COMPASS)
            .name("§b🔍 Navigation rapide")
            .lore(
                "",
                "§7Zone actuelle: §a" + currentZone,
                "§7Zone max atteinte: §e" + maxZoneReached,
                "",
                "§eCliquez pour aller à votre zone"
            )
            .build());
    }

    /**
     * Crée l'item représentant une zone
     */
    private ItemStack createZoneItem(Zone zone, int maxZoneReached, int currentZone, boolean isAdmin) {
        boolean unlocked = zone.getId() <= maxZoneReached || zone.getId() == 0;
        boolean isCurrent = zone.getId() == currentZone;

        // Choisir le matériau en fonction de l'état
        Material material;
        if (zone.isSafeZone()) {
            material = Material.EMERALD;
        } else if (zone.isBossZone()) {
            material = Material.DRAGON_HEAD;
        } else if (zone.isPvpEnabled()) {
            material = Material.IRON_SWORD;
        } else if (!unlocked) {
            material = Material.BARRIER;
        } else {
            material = getMaterialForDifficulty(zone.getDifficulty());
        }

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        if (zone.getDescription() != null && !zone.getDescription().isEmpty()) {
            lore.add("§7\"" + zone.getDescription() + "\"");
            lore.add("");
        }

        // Section Difficulté
        lore.add("§e§l▸ DIFFICULTÉ");
        lore.add("  §7Niveau: " + zone.getStarsDisplay());
        lore.add("  §7Zombies: §cNiv." + zone.getMinZombieLevel() + "-" + zone.getMaxZombieLevel());
        lore.add("");

        // Section Multiplicateurs
        lore.add("§6§l▸ MULTIPLICATEURS");
        lore.add("  §7Bonus XP: §a+" + (int) ((zone.getXpMultiplier() - 1) * 100) + "%");
        lore.add("  §7Bonus Loot: §e+" + (int) ((zone.getLootMultiplier() - 1) * 100) + "%");
        lore.add("  §7Taux Spawn: §c" + String.format("%.0f", zone.getSpawnRateMultiplier() * 100) + "%");
        lore.add("");

        // Section Score Items (estimation basée sur la zone)
        lore.add("§d§l▸ SCORE ITEMS MOYEN");
        int[] scoreRange = getEstimatedScoreRange(zone.getId());
        lore.add("  §7Commun: §f" + formatScore(scoreRange[0]) + " - " + formatScore(scoreRange[1]));
        lore.add("  §7Légendaire: §6" + formatScore(scoreRange[2]) + " - " + formatScore(scoreRange[3]));
        lore.add("");

        // Section Taux de loot
        lore.add("§b§l▸ TAUX DE DROP");
        int tier = getZombieTier(zone.getId());
        String[] dropRates = getDropRatesForTier(tier);
        lore.add("  §7Chance de drop: §e" + dropRates[0]);
        lore.add("  §7Raretés: §f" + dropRates[1]);
        lore.add("");

        // Flags spéciaux
        if (zone.isPvpEnabled() || zone.isSafeZone() || zone.isBossZone() || zone.isDangerous()) {
            lore.add("§c§l▸ FLAGS SPÉCIAUX");
            if (zone.isPvpEnabled()) lore.add("  §c⚔ PvP activé");
            if (zone.isSafeZone()) lore.add("  §a🛡 Zone sécurisée");
            if (zone.isBossZone()) lore.add("  §5👑 Zone de Boss");
            if (zone.isDangerous()) {
                lore.add("  §e⚠ " + formatEnvironmentalEffect(zone.getEnvironmentalEffect()));
                lore.add("    §7Dégâts: §c" + zone.getEnvironmentalDamage() + "/s");
            }
            lore.add("");
        }

        // Refuge
        if (zone.getRefugeId() > 0) {
            lore.add("§a✓ §7Refuge disponible");
            lore.add("");
        }

        // Joueurs actuels
        int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());
        if (playersInZone > 0) {
            lore.add("§7Joueurs: §e" + playersInZone);
            lore.add("");
        }

        // Action
        if (isCurrent) {
            lore.add("§a§l► VOUS ÊTES ICI");
        } else if (isAdmin) {
            lore.add("§e[Clic] §7Voir les détails");
            lore.add("§6[Clic-Droit] §7Se téléporter §c(Admin)");
        } else if (unlocked) {
            lore.add("§e[Clic] §7Voir les détails");
        } else {
            lore.add("§c✖ Zone non découverte");
            lore.add("§7Atteignez la zone " + zone.getId() + " pour débloquer");
        }

        // Construire l'item
        ItemBuilder builder = new ItemBuilder(material)
            .name((isCurrent ? "§a► " : "") + zone.getColoredName() + " §7(Zone " + zone.getId() + ")")
            .lore(lore);

        if (isCurrent || zone.isBossZone()) {
            builder.glow(true);
        }

        return builder.build();
    }

    /**
     * Crée l'item de filtre par acte
     */
    private ItemStack createActFilterItem() {
        String currentFilter = actFilter == 0 ? "§aToutes les zones" : "§eActe " + actFilter;

        return new ItemBuilder(Material.BOOK)
            .name("§6📚 Filtrer par Acte")
            .lore(
                "",
                "§7Filtre actuel: " + currentFilter,
                "",
                "§7• §fActe I §7(Zones 1-10) - Les Derniers Jours",
                "§7• §fActe II §7(Zones 11-20) - La Contamination",
                "§7• §fActe III §7(Zones 21-30) - Le Chaos",
                "§7• §fActe IV §7(Zones 31-40) - L'Extinction",
                "§7• §fActe V §7(Zones 41-50) - L'Origine du Mal",
                "",
                "§eCliquez pour changer le filtre"
            )
            .build();
    }

    /**
     * Crée l'item d'information centrale
     */
    private ItemStack createInfoItem(int totalZones, int maxZoneReached) {
        double progress = (double) maxZoneReached / 50.0 * 100;
        String progressBar = createProgressBar(maxZoneReached, 50, 20);

        return new ItemBuilder(Material.FILLED_MAP)
            .name("§6§l🗺 Wiki des Zones")
            .lore(
                "",
                "§7Bienvenue dans le Wiki des Zones!",
                "§7Consultez les informations sur",
                "§7chaque zone du monde.",
                "",
                "§e§l▸ VOTRE PROGRESSION",
                "  " + progressBar,
                "  §7Zones découvertes: §a" + maxZoneReached + "§7/§e50",
                "  §7Progression: §f" + String.format("%.1f", progress) + "%",
                "",
                "§b§l▸ LÉGENDE",
                "  §a● §7Accessible",
                "  §c● §7Non découvert",
                "  §e● §7Zone actuelle",
                "",
                "§7Les admins peuvent se téléporter",
                "§7via clic-droit sur une zone."
            )
            .build();
    }

    /**
     * Obtient les zones filtrées par acte
     */
    private List<Zone> getFilteredZones() {
        List<Zone> allZones = plugin.getZoneManager().getZonesSorted();
        if (actFilter == 0) {
            // Exclure la zone 0 (spawn) de la liste
            return allZones.stream()
                .filter(z -> z.getId() > 0)
                .toList();
        }

        int minZone = (actFilter - 1) * 10 + 1;
        int maxZone = actFilter * 10;

        return allZones.stream()
            .filter(z -> z.getId() >= minZone && z.getId() <= maxZone)
            .toList();
    }

    /**
     * Estime la fourchette de score d'items pour une zone
     */
    private int[] getEstimatedScoreRange(int zoneId) {
        // Basé sur ZoneScaling et le système de score
        // Score = (zoneBaseScore + statsContribution) × rarityMultiplier × zoneMultiplier
        double zoneMultiplier = 1.0 + (zoneId * 0.05); // 1.0 → 3.5

        // Score de base par zone (approximation)
        int baseScore = 200 + (zoneId * 80);

        // Fourchettes pour différentes raretés
        int commonMin = (int) (baseScore * 0.8);
        int commonMax = (int) (baseScore * 1.2);
        int legendaryMin = (int) (baseScore * 2.5);
        int legendaryMax = (int) (baseScore * 4.0);

        return new int[]{commonMin, commonMax, legendaryMin, legendaryMax};
    }

    /**
     * Détermine le tier de zombie pour une zone
     */
    private int getZombieTier(int zoneId) {
        if (zoneId <= 10) return 1;
        if (zoneId <= 20) return 2;
        if (zoneId <= 30) return 3;
        if (zoneId <= 40) return 4;
        return 5;
    }

    /**
     * Obtient les taux de drop pour un tier de zombie
     */
    private String[] getDropRatesForTier(int tier) {
        return switch (tier) {
            case 1 -> new String[]{"15%", "Commun → Peu commun"};
            case 2 -> new String[]{"25%", "Commun → Rare"};
            case 3 -> new String[]{"18%", "Peu commun → Épique"};
            case 4 -> new String[]{"22%", "Rare → Légendaire"};
            case 5 -> new String[]{"30%", "Rare → Mythique"};
            default -> new String[]{"15%", "Commun"};
        };
    }

    /**
     * Obtient le matériau basé sur la difficulté
     */
    private Material getMaterialForDifficulty(int difficulty) {
        return switch (difficulty) {
            case 1, 2 -> Material.LIME_TERRACOTTA;
            case 3, 4 -> Material.YELLOW_TERRACOTTA;
            case 5, 6 -> Material.ORANGE_TERRACOTTA;
            case 7, 8 -> Material.RED_TERRACOTTA;
            case 9, 10 -> Material.BLACK_TERRACOTTA;
            default -> Material.WHITE_TERRACOTTA;
        };
    }

    /**
     * Formate l'effet environnemental
     */
    private String formatEnvironmentalEffect(String effect) {
        return switch (effect) {
            case "HEAT" -> "Chaleur extrême";
            case "COLD" -> "Froid glacial";
            case "TOXIC" -> "Zone toxique";
            case "RADIATION" -> "Radiations";
            case "FIRE" -> "Zone enflammée";
            case "DARKNESS" -> "Ténèbres profondes";
            default -> effect;
        };
    }

    /**
     * Formate un score pour l'affichage
     */
    private String formatScore(int score) {
        if (score < 1000) return String.valueOf(score);
        if (score < 1000000) return String.format("%.1fK", score / 1000.0);
        return String.format("%.1fM", score / 1000000.0);
    }

    /**
     * Crée une barre de progression
     */
    private String createProgressBar(int current, int max, int length) {
        if (max == 0) return "§8" + "▌".repeat(length);
        int filled = Math.min(length, (int) ((current * (double) length) / max));
        int empty = length - filled;
        return "§a" + "▌".repeat(filled) + "§8" + "▌".repeat(empty);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public int getPage() {
        return page;
    }

    public int getActFilter() {
        return actFilter;
    }

    public ZombieZPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gestionnaire d'événements pour le GUI
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

            // Navigation
            if (slot == SLOT_PREV_PAGE && gui.getPage() > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneWikiGUI(gui.getPlugin(), player, gui.getPage() - 1, gui.getActFilter()).open();
                return;
            }

            if (slot == SLOT_NEXT_PAGE) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneWikiGUI(gui.getPlugin(), player, gui.getPage() + 1, gui.getActFilter()).open();
                return;
            }

            // Filtre par acte
            if (slot == SLOT_FILTER_ACT) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                int newFilter = (gui.getActFilter() + 1) % 6; // 0-5
                new ZoneWikiGUI(gui.getPlugin(), player, 0, newFilter).open();
                return;
            }

            // Navigation rapide vers zone actuelle
            if (slot == SLOT_SEARCH) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
                int currentZone = data != null ? data.getCurrentZone().get() : 1;

                // Calculer la page de la zone actuelle
                int targetPage = (currentZone - 1) / ZONES_PER_PAGE;
                new ZoneWikiGUI(gui.getPlugin(), player, targetPage, 0).open();
                return;
            }

            // Clic sur une zone
            for (int i = 0; i < ZONE_SLOTS.length; i++) {
                if (slot == ZONE_SLOTS[i]) {
                    List<Zone> zones = gui.getFilteredZones();
                    int zoneIndex = gui.getPage() * ZONES_PER_PAGE + i;

                    if (zoneIndex < zones.size()) {
                        Zone zone = zones.get(zoneIndex);

                        if (event.isRightClick() && player.hasPermission("zombiez.admin")) {
                            // Téléportation admin
                            teleportToZone(player, zone);
                        } else {
                            // Ouvrir détails
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            new ZoneDetailGUI(gui.getPlugin(), player, zone).open();
                        }
                    }
                    return;
                }
            }
        }

        private List<Zone> getFilteredZones(ZoneWikiGUI gui) {
            List<Zone> allZones = gui.getPlugin().getZoneManager().getZonesSorted();
            int actFilter = gui.getActFilter();

            if (actFilter == 0) {
                return allZones.stream()
                    .filter(z -> z.getId() > 0)
                    .toList();
            }

            int minZone = (actFilter - 1) * 10 + 1;
            int maxZone = actFilter * 10;

            return allZones.stream()
                .filter(z -> z.getId() >= minZone && z.getId() <= maxZone)
                .toList();
        }

        private void teleportToZone(Player player, Zone zone) {
            // Calculer le centre de la zone
            int centerZ = (zone.getMinZ() + zone.getMaxZ()) / 2;
            int centerX = 621; // Centre de la map

            // Trouver un Y sécurisé
            var world = Bukkit.getWorld(plugin.getZoneManager().getGameWorld());
            if (world == null) {
                player.sendMessage("§c[Wiki] Erreur: Monde non trouvé!");
                return;
            }

            int y = world.getHighestBlockYAt(centerX, centerZ) + 1;

            org.bukkit.Location loc = new org.bukkit.Location(world, centerX + 0.5, y, centerZ + 0.5);
            player.teleport(loc);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage("");
            player.sendMessage("§a[Wiki] §7Téléporté vers " + zone.getColoredName());
            player.sendMessage("§7Position: §fX:" + centerX + " Y:" + y + " Z:" + centerZ);
            player.sendMessage("");
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof ZoneWikiGUI) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Méthode helper pour accéder aux zones filtrées depuis le listener
     */
    private List<Zone> getFilteredZones() {
        List<Zone> allZones = plugin.getZoneManager().getZonesSorted();
        if (actFilter == 0) {
            return allZones.stream()
                .filter(z -> z.getId() > 0)
                .toList();
        }

        int minZone = (actFilter - 1) * 10 + 1;
        int maxZone = actFilter * 10;

        return allZones.stream()
            .filter(z -> z.getId() >= minZone && z.getId() <= maxZone)
            .toList();
    }
}
