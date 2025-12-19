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
 * Menu détaillé d'une zone spécifique
 * Affiche toutes les informations: difficulté, loot, multiplicateurs, etc.
 */
public class ZoneDetailGUI implements InventoryHolder {

    private static final int SIZE = 54; // 6 lignes

    // Slots des différentes sections
    private static final int SLOT_ZONE_INFO = 4;      // Info principale en haut
    private static final int SLOT_DIFFICULTY = 19;    // Difficulté
    private static final int SLOT_MULTIPLIERS = 21;   // Multiplicateurs
    private static final int SLOT_LOOT = 23;          // Taux de loot
    private static final int SLOT_ITEMS = 25;         // Score items
    private static final int SLOT_ZOMBIES = 29;       // Types de zombies
    private static final int SLOT_ENVIRONMENT = 31;   // Effets environnementaux
    private static final int SLOT_FLAGS = 33;         // Flags spéciaux
    private static final int SLOT_PLAYERS = 40;       // Joueurs dans la zone

    // Navigation
    private static final int SLOT_BACK = 45;          // Retour
    private static final int SLOT_PREV_ZONE = 48;     // Zone précédente
    private static final int SLOT_TELEPORT = 49;      // Téléportation (admin)
    private static final int SLOT_NEXT_ZONE = 50;     // Zone suivante

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Zone zone;
    private final Inventory inventory;

    public ZoneDetailGUI(ZombieZPlugin plugin, Player player, Zone zone) {
        this.plugin = plugin;
        this.player = player;
        this.zone = zone;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8§l🗺 " + zone.getDisplayName());

        setupGUI();
    }

    private void setupGUI() {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int maxZoneReached = data != null ? data.getMaxZone().get() : 1;
        boolean unlocked = zone.getId() <= maxZoneReached || zone.getId() == 0;
        boolean isAdmin = player.hasPermission("zombiez.admin");

        // Bordure du haut (couleur de la zone)
        Material headerMaterial = getZoneColorMaterial();
        ItemStack headerGlass = ItemBuilder.placeholder(headerMaterial);
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, headerGlass);
        }

        // Bordures latérales grises
        ItemStack sideGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        int[] sideSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideSlots) {
            inventory.setItem(slot, sideGlass);
        }

        // Bordure du bas
        ItemStack footerGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, footerGlass);
        }

        // Remplir le centre avec du verre
        ItemStack centerGlass = ItemBuilder.placeholder(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = 10; i < 45; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, centerGlass);
            }
        }

        // === SECTION PRINCIPALE ===
        inventory.setItem(SLOT_ZONE_INFO, createMainInfoItem(unlocked));

        // === SECTIONS DÉTAILLÉES ===
        inventory.setItem(SLOT_DIFFICULTY, createDifficultyItem());
        inventory.setItem(SLOT_MULTIPLIERS, createMultipliersItem());
        inventory.setItem(SLOT_LOOT, createLootItem());
        inventory.setItem(SLOT_ITEMS, createItemScoreItem());
        inventory.setItem(SLOT_ZOMBIES, createZombiesItem());
        inventory.setItem(SLOT_ENVIRONMENT, createEnvironmentItem());
        inventory.setItem(SLOT_FLAGS, createFlagsItem());
        inventory.setItem(SLOT_PLAYERS, createPlayersItem());

        // === NAVIGATION ===
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c← Retour au Wiki")
            .lore(
                "",
                "§7Retourner à la liste des zones",
                "",
                "§eCliquez pour revenir"
            )
            .build());

        // Zone précédente
        Zone prevZone = plugin.getZoneManager().getZoneById(zone.getId() - 1);
        if (prevZone != null && prevZone.getId() > 0) {
            inventory.setItem(SLOT_PREV_ZONE, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§e◀ " + prevZone.getColoredName())
                .lore(
                    "",
                    "§7Zone précédente",
                    "",
                    "§eCliquez pour voir"
                )
                .build());
        }

        // Zone suivante
        Zone nextZone = plugin.getZoneManager().getZoneById(zone.getId() + 1);
        if (nextZone != null) {
            inventory.setItem(SLOT_NEXT_ZONE, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§e" + nextZone.getColoredName() + " ▶")
                .lore(
                    "",
                    "§7Zone suivante",
                    "",
                    "§eCliquez pour voir"
                )
                .build());
        }

        // Téléportation (admin uniquement)
        if (isAdmin) {
            inventory.setItem(SLOT_TELEPORT, new ItemBuilder(Material.ENDER_PEARL)
                .name("§6⚡ Téléportation Admin")
                .lore(
                    "",
                    "§7Se téléporter au centre",
                    "§7de cette zone.",
                    "",
                    "§7Position: §fZ " + ((zone.getMinZ() + zone.getMaxZ()) / 2),
                    "",
                    "§c⚠ Admin uniquement",
                    "",
                    "§eCliquez pour téléporter"
                )
                .glow(true)
                .build());
        } else {
            inventory.setItem(SLOT_TELEPORT, new ItemBuilder(Material.BARRIER)
                .name("§c⚡ Téléportation")
                .lore(
                    "",
                    "§7La téléportation n'est",
                    "§7disponible que pour les",
                    "§7administrateurs.",
                    "",
                    "§7Explorez le monde pour",
                    "§7atteindre cette zone!"
                )
                .build());
        }
    }

    /**
     * Crée l'item d'information principale
     */
    private ItemStack createMainInfoItem(boolean unlocked) {
        Material material = zone.isBossZone() ? Material.DRAGON_HEAD :
                          zone.isPvpEnabled() ? Material.IRON_SWORD :
                          zone.isSafeZone() ? Material.EMERALD : Material.FILLED_MAP;

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (zone.getDescription() != null && !zone.getDescription().isEmpty()) {
            lore.add("§7§o\"" + zone.getDescription() + "\"");
            lore.add("");
        }

        // Acte
        int act = getActForZone(zone.getId());
        String actName = getActName(act);
        lore.add("§7Acte: §f" + actName);
        lore.add("§7Zone: §f#" + zone.getId() + "/50");
        lore.add("");

        // Position
        lore.add("§7Coordonnées Z:");
        lore.add("  §7Début: §fZ=" + zone.getMaxZ());
        lore.add("  §7Fin: §fZ=" + zone.getMinZ());
        lore.add("  §7Taille: §f" + (zone.getMaxZ() - zone.getMinZ()) + " blocs");
        lore.add("");

        // Biome
        lore.add("§7Biome: §f" + formatBiome(zone.getBiomeType()));
        lore.add("§7Thème: §f" + formatTheme(zone.getTheme()));

        if (!unlocked) {
            lore.add("");
            lore.add("§c§l✖ ZONE NON DÉCOUVERTE");
            lore.add("§7Progressez pour débloquer!");
        }

        return new ItemBuilder(material)
            .name(zone.getColoredName())
            .lore(lore)
            .glow(zone.isBossZone())
            .build();
    }

    /**
     * Crée l'item de difficulté
     */
    private ItemStack createDifficultyItem() {
        Material material = switch (zone.getDifficulty()) {
            case 1, 2 -> Material.WOODEN_SWORD;
            case 3, 4 -> Material.STONE_SWORD;
            case 5, 6 -> Material.IRON_SWORD;
            case 7, 8 -> Material.DIAMOND_SWORD;
            case 9, 10 -> Material.NETHERITE_SWORD;
            default -> Material.WOODEN_SWORD;
        };

        String difficultyText = switch (zone.getDifficulty()) {
            case 1, 2 -> "§a§lFACILE";
            case 3, 4 -> "§e§lMOYEN";
            case 5, 6 -> "§6§lDIFFICILE";
            case 7, 8 -> "§c§lTRÈS DIFFICILE";
            case 9, 10 -> "§4§lEXTRÊME";
            default -> "§f§lINCONNU";
        };

        return new ItemBuilder(material)
            .name("§c⚔ Difficulté")
            .lore(
                "",
                "§7Niveau: " + difficultyText,
                "",
                "  " + zone.getStarsDisplay(),
                "",
                "§7Difficulté numérique: §c" + zone.getDifficulty() + "/10",
                "",
                "§7Cette zone est classée",
                "§7en fonction de la force",
                "§7des zombies et des",
                "§7dangers environnementaux."
            )
            .hideAttributes()
            .build();
    }

    /**
     * Crée l'item des multiplicateurs
     */
    private ItemStack createMultipliersItem() {
        int xpBonus = (int) ((zone.getXpMultiplier() - 1) * 100);
        int lootBonus = (int) ((zone.getLootMultiplier() - 1) * 100);
        int spawnRate = (int) (zone.getSpawnRateMultiplier() * 100);
        int healthBonus = (int) ((zone.getZombieHealthMultiplier() - 1) * 100);
        int damageBonus = (int) ((zone.getZombieDamageMultiplier() - 1) * 100);
        int speedBonus = (int) ((zone.getZombieSpeedMultiplier() - 1) * 100);

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§6✦ Multiplicateurs")
            .lore(
                "",
                "§a§l▸ BONUS JOUEUR",
                "  §7XP gagné: §a+" + xpBonus + "%",
                "  §7Chance de loot: §e+" + lootBonus + "%",
                "",
                "§c§l▸ FORCE ZOMBIES",
                "  §7Vie: §c+" + healthBonus + "%",
                "  §7Dégâts: §c+" + damageBonus + "%",
                "  §7Vitesse: §c+" + speedBonus + "%",
                "",
                "§e§l▸ SPAWN",
                "  §7Taux de spawn: §f" + spawnRate + "%",
                "",
                "§7Les multiplicateurs augmentent",
                "§7au fur et à mesure que vous",
                "§7progressez dans les zones."
            )
            .build();
    }

    /**
     * Crée l'item des taux de loot
     */
    private ItemStack createLootItem() {
        int tier = getZombieTier(zone.getId());

        String[] tierInfo = switch (tier) {
            case 1 -> new String[]{"Tier 1", "15%", "0", "Commun", "Peu commun"};
            case 2 -> new String[]{"Tier 2", "25%", "0", "Commun", "Rare"};
            case 3 -> new String[]{"Tier 3", "18%", "0", "Peu commun", "Épique"};
            case 4 -> new String[]{"Tier 4", "22%", "0", "Rare", "Légendaire"};
            case 5 -> new String[]{"Tier 5", "30%", "0", "Rare", "Mythique"};
            default -> new String[]{"Tier 1", "15%", "0", "Commun", "Peu commun"};
        };

        return new ItemBuilder(Material.CHEST)
            .name("§e📦 Taux de Loot")
            .lore(
                "",
                "§7Table de loot: §f" + tierInfo[0],
                "",
                "§6§l▸ ZOMBIES NORMAUX",
                "  §7Chance de drop: §e" + tierInfo[1],
                "  §7Drops garantis: §f" + tierInfo[2],
                "  §7Raretés: §f" + tierInfo[3] + " → " + tierInfo[4],
                "",
                "§5§l▸ MINI-BOSS",
                "  §7Chance de drop: §e100%",
                "  §7Drops garantis: §f2",
                "  §7Raretés: §fRare → Légendaire",
                "",
                "§c§l▸ BOSS DE ZONE",
                "  §7Chance de drop: §e100%",
                "  §7Drops garantis: §f3",
                "  §7Raretés: §fÉpique → Mythique",
                "",
                "§7Le multiplicateur de loot de",
                "§7la zone améliore vos chances!"
            )
            .build();
    }

    /**
     * Crée l'item de score des items
     */
    private ItemStack createItemScoreItem() {
        int[] scoreRange = getEstimatedScoreRange(zone.getId());

        String commonColor = getScoreColor(scoreRange[0]);
        String uncommonColor = getScoreColor((int)(scoreRange[0] * 1.3));
        String rareColor = getScoreColor((int)(scoreRange[0] * 1.8));
        String epicColor = getScoreColor(scoreRange[2]);
        String legendaryColor = getScoreColor(scoreRange[3]);

        return new ItemBuilder(Material.DIAMOND)
            .name("§d💎 Score des Items")
            .lore(
                "",
                "§7Fourchette de score des items",
                "§7qui peuvent drop dans cette zone:",
                "",
                "§f§l▸ COMMUN",
                "  §7Score: " + commonColor + formatScore(scoreRange[0]) + " §7- " + commonColor + formatScore(scoreRange[1]),
                "",
                "§a§l▸ PEU COMMUN",
                "  §7Score: " + uncommonColor + formatScore((int)(scoreRange[0] * 1.2)) + " §7- " + uncommonColor + formatScore((int)(scoreRange[1] * 1.3)),
                "",
                "§9§l▸ RARE",
                "  §7Score: " + rareColor + formatScore((int)(scoreRange[0] * 1.6)) + " §7- " + rareColor + formatScore((int)(scoreRange[1] * 1.8)),
                "",
                "§5§l▸ ÉPIQUE",
                "  §7Score: " + epicColor + formatScore((int)(scoreRange[2] * 0.8)) + " §7- " + epicColor + formatScore((int)(scoreRange[2] * 1.2)),
                "",
                "§6§l▸ LÉGENDAIRE+",
                "  §7Score: " + legendaryColor + formatScore(scoreRange[2]) + " §7- " + legendaryColor + formatScore(scoreRange[3]),
                "",
                "§7Plus le score est élevé,",
                "§7plus l'item est puissant!"
            )
            .build();
    }

    /**
     * Crée l'item des types de zombies
     */
    private ItemStack createZombiesItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Niveaux des zombies:");
        lore.add("  §cMin: §f" + zone.getMinZombieLevel());
        lore.add("  §cMax: §f" + zone.getMaxZombieLevel());
        lore.add("");
        lore.add("§c§l▸ TYPES DE ZOMBIES");

        String[] zombieTypes = zone.getAllowedZombieTypes();
        if (zombieTypes != null && zombieTypes.length > 0) {
            for (String type : zombieTypes) {
                String formatted = formatZombieType(type);
                lore.add("  §7• " + formatted);
            }
        } else {
            lore.add("  §8Aucun type spécifique");
        }

        lore.add("");
        lore.add("§7Les zombies de cette zone");
        lore.add("§7sont adaptés au thème et");
        lore.add("§7à la difficulté locale.");

        return new ItemBuilder(Material.ZOMBIE_HEAD)
            .name("§c🧟 Zombies")
            .lore(lore)
            .build();
    }

    /**
     * Crée l'item d'environnement
     */
    private ItemStack createEnvironmentItem() {
        Material material;
        String effectName;
        String effectDesc;

        switch (zone.getEnvironmentalEffect()) {
            case "HEAT" -> {
                material = Material.BLAZE_POWDER;
                effectName = "§6Chaleur extrême";
                effectDesc = "La chaleur intense vous brûle lentement.";
            }
            case "COLD" -> {
                material = Material.BLUE_ICE;
                effectName = "§bFroid glacial";
                effectDesc = "Le froid vous gèle jusqu'aux os.";
            }
            case "TOXIC" -> {
                material = Material.SLIME_BALL;
                effectName = "§aZone toxique";
                effectDesc = "L'air est empoisonné.";
            }
            case "RADIATION" -> {
                material = Material.GLOWSTONE_DUST;
                effectName = "§eRadiations";
                effectDesc = "Les radiations vous contaminent.";
            }
            case "FIRE" -> {
                material = Material.FIRE_CHARGE;
                effectName = "§cZone enflammée";
                effectDesc = "Les flammes sont partout.";
            }
            case "DARKNESS" -> {
                material = Material.SCULK;
                effectName = "§8Ténèbres profondes";
                effectDesc = "L'obscurité consume votre âme.";
            }
            default -> {
                material = Material.FEATHER;
                effectName = "§aAucun";
                effectDesc = "Pas d'effet environnemental.";
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Effet actif: " + effectName);
        lore.add("");

        if (zone.isDangerous()) {
            lore.add("§c§l⚠ ZONE DANGEREUSE");
            lore.add("");
            lore.add("§7" + effectDesc);
            lore.add("");
            lore.add("§7Dégâts: §c" + zone.getEnvironmentalDamage() + " §7par seconde");
            lore.add("§7Intervalle: §f" + (zone.getEnvironmentalInterval() / 20.0) + "s");
            lore.add("");
            lore.add("§e⚡ CONSEIL");
            lore.add("§7Équipez-vous d'items avec");
            lore.add("§7résistance aux effets pour");
            lore.add("§7survivre plus longtemps!");
        } else {
            lore.add("§a✓ §7Zone sans danger environnemental");
            lore.add("");
            lore.add("§7Vous ne subirez pas de dégâts");
            lore.add("§7environnementaux dans cette zone.");
        }

        return new ItemBuilder(material)
            .name("§e🌍 Environnement")
            .lore(lore)
            .build();
    }

    /**
     * Crée l'item des flags
     */
    private ItemStack createFlagsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        boolean hasFlags = zone.isPvpEnabled() || zone.isSafeZone() || zone.isBossZone() || zone.getRefugeId() > 0;

        if (!hasFlags) {
            lore.add("§7Aucun flag spécial.");
            lore.add("");
            lore.add("§7Cette zone est une zone");
            lore.add("§7standard de progression.");
        } else {
            lore.add("§e§l▸ FLAGS ACTIFS");
            lore.add("");

            if (zone.isSafeZone()) {
                lore.add("§a🛡 ZONE SÉCURISÉE");
                lore.add("  §7• Pas de spawn de zombies");
                lore.add("  §7• Zone de repos");
                lore.add("");
            }

            if (zone.isPvpEnabled()) {
                lore.add("§c⚔ PVP ACTIVÉ");
                lore.add("  §7• Combat joueur vs joueur");
                lore.add("  §7• Attention aux autres!");
                lore.add("");
            }

            if (zone.isBossZone()) {
                lore.add("§5👑 ZONE DE BOSS");
                lore.add("  §7• Boss final présent");
                lore.add("  §7• Loot exceptionnel");
                lore.add("");
            }

            if (zone.getRefugeId() > 0) {
                lore.add("§a✓ REFUGE #" + zone.getRefugeId());
                lore.add("  §7• Point de respawn");
                lore.add("  §7• Zone de commerce");
                lore.add("");
            }
        }

        return new ItemBuilder(Material.OAK_SIGN)
            .name("§6🏴 Flags Spéciaux")
            .lore(lore)
            .build();
    }

    /**
     * Crée l'item des joueurs présents
     */
    private ItemStack createPlayersItem() {
        int playersCount = plugin.getZoneManager().getPlayersInZone(zone.getId());
        List<Player> players = plugin.getZoneManager().getPlayersInZone(zone);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Joueurs actuellement dans");
        lore.add("§7cette zone: §e" + playersCount);
        lore.add("");

        if (playersCount > 0) {
            lore.add("§e§l▸ LISTE DES JOUEURS");
            int shown = 0;
            for (Player p : players) {
                if (shown >= 10) {
                    lore.add("  §7... et " + (playersCount - 10) + " autres");
                    break;
                }
                lore.add("  §7• §f" + p.getName());
                shown++;
            }
        } else {
            lore.add("§8Aucun joueur présent");
        }

        return new ItemBuilder(playersCount > 0 ? Material.PLAYER_HEAD : Material.SKELETON_SKULL)
            .name("§b👥 Joueurs en Zone")
            .lore(lore)
            .build();
    }

    // === MÉTHODES UTILITAIRES ===

    private Material getZoneColorMaterial() {
        String color = zone.getColor();
        return switch (color) {
            case "§a" -> Material.LIME_STAINED_GLASS_PANE;
            case "§2" -> Material.GREEN_STAINED_GLASS_PANE;
            case "§e" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "§6" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "§c" -> Material.RED_STAINED_GLASS_PANE;
            case "§4" -> Material.RED_STAINED_GLASS_PANE;
            case "§b" -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "§3" -> Material.CYAN_STAINED_GLASS_PANE;
            case "§5" -> Material.PURPLE_STAINED_GLASS_PANE;
            case "§d" -> Material.MAGENTA_STAINED_GLASS_PANE;
            case "§0", "§8" -> Material.BLACK_STAINED_GLASS_PANE;
            case "§7" -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            default -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    private int getActForZone(int zoneId) {
        if (zoneId <= 10) return 1;
        if (zoneId <= 20) return 2;
        if (zoneId <= 30) return 3;
        if (zoneId <= 40) return 4;
        return 5;
    }

    private String getActName(int act) {
        return switch (act) {
            case 1 -> "I - Les Derniers Jours";
            case 2 -> "II - La Contamination";
            case 3 -> "III - Le Chaos";
            case 4 -> "IV - L'Extinction";
            case 5 -> "V - L'Origine du Mal";
            default -> "Inconnu";
        };
    }

    private String formatBiome(String biome) {
        if (biome == null) return "Inconnu";
        return biome.replace("_", " ").toLowerCase();
    }

    private String formatTheme(String theme) {
        if (theme == null) return "Inconnu";
        return theme.replace("_", " ");
    }

    private int getZombieTier(int zoneId) {
        if (zoneId <= 10) return 1;
        if (zoneId <= 20) return 2;
        if (zoneId <= 30) return 3;
        if (zoneId <= 40) return 4;
        return 5;
    }

    private int[] getEstimatedScoreRange(int zoneId) {
        int baseScore = 200 + (zoneId * 80);
        int commonMin = (int) (baseScore * 0.8);
        int commonMax = (int) (baseScore * 1.2);
        int legendaryMin = (int) (baseScore * 2.5);
        int legendaryMax = (int) (baseScore * 4.0);
        return new int[]{commonMin, commonMax, legendaryMin, legendaryMax};
    }

    private String getScoreColor(int score) {
        if (score >= 20000) return "§c§l";
        if (score >= 12000) return "§d§l";
        if (score >= 7000) return "§6§l";
        if (score >= 4000) return "§5";
        if (score >= 2000) return "§9";
        if (score >= 800) return "§a";
        if (score >= 300) return "§f";
        return "§7";
    }

    private String formatScore(int score) {
        if (score < 1000) return String.valueOf(score);
        if (score < 1000000) return String.format("%.1fK", score / 1000.0);
        return String.format("%.1fM", score / 1000000.0);
    }

    private String formatZombieType(String type) {
        return switch (type) {
            case "WALKER" -> "§fWalker §7- Zombie basique";
            case "SHAMBLER" -> "§fShambler §7- Lent mais résistant";
            case "CRAWLER" -> "§eCrawler §7- Rampe et surprend";
            case "RUNNER" -> "§eRunner §7- Rapide et agile";
            case "ARMORED" -> "§6Armored §7- Blindé";
            case "ARMORED_ELITE" -> "§6Armored Elite §7- Très blindé";
            case "SPITTER" -> "§aSpitter §7- Crache de l'acide";
            case "SCREAMER" -> "§dScreamer §7- Alerte les autres";
            case "BLOATER" -> "§2Bloater §7- Explose au contact";
            case "TOXIC" -> "§aToxic §7- Aura empoisonnée";
            case "BERSERKER" -> "§cBerserker §7- Rage frénétique";
            case "LURKER" -> "§8Lurker §7- Invisible et mortel";
            case "SHADOW" -> "§8Shadow §7- Ombre furtive";
            case "NECROMANCER" -> "§5Necromancer §7- Réanime les morts";
            case "SPECTRE" -> "§bSpectre §7- Fantomatique";
            case "GIANT" -> "§4Giant §7- Énorme et puissant";
            case "COLOSSUS" -> "§4Colossus §7- Titan dévastateur";
            case "RAVAGER" -> "§cRavager §7- Destructeur";
            case "EXPLOSIVE" -> "§6Explosive §7- Boom!";
            case "DEMON" -> "§4Demon §7- Créature infernale";
            case "INFERNAL" -> "§cInfernal §7- Flammes éternelles";
            case "MUTANT" -> "§5Mutant §7- Muté par les radiations";
            case "FROZEN" -> "§bFrozen §7- Gèle ses victimes";
            case "YETI" -> "§fYeti §7- Bête des neiges";
            case "WENDIGO" -> "§8Wendigo §7- Horreur du froid";
            case "DROWNER" -> "§3Drowner §7- Attaque depuis l'eau";
            case "CLIMBER" -> "§eClimber §7- Escalade tout";
            case "CREAKING" -> "§2Creaking §7- Bois animé";
            case "CORRUPTED_WARDEN" -> "§5Corrupted Warden §7- Gardien corrompu";
            case "ARCHON" -> "§d§lArchon §7- Elite suprême";
            case "PATIENT_ZERO" -> "§c§lPatient Zéro §7- L'ORIGINE";
            default -> "§7" + type;
        };
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Zone getZone() {
        return zone;
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
            if (!(event.getInventory().getHolder() instanceof ZoneDetailGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            switch (slot) {
                case SLOT_BACK -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new ZoneWikiGUI(gui.getPlugin(), player).open();
                }
                case SLOT_PREV_ZONE -> {
                    Zone prevZone = gui.getPlugin().getZoneManager().getZoneById(gui.getZone().getId() - 1);
                    if (prevZone != null && prevZone.getId() > 0) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new ZoneDetailGUI(gui.getPlugin(), player, prevZone).open();
                    }
                }
                case SLOT_NEXT_ZONE -> {
                    Zone nextZone = gui.getPlugin().getZoneManager().getZoneById(gui.getZone().getId() + 1);
                    if (nextZone != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new ZoneDetailGUI(gui.getPlugin(), player, nextZone).open();
                    }
                }
                case SLOT_TELEPORT -> {
                    if (player.hasPermission("zombiez.admin")) {
                        teleportToZone(player, gui.getZone());
                    } else {
                        player.sendMessage("§c[Wiki] Vous n'avez pas la permission de vous téléporter!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }
        }

        private void teleportToZone(Player player, Zone zone) {
            int centerZ = (zone.getMinZ() + zone.getMaxZ()) / 2;
            int centerX = 621;

            var world = Bukkit.getWorld(plugin.getZoneManager().getGameWorld());
            if (world == null) {
                player.sendMessage("§c[Wiki] Erreur: Monde non trouvé!");
                return;
            }

            int y = world.getHighestBlockYAt(centerX, centerZ) + 1;

            org.bukkit.Location loc = new org.bukkit.Location(world, centerX + 0.5, y, centerZ + 0.5);
            player.teleport(loc);
            player.closeInventory();

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage("");
            player.sendMessage("§a[Wiki] §7Téléporté vers " + zone.getColoredName());
            player.sendMessage("§7Position: §fX:" + centerX + " Y:" + y + " Z:" + centerZ);
            player.sendMessage("");
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof ZoneDetailGUI) {
                event.setCancelled(true);
            }
        }
    }
}
