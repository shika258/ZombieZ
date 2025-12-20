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
        // Remplir le fond avec une couleur sombre
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name("§8")
            .build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordures latérales décoratives avec couleur de l'acte sélectionné
        Material borderMaterial = getBorderMaterialForAct(filterAct);
        ItemStack border = new ItemBuilder(borderMaterial)
            .name("§8")
            .build();
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }

        // Slot 6 et 7 - Séparateurs entre filtres et légende
        ItemStack separator = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name("§8")
            .build();
        inventory.setItem(6, separator);
        inventory.setItem(7, separator);

        // Filtres par acte
        setupFilters();

        // Afficher les zones
        displayZones();

        // Navigation
        setupNavigation();
    }

    /**
     * Retourne le matériau de bordure basé sur l'acte sélectionné
     */
    private Material getBorderMaterialForAct(int act) {
        return switch (act) {
            case 1 -> Material.LIME_STAINED_GLASS_PANE;    // Acte I - Vert clair
            case 2 -> Material.GREEN_STAINED_GLASS_PANE;   // Acte II - Vert foncé
            case 3 -> Material.ORANGE_STAINED_GLASS_PANE;  // Acte III - Orange
            case 4 -> Material.LIGHT_BLUE_STAINED_GLASS_PANE; // Acte IV - Bleu clair
            case 5 -> Material.PURPLE_STAINED_GLASS_PANE;  // Acte V - Violet
            default -> Material.GRAY_STAINED_GLASS_PANE;   // Tous - Gris
        };
    }

    private void setupFilters() {
        // Bouton "Toutes les Zones"
        boolean allSelected = filterAct == 0;
        inventory.setItem(SLOT_FILTER_ALL, new ItemBuilder(allSelected ? Material.ENDER_CHEST : Material.CHEST)
            .name((allSelected ? "§a▸ " : "§7") + "Toutes les Zones")
            .lore(
                "",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Afficher les §f50 zones §7du jeu",
                "§7organisees par progression.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "",
                allSelected ? "§a✔ Actuellement selectionne" : "§e▸ Cliquez pour afficher"
            )
            .glow(allSelected)
            .build());

        // Configuration des 5 actes avec leurs caractéristiques
        String[] actNames = {
            "Les Derniers Jours",   // Acte I (1-10)
            "La Contamination",      // Acte II (11-20)
            "Le Chaos",              // Acte III (21-30)
            "L'Extinction",          // Acte IV (31-40)
            "L'Origine du Mal"       // Acte V (41-50)
        };

        String[] actDescriptions = {
            "Civilisation en ruines",
            "La nature se corrompt",
            "Destruction totale",
            "Froid et mort",
            "Corruption absolue"
        };

        // Items représentatifs de chaque acte
        Material[] actMaterials = {
            Material.SHIELD,            // Acte I - Civilisation
            Material.BROWN_MUSHROOM,    // Acte II - Nature corrompue
            Material.FIRE_CHARGE,       // Acte III - Destruction
            Material.PACKED_ICE,        // Acte IV - Froid
            Material.DRAGON_EGG         // Acte V - Origine
        };

        String[] actColors = {"§a", "§2", "§6", "§b", "§5"};
        String[] actSymbols = {"🏰", "🍄", "🔥", "❄", "💀"};

        for (int i = 0; i < 5; i++) {
            int act = i + 1;
            boolean selected = filterAct == act;
            int startZone = (i * 10) + 1;
            int endZone = (i + 1) * 10;

            inventory.setItem(SLOT_FILTERS[i], new ItemBuilder(actMaterials[i])
                .name((selected ? "§a▸ " : "") + actColors[i] + "§l" + actSymbols[i] + " Acte " + toRoman(act))
                .lore(
                    actColors[i] + actNames[i],
                    "",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7" + actDescriptions[i],
                    "",
                    "§7Zones: " + actColors[i] + startZone + " §8→ " + actColors[i] + endZone,
                    "§7Difficulte: " + getDifficultyRange(act),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "",
                    selected ? "§a✔ Actuellement selectionne" : "§e▸ Cliquez pour filtrer"
                )
                .glow(selected)
                .build());
        }

        // Legende améliorée (slot 8)
        inventory.setItem(8, new ItemBuilder(Material.KNOWLEDGE_BOOK)
            .name("§6§l📖 Guide du Wiki")
            .lore(
                "",
                "§8▬▬▬ §e§lCONTROLES §8▬▬▬",
                "§a⚲ Clic gauche §8→ §7Details de la zone",
                player.hasPermission("zombiez.admin")
                    ? "§d⌖ Clic droit §8→ §7Teleportation (Admin)"
                    : "",
                "",
                "§8▬▬▬ §e§lDIFFICULTE §8▬▬▬",
                "§a★☆☆☆☆☆☆ §8→ §7Debutant",
                "§e★★★☆☆☆☆ §8→ §7Intermediaire",
                "§6★★★★★☆☆ §8→ §7Avance",
                "§c★★★★★★☆ §8→ §7Expert",
                "§4★★★★★★★ §8→ §7Legendaire",
                "",
                "§8▬▬▬ §e§lZONES SPECIALES §8▬▬▬",
                "§a♥ Zone Safe §8→ §7Pas de mobs",
                "§c☠ Zone PvP §8→ §7Combat joueurs",
                "§d👑 Zone Boss §8→ §7Boss final"
            )
            .glow(true)
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

        // Ligne de navigation avec bordure décorative
        ItemStack navBorder = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE)
            .name("§8")
            .build();

        // Décorer les slots de navigation non utilisés
        for (int slot : new int[]{46, 48, 50, 51, 52}) {
            inventory.setItem(slot, navBorder);
        }

        // Page précédente
        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§a§l◄ Page Precedente")
                .lore(
                    "",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Aller a la page §e" + page,
                    "",
                    "§7Navigation: §a" + (page + 1) + "§7/§e" + totalPages,
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "",
                    "§e▸ Cliquez pour naviguer"
                )
                .build());
        } else {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("§8◄ Debut de liste")
                .lore("", "§7Vous etes a la premiere page")
                .build());
        }

        // Info générale au centre
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Bouton fermer avec style
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c§l✖ Fermer le Menu")
            .lore(
                "",
                "§7Retourner au jeu",
                "",
                "§c▸ Cliquez pour fermer"
            )
            .build());

        // Page suivante
        if ((page + 1) * ZONES_PER_PAGE < filteredZones.size()) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§a§lPage Suivante ►")
                .lore(
                    "",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Aller a la page §e" + (page + 2),
                    "",
                    "§7Navigation: §a" + (page + 1) + "§7/§e" + totalPages,
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "",
                    "§e▸ Cliquez pour naviguer"
                )
                .build());
        } else {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("§8Fin de liste ►")
                .lore("", "§7Vous etes a la derniere page")
                .build());
        }
    }

    private ItemStack createZoneItem(Zone zone) {
        Material material = getZoneMaterial(zone);
        List<String> lore = new ArrayList<>();
        int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());

        // Couleur de l'acte pour la cohérence visuelle
        String actColor = getActColor(zone.getId());

        // Description italique
        lore.add("");
        lore.add("§7§o\"" + zone.getDescription() + "\"");

        // Séparateur
        lore.add("");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Difficulté avec étoiles
        lore.add("§7Difficulte: " + zone.getStarsDisplay());

        // Niveaux des mobs si applicable
        if (zone.getMinZombieLevel() > 0) {
            lore.add("§7Niveaux Mobs: §c⚔ " + zone.getMinZombieLevel() + " §8→ §c" + zone.getMaxZombieLevel());
        }

        // Item Score recommandé
        int avgScore = ZoneScaling.getBaseScoreForZone(zone.getId());
        lore.add("§7Item Score: §6⚡ " + formatNumber(avgScore));

        // Séparateur bonus
        lore.add("");
        lore.add("§8▬▬▬ §e§lBONUS §8▬▬▬");
        lore.add("§a✦ +" + formatBonus(zone.getXpMultiplier()) + " XP");
        lore.add("§6✦ +" + formatBonus(zone.getLootMultiplier()) + " Loot");

        // Effets environnementaux
        if (!zone.getEnvironmentalEffect().equals("NONE")) {
            lore.add("");
            lore.add("§8▬▬▬ §c§lDANGER §8▬▬▬");
            lore.add(getEnvironmentDisplay(zone.getEnvironmentalEffect()) +
                     " §8(§c" + String.format("%.1f", zone.getEnvironmentalDamage()) + "❤/s§8)");
        }

        // Flags spéciaux avec icônes améliorées
        if (zone.isPvpEnabled() || zone.isBossZone() || zone.isSafeZone() || zone.getRefugeId() > 0) {
            lore.add("");
            lore.add("§8▬▬▬ §f§lSPECIAL §8▬▬▬");
            if (zone.isSafeZone()) lore.add("§a♥ Zone Securisee");
            if (zone.isPvpEnabled()) lore.add("§c☠ PvP Active §8- §7Combat joueurs");
            if (zone.isBossZone()) lore.add("§d👑 Zone Boss Final");
            if (zone.getRefugeId() > 0) lore.add("§e🏠 Refuge #" + zone.getRefugeId() + " §7disponible");
        }

        // Joueurs présents avec indicateur visuel
        if (playersInZone > 0) {
            lore.add("");
            String playerIndicator = playersInZone > 5 ? "§c" : (playersInZone > 2 ? "§e" : "§a");
            lore.add("§7Joueurs: " + playerIndicator + "● " + playersInZone + " §7present" + (playersInZone > 1 ? "s" : ""));
        }

        // Instructions d'interaction
        lore.add("");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§a⚲ Clic gauche §8→ §7Voir details");
        if (player.hasPermission("zombiez.admin")) {
            lore.add("§d⌖ Clic droit §8→ §7Teleporter");
        }

        // Préfixe pour les zones spéciales
        String prefix = "";
        if (zone.isSafeZone()) prefix = "§a♥ ";
        else if (zone.isPvpEnabled()) prefix = "§c☠ ";
        else if (zone.isBossZone()) prefix = "§d👑 ";

        // Formatage du numéro de zone avec padding
        String zoneNum = String.format("%02d", zone.getId());

        return new ItemBuilder(material)
            .name(prefix + zone.getColor() + "§l#" + zoneNum + " §8| " + zone.getColor() + zone.getDisplayName())
            .lore(lore)
            .glow(zone.isBossZone() || zone.isPvpEnabled() || zone.isSafeZone())
            .hideAttributes()
            .build();
    }

    /**
     * Retourne la couleur associée à l'acte de la zone
     */
    private String getActColor(int zoneId) {
        if (zoneId == 0) return "§a"; // Spawn
        int act = ((zoneId - 1) / 10) + 1;
        return switch (act) {
            case 1 -> "§a"; // Acte I - Vert
            case 2 -> "§2"; // Acte II - Vert foncé
            case 3 -> "§6"; // Acte III - Orange
            case 4 -> "§b"; // Acte IV - Cyan
            case 5 -> "§5"; // Acte V - Violet
            default -> "§7";
        };
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
        int currentAct = currentZone.getId() == 0 ? 0 : ((currentZone.getId() - 1) / 10) + 1;

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Position actuelle avec style
        lore.add("§8▬▬▬ §e§lPOSITION §8▬▬▬");
        lore.add("§7Zone: " + currentZone.getColoredName());
        if (currentAct > 0) {
            lore.add("§7Acte: " + getActColor(currentZone.getId()) + "Acte " + toRoman(currentAct));
        }
        lore.add("§7Coord Z: §e" + player.getLocation().getBlockZ());

        // Progression du joueur
        lore.add("");
        lore.add("§8▬▬▬ §a§lPROGRESSION §8▬▬▬");
        lore.add("§7Zone max: §a" + highestZone + "§7/§e50");
        lore.add(createProgressBar(highestZone, 50));

        // Actes débloqués
        int unlockedActs = Math.min(5, ((highestZone - 1) / 10) + 1);
        StringBuilder actsDisplay = new StringBuilder("§7Actes: ");
        for (int i = 1; i <= 5; i++) {
            if (i <= unlockedActs) {
                actsDisplay.append(getActColor(i * 10)).append("▰");
            } else {
                actsDisplay.append("§8▱");
            }
        }
        lore.add(actsDisplay.toString());

        // Statistiques serveur
        lore.add("");
        lore.add("§8▬▬▬ §b§lSERVEUR §8▬▬▬");
        String onlineColor = playersOnline > 50 ? "§a" : (playersOnline > 20 ? "§e" : "§7");
        lore.add("§7Joueurs: " + onlineColor + playersOnline + " §7en ligne");

        // Conseils utiles
        lore.add("");
        lore.add("§8▬▬▬ §6§lCONSEILS §8▬▬▬");
        lore.add("§7• Direction: §cNord §7(Z-)");
        lore.add("§7• Taille zone: §e200 blocs");
        lore.add("§7• Utilisez les §eRefuges §7!");

        // Badge de completion
        if (highestZone >= 50) {
            lore.add("");
            lore.add("§d§l✦ MAITRE DES ZONES ✦");
        }

        return new ItemBuilder(Material.RECOVERY_COMPASS)
            .name("§6§l🗺 Votre Progression")
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

    /**
     * Retourne un item unique et thématique pour chaque zone
     * Organisé par acte avec des items représentatifs du thème de chaque zone
     */
    private Material getZoneMaterial(Zone zone) {
        // Zones spéciales en priorité
        if (zone.isSafeZone()) return Material.EMERALD_BLOCK;
        if (zone.isBossZone()) return Material.DRAGON_HEAD;
        if (zone.isPvpEnabled()) return Material.NETHERITE_SWORD;

        // Item unique par zone basé sur le thème
        return switch (zone.getId()) {
            // === ACTE I - Les Derniers Jours (Civilisation) ===
            case 1 -> Material.SHIELD;                    // Bastion du Réveil - château médiéval
            case 2 -> Material.LANTERN;                   // Faubourgs Oubliés - quartiers abandonnés
            case 3 -> Material.WHEAT;                     // Champs du Silence - terres agricoles
            case 4 -> Material.APPLE;                     // Verger des Pendus - verger macabre
            case 5 -> Material.SADDLE;                    // Route des Fuyards - véhicules abandonnés
            case 6 -> Material.FLOWER_POT;                // Hameau Brisé - petit village
            case 7 -> Material.DARK_OAK_LEAVES;           // Bois des Soupirs - forêt sombre
            case 8 -> Material.CRACKED_STONE_BRICKS;      // Ruines de Clairval - vestiges
            case 9 -> Material.CHAIN;                     // Pont des Disparus - pont tragique
            case 10 -> Material.CROSSBOW;                 // Avant-Poste Déserté - camp militaire

            // === ACTE II - La Contamination (Nature corrompue) ===
            case 11 -> Material.BROWN_MUSHROOM_BLOCK;     // Forêt Putréfiée - arbres malades
            case 12 -> Material.BELL;                     // Clairière des Hurlements - cris
            case 13 -> Material.LILY_PAD;                 // Marais Infect - eaux stagnantes
            case 14 -> Material.VINE;                     // Jardins Dévoyés - plantes mutées
            case 15 -> Material.MOSS_BLOCK;               // Village Moisi - moisissure vivante
            case 16 -> Material.SWEET_BERRIES;            // Ronces Noires - ronces géantes
            case 17 -> Material.BONE;                     // Territoire des Errants - zombies marcheurs
            case 18 -> Material.CHARCOAL;                 // Campement Calciné - camp brûlé
            case 19 -> Material.RED_MUSHROOM;             // Bois Rouge - feuilles rouges/sang
            case 20 -> Material.ENDER_EYE;                // Lisière de la Peur - frontière

            // === ACTE III - Le Chaos (Destruction) ===
            case 21 -> Material.BLAZE_POWDER;             // Faille Incandescente - fissure enflammée
            case 22 -> Material.GUNPOWDER;                // Cratères de Cendre - explosions
            case 23 -> Material.FIRE_CHARGE;              // Plaines Brûlées - carbonisées
            case 24 -> Material.NETHERITE_INGOT;          // Fournaise Antique - forge titanesque
            case 25 -> Material.YELLOW_DYE;               // Terres de Soufre - sol toxique jaune
            // Zone 26 est PvP, traitée en priorité
            case 27 -> Material.LAVA_BUCKET;              // Rivière de Lave - fleuve en fusion
            case 28 -> Material.SOUL_SAND;                // Canyon des Damnés - âmes perdues
            case 29 -> Material.IRON_BARS;                // Forteresse Effondrée - ruines militaires
            case 30 -> Material.TNT;                      // No Man's Land - terre dévastée

            // === ACTE IV - L'Extinction (Froid et mort) ===
            case 31 -> Material.SNOW_BLOCK;               // Toundra Morte - toundra gelée
            case 32 -> Material.POWDER_SNOW_BUCKET;       // Neiges Hurlantes - vent et neige
            case 33 -> Material.BLUE_ICE;                 // Plaines Gelées - glace éternelle
            case 34 -> Material.GLASS;                    // Lac de Verre - lac gelé transparent
            case 35 -> Material.PRISMARINE;               // Ruines Englouties - cité sous la glace
            case 36 -> Material.ICE;                      // Pics du Désespoir - montagnes glacées
            case 37 -> Material.PACKED_ICE;               // Blizzard Éternel - tempête de neige
            case 38 -> Material.SKELETON_SKULL;           // Tombe Blanche - cimetière enneigé
            case 39 -> Material.CANDLE;                   // Sanctuaire Abandonné - temple profané
            case 40 -> Material.SCULK;                    // Seuil de l'Oblivion - frontière oubli

            // === ACTE V - L'Origine du Mal (Corruption pure) ===
            case 41 -> Material.SCULK_VEIN;               // Terres Corrompues - corruption infiltrée
            case 42 -> Material.WITHER_ROSE;              // Forêt Noire - lumière interdite
            case 43 -> Material.SCULK_CATALYST;           // Racines du Mal - racines infection
            case 44 -> Material.CRIMSON_FUNGUS;           // Marécages Carmine - eau rouge sang
            case 45 -> Material.HEART_OF_THE_SEA;         // Veines du Monde - tunnels organiques
            case 46 -> Material.REINFORCED_DEEPSLATE;     // Citadelle Profanée - temple souillé
            case 47 -> Material.NETHER_WART;              // Cœur Putride - cœur de l'infection
            case 48 -> Material.CREEPER_HEAD;             // Trône des Infectés - siège du pouvoir
            case 49 -> Material.END_STONE;                // Dernier Rempart - dernière barrière
            // Zone 50 est boss, traitée en priorité

            default -> getMaterialByEnvironment(zone);
        };
    }

    /**
     * Fallback pour les zones non mappées - basé sur l'environnement
     */
    private Material getMaterialByEnvironment(Zone zone) {
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
