package com.rinaorc.zombiez.zones.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
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

/**
 * GUI de details d'une zone specifique
 * Affiche toutes les informations detaillees sur la zone
 */
public class ZoneDetailGUI implements InventoryHolder {

    private static final int SIZE = 45;

    // Slots pour les differentes informations
    private static final int SLOT_TITLE = 4;
    private static final int SLOT_DIFFICULTY = 10;
    private static final int SLOT_ZOMBIES = 12;
    private static final int SLOT_LOOT = 14;
    private static final int SLOT_ENVIRONMENT = 16;
    private static final int SLOT_MULTIPLIERS = 20;
    private static final int SLOT_ITEM_SCORE = 22;
    private static final int SLOT_SPECIAL = 24;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_TELEPORT = 40;
    private static final int SLOT_NEXT_ZONE = 44;
    private static final int SLOT_PREV_ZONE = 38;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Zone zone;
    private final Inventory inventory;
    private final int returnPage;
    private final int returnFilter;

    public ZoneDetailGUI(ZombieZPlugin plugin, Player player, Zone zone, int returnPage, int returnFilter) {
        this.plugin = plugin;
        this.player = player;
        this.zone = zone;
        this.returnPage = returnPage;
        this.returnFilter = returnFilter;

        String title = "§8§l\uD83D\uDCCB " + zone.getColor() + zone.getDisplayName();
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        setupGUI();
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure coloree selon la zone
        ItemStack border = ItemBuilder.placeholder(getBorderMaterial());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);
        inventory.setItem(27, border);
        inventory.setItem(35, border);

        // Titre de la zone
        inventory.setItem(SLOT_TITLE, createTitleItem());

        // Informations detaillees
        inventory.setItem(SLOT_DIFFICULTY, createDifficultyItem());
        inventory.setItem(SLOT_ZOMBIES, createZombiesItem());
        inventory.setItem(SLOT_LOOT, createLootItem());
        inventory.setItem(SLOT_ENVIRONMENT, createEnvironmentItem());
        inventory.setItem(SLOT_MULTIPLIERS, createMultipliersItem());
        inventory.setItem(SLOT_ITEM_SCORE, createItemScoreItem());
        inventory.setItem(SLOT_SPECIAL, createSpecialItem());

        // Navigation
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§e◄ Retour au Wiki")
            .lore("", "§7Retourner a la liste des zones")
            .build());

        // Navigation entre zones
        Zone prevZone = zone.getId() > 0 ? plugin.getZoneManager().getZoneById(zone.getId() - 1) : null;
        Zone nextZone = plugin.getZoneManager().getZoneById(zone.getId() + 1);

        if (prevZone != null) {
            inventory.setItem(SLOT_PREV_ZONE, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§e◄ Zone precedente")
                .lore("", prevZone.getColor() + prevZone.getDisplayName())
                .build());
        }

        if (nextZone != null) {
            inventory.setItem(SLOT_NEXT_ZONE, new ItemBuilder(Material.SPECTRAL_ARROW)
                .name("§eZone suivante ►")
                .lore("", nextZone.getColor() + nextZone.getDisplayName())
                .build());
        }

        // Teleportation (admin seulement)
        if (player.hasPermission("zombiez.admin")) {
            inventory.setItem(SLOT_TELEPORT, new ItemBuilder(Material.ENDER_PEARL)
                .name("§d\uD83D\uDCCD Se teleporter")
                .lore(
                    "",
                    "§7Teleportation vers cette zone",
                    "",
                    "§7Position: §eZ=" + (zone.getMaxZ() - 10),
                    "",
                    "§c[Admin] §eCliquez pour teleporter"
                )
                .glow()
                .build());
        } else {
            inventory.setItem(SLOT_TELEPORT, new ItemBuilder(Material.BARRIER)
                .name("§c\uD83D\uDD12 Teleportation")
                .lore(
                    "",
                    "§7Reservee aux administrateurs",
                    "",
                    "§7Les joueurs doivent progresser",
                    "§7naturellement dans les zones!"
                )
                .build());
        }
    }

    private ItemStack createTitleItem() {
        int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7§o\"" + zone.getDescription() + "\"");
        lore.add("");
        lore.add("§7═══════════════════════════════");
        lore.add("");
        lore.add("§7Zone ID: §e#" + zone.getId());
        lore.add("§7Acte: §e" + getActName());
        lore.add("§7Coordonnees: §fZ " + zone.getMaxZ() + " §8→ §fZ " + zone.getMinZ());
        lore.add("§7Theme: §f" + formatTheme(zone.getTheme()));
        lore.add("§7Biome: §f" + translateBiome(zone.getBiomeType()));

        if (playersInZone > 0) {
            lore.add("");
            lore.add("§7Joueurs presents: §a" + playersInZone);
        }

        String prefix = "";
        if (zone.isSafeZone()) prefix = "§a♥ ";
        else if (zone.isPvpEnabled()) prefix = "§4☠ ";
        else if (zone.isBossZone()) prefix = "§6👑 ";

        return new ItemBuilder(getTitleMaterial())
            .name(prefix + zone.getColor() + "§l" + zone.getDisplayName().toUpperCase())
            .lore(lore)
            .glow(zone.isBossZone())
            .hideAttributes()
            .build();
    }

    private String formatTheme(String theme) {
        if (theme == null || theme.isEmpty()) return "Standard";
        return theme.replace("_", " ").substring(0, 1).toUpperCase() +
               theme.replace("_", " ").substring(1).toLowerCase();
    }

    private String translateBiome(String biome) {
        if (biome == null) return "Inconnu";
        return switch (biome.toUpperCase()) {
            case "PLAINS" -> "Plaines";
            case "FOREST" -> "Foret";
            case "DARK_FOREST" -> "Foret Sombre";
            case "SWAMP" -> "Marais";
            case "DESERT" -> "Desert";
            case "TAIGA" -> "Taiga";
            case "SNOWY_TAIGA" -> "Taiga Enneigee";
            case "SNOWY_PLAINS" -> "Plaines Enneigees";
            case "BADLANDS" -> "Badlands";
            case "JUNGLE" -> "Jungle";
            case "NETHER_WASTES" -> "Enfer";
            case "BASALT_DELTAS" -> "Deltas de Basalte";
            case "CRIMSON_FOREST" -> "Foret Cramoisie";
            case "WARPED_FOREST" -> "Foret Biscornue";
            case "SOUL_SAND_VALLEY" -> "Vallee des Ames";
            case "DEEP_DARK" -> "Abimes Sombres";
            case "END_HIGHLANDS" -> "Hauts de l'End";
            default -> biome.replace("_", " ");
        };
    }

    private ItemStack createDifficultyItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Niveau de difficulte:");
        lore.add(zone.getStarsDisplay());
        lore.add("");
        lore.add("§7Difficulte numerique: §e" + zone.getDifficulty() + "§7/§c10");
        lore.add("");
        lore.add("§7Phase de jeu: §f" + ZoneScaling.getZoneDescription(zone.getId()));
        lore.add("");

        // Conseils selon la difficulte
        if (zone.getDifficulty() <= 3) {
            lore.add("§a✓ Zone accessible aux debutants");
        } else if (zone.getDifficulty() <= 6) {
            lore.add("§e⚠ Equipement moyen recommande");
        } else if (zone.getDifficulty() <= 8) {
            lore.add("§c⚠ Zone dangereuse - Prudence!");
        } else {
            lore.add("§4☠ Zone extreme - Experts uniquement!");
        }

        return new ItemBuilder(Material.GOLDEN_SWORD)
            .name("§e⚔ Difficulte")
            .lore(lore)
            .hideAttributes()
            .build();
    }

    private ItemStack createZombiesItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (zone.isSafeZone()) {
            lore.add("§a✓ Zone securisee");
            lore.add("§7Aucun zombie ne spawn ici");
        } else {
            lore.add("§7Niveaux: §c" + zone.getMinZombieLevel() + " §7- §c" + zone.getMaxZombieLevel());
            lore.add("");
            lore.add("§7═══ Types de Zombies ═══");

            String[] types = zone.getAllowedZombieTypes();
            if (types != null && types.length > 0) {
                for (String type : types) {
                    String formatted = formatZombieType(type);
                    lore.add("  §7• " + formatted);
                }
            }

            lore.add("");
            lore.add("§7Sante: §c" + formatMultiplier(zone.getZombieHealthMultiplier()));
            lore.add("§7Degats: §c" + formatMultiplier(zone.getZombieDamageMultiplier()));
            lore.add("§7Vitesse: §c" + formatMultiplier(zone.getZombieSpeedMultiplier()));
        }

        return new ItemBuilder(Material.ZOMBIE_HEAD)
            .name("§c\uD83E\uDDDF Types de Zombies")
            .lore(lore)
            .build();
    }

    private ItemStack createLootItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Multiplicateur de loot: §6" + formatMultiplier(zone.getLootMultiplier()));
        lore.add("");
        lore.add("§7═══ Taux de Drop Estimes ═══");

        // Calcul des taux approximatifs selon le multiplicateur
        double mult = zone.getLootMultiplier();
        lore.add("§f  Common: §7" + formatPercent(60 * mult));
        lore.add("§a  Uncommon: §7" + formatPercent(25 * mult));
        lore.add("§9  Rare: §7" + formatPercent(10 * mult));
        lore.add("§5  Epic: §7" + formatPercent(4 * mult));
        lore.add("§6  Legendary: §7" + formatPercent(0.9 * mult));
        lore.add("§d  Mythic: §7" + formatPercent(0.1 * mult));
        lore.add("§c§l  Exalted: §7" + formatPercent(0.01 * mult));

        lore.add("");
        lore.add("§7Plus le multiplicateur est eleve,");
        lore.add("§7plus les drops sont nombreux!");

        return new ItemBuilder(Material.CHEST)
            .name("§6\uD83C\uDF81 Loot & Recompenses")
            .lore(lore)
            .build();
    }

    private ItemStack createEnvironmentItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        String effect = zone.getEnvironmentalEffect();
        if (effect.equals("NONE")) {
            lore.add("§a✓ Aucun danger environnemental");
            lore.add("");
            lore.add("§7Cette zone ne possede pas");
            lore.add("§7d'effets environnementaux.");
        } else {
            lore.add("§c⚠ Zone dangereuse!");
            lore.add("");
            lore.add("§7Effet: " + getEnvironmentDisplay(effect));
            lore.add("§7Degats: §c" + zone.getEnvironmentalDamage() + " §7par seconde");
            lore.add("§7Intervalle: §e" + (zone.getEnvironmentalInterval() / 20.0) + "s");
            lore.add("");

            // Conseils selon l'effet
            switch (effect) {
                case "HEAT", "FIRE" -> {
                    lore.add("§c\uD83D\uDD25 Protection contre le feu recommandee");
                    lore.add("§7Potions de resistance au feu");
                }
                case "COLD" -> {
                    lore.add("§b❄ Protection contre le froid recommandee");
                    lore.add("§7Armure chaude ou potions");
                }
                case "TOXIC" -> {
                    lore.add("§2☠ Protection toxique recommandee");
                    lore.add("§7Masques ou potions d'antidote");
                }
                case "RADIATION" -> {
                    lore.add("§e☢ Protection anti-radiation recommandee");
                    lore.add("§7Equipement special requis");
                }
                case "DARKNESS" -> {
                    lore.add("§8\uD83C\uDF19 Vision nocturne recommandee");
                    lore.add("§7Torches et potions de vision");
                }
            }
        }

        Material mat = switch (effect) {
            case "HEAT", "FIRE" -> Material.FIRE_CHARGE;
            case "COLD" -> Material.SNOWBALL;
            case "TOXIC" -> Material.FERMENTED_SPIDER_EYE;
            case "RADIATION" -> Material.GLOWSTONE_DUST;
            case "DARKNESS" -> Material.INK_SAC;
            default -> Material.FEATHER;
        };

        return new ItemBuilder(mat)
            .name("§7\uD83C\uDF2B Environnement")
            .lore(lore)
            .build();
    }

    private ItemStack createMultipliersItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7═══ Bonus de Zone ═══");
        lore.add("");
        lore.add("§a✦ §7XP gagnee: §a" + formatMultiplier(zone.getXpMultiplier()));
        lore.add("§6✦ §7Loot: §6" + formatMultiplier(zone.getLootMultiplier()));
        lore.add("§c✦ §7Taux de spawn: §c" + formatMultiplier(zone.getSpawnRateMultiplier()));
        lore.add("");
        lore.add("§7═══ Stats Zombies ═══");
        lore.add("");
        lore.add("§c❤ §7Sante: §c" + formatMultiplier(zone.getZombieHealthMultiplier()));
        lore.add("§c⚔ §7Degats: §c" + formatMultiplier(zone.getZombieDamageMultiplier()));
        lore.add("§c\uD83C\uDFC3 §7Vitesse: §c" + formatMultiplier(zone.getZombieSpeedMultiplier()));
        lore.add("");
        lore.add("§7Les multiplicateurs augmentent");
        lore.add("§7avec la progression!");

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§b\uD83D\uDCC8 Multiplicateurs")
            .lore(lore)
            .build();
    }

    private ItemStack createItemScoreItem() {
        int baseScore = ZoneScaling.getBaseScoreForZone(zone.getId());
        double scoreMultiplier = ZoneScaling.getScoreMultiplier(zone.getId());

        // Estimation des scores par rarete
        int commonScore = (int) (baseScore * 0.5);
        int uncommonScore = (int) (baseScore * 0.75);
        int rareScore = baseScore;
        int epicScore = (int) (baseScore * 1.5);
        int legendaryScore = (int) (baseScore * 2.5);
        int mythicScore = (int) (baseScore * 4);
        int exaltedScore = (int) (baseScore * 6);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Score de base: §6" + formatNumber(baseScore));
        lore.add("§7Multiplicateur: §e" + formatMultiplier(scoreMultiplier));
        lore.add("");
        lore.add("§7═══ Scores par Rarete ═══");
        lore.add("§f  Common: §7~" + formatNumber(commonScore));
        lore.add("§a  Uncommon: §7~" + formatNumber(uncommonScore));
        lore.add("§9  Rare: §7~" + formatNumber(rareScore));
        lore.add("§5  Epic: §7~" + formatNumber(epicScore));
        lore.add("§6  Legendary: §7~" + formatNumber(legendaryScore));
        lore.add("§d  Mythic: §7~" + formatNumber(mythicScore));
        lore.add("§c§l  Exalted: §7~" + formatNumber(exaltedScore));
        lore.add("");
        lore.add("§7Le score represente la puissance");
        lore.add("§7globale d'un item.");

        return new ItemBuilder(Material.NETHER_STAR)
            .name("§6\uD83C\uDFC6 Item Score")
            .lore(lore)
            .glow()
            .build();
    }

    private ItemStack createSpecialItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        boolean hasSpecial = zone.isSafeZone() || zone.isPvpEnabled() || zone.isBossZone() || zone.getRefugeId() > 0;

        if (!hasSpecial) {
            lore.add("§7Aucune caracteristique speciale");
            return new ItemBuilder(Material.GRAY_DYE)
                .name("§8✧ Caracteristiques Speciales")
                .lore(lore)
                .build();
        }

        if (zone.isSafeZone()) {
            lore.add("§a♥ ZONE SECURISEE");
            lore.add("§7  - Pas de spawn de zombies");
            lore.add("§7  - PvP desactive");
            lore.add("§7  - Point de depart");
            lore.add("");
        }

        if (zone.isPvpEnabled()) {
            lore.add("§4☠ ZONE PVP");
            lore.add("§7  - Combats entre joueurs actifs");
            lore.add("§7  - Pas de refuge ici");
            lore.add("§7  - Seuls les forts survivent!");
            lore.add("");
        }

        if (zone.isBossZone()) {
            lore.add("§6\uD83D\uDC51 ZONE DE BOSS");
            lore.add("§7  - Patient Zero vous attend");
            lore.add("§7  - Combat final ultime");
            lore.add("§7  - Recompenses legendaires");
            lore.add("");
        }

        if (zone.getRefugeId() > 0) {
            lore.add("§e\uD83C\uDFE0 REFUGE #" + zone.getRefugeId());
            lore.add("§7  - Point de sauvegarde");
            lore.add("§7  - Vendeurs disponibles");
            lore.add("§7  - Zone de repos");
            lore.add("");
        }

        Material mat = Material.BEACON;
        if (zone.isPvpEnabled()) mat = Material.IRON_SWORD;
        else if (zone.isBossZone()) mat = Material.DRAGON_HEAD;
        else if (zone.isSafeZone()) mat = Material.EMERALD_BLOCK;

        return new ItemBuilder(mat)
            .name("§d✧ Caracteristiques Speciales")
            .lore(lore)
            .glow()
            .hideAttributes()
            .build();
    }

    private Material getBorderMaterial() {
        if (zone.isSafeZone()) return Material.LIME_STAINED_GLASS_PANE;
        if (zone.isPvpEnabled()) return Material.RED_STAINED_GLASS_PANE;
        if (zone.isBossZone()) return Material.PURPLE_STAINED_GLASS_PANE;

        return switch (zone.getDifficulty()) {
            case 1, 2 -> Material.LIME_STAINED_GLASS_PANE;
            case 3, 4 -> Material.YELLOW_STAINED_GLASS_PANE;
            case 5, 6 -> Material.ORANGE_STAINED_GLASS_PANE;
            case 7, 8 -> Material.RED_STAINED_GLASS_PANE;
            case 9, 10 -> Material.PURPLE_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    private Material getTitleMaterial() {
        if (zone.isSafeZone()) return Material.EMERALD;
        if (zone.isBossZone()) return Material.NETHER_STAR;
        if (zone.isPvpEnabled()) return Material.IRON_SWORD;

        return switch (zone.getEnvironmentalEffect()) {
            case "HEAT", "FIRE" -> Material.MAGMA_CREAM;
            case "COLD" -> Material.PACKED_ICE;
            case "TOXIC" -> Material.SLIME_BALL;
            case "RADIATION" -> Material.GLOWSTONE_DUST;
            case "DARKNESS" -> Material.ECHO_SHARD;
            default -> Material.PAPER;
        };
    }

    private String getActName() {
        int act = ((zone.getId() - 1) / 10) + 1;
        if (zone.getId() == 0) act = 1;
        return switch (act) {
            case 1 -> "I - Les Derniers Jours";
            case 2 -> "II - La Contamination";
            case 3 -> "III - Le Chaos";
            case 4 -> "IV - L'Extinction";
            case 5 -> "V - L'Origine du Mal";
            default -> "???";
        };
    }

    private String formatZombieType(String type) {
        String color = switch (type.toUpperCase()) {
            case "WALKER", "SHAMBLER", "CRAWLER" -> "§7";
            case "RUNNER", "LURKER", "SHADOW" -> "§a";
            case "BLOATER", "SPITTER", "TOXIC", "DROWNER" -> "§2";
            case "ARMORED", "ARMORED_ELITE", "SCREAMER" -> "§e";
            case "BERSERKER", "EXPLOSIVE", "GIANT", "DEMON" -> "§c";
            case "NECROMANCER", "SPECTRE", "INFERNAL", "YETI", "WENDIGO", "FROZEN" -> "§5";
            case "MUTANT", "COLOSSUS", "RAVAGER", "CLIMBER" -> "§4";
            case "ARCHON", "CORRUPTED_WARDEN", "CREAKING", "PATIENT_ZERO" -> "§d";
            default -> "§7";
        };
        return color + type.replace("_", " ");
    }

    private String getEnvironmentDisplay(String effect) {
        return switch (effect) {
            case "HEAT" -> "§c\uD83D\uDD25 Chaleur Extreme";
            case "FIRE" -> "§4\uD83D\uDD25 Flammes";
            case "COLD" -> "§b❄ Froid Glacial";
            case "TOXIC" -> "§2☠ Gaz Toxique";
            case "RADIATION" -> "§e☢ Radiation";
            case "DARKNESS" -> "§8\uD83C\uDF19 Tenebres Profondes";
            default -> "§7Aucun";
        };
    }

    private String formatMultiplier(double value) {
        return String.format("x%.2f", value);
    }

    private String formatPercent(double value) {
        if (value >= 1) return String.format("%.1f%%", value);
        return String.format("%.2f%%", value);
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        }
        if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // Getters
    public Zone getZone() { return zone; }
    public int getReturnPage() { return returnPage; }
    public int getReturnFilter() { return returnFilter; }
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
            if (!(event.getInventory().getHolder() instanceof ZoneDetailGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Retour au wiki
            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneWikiGUI(gui.getPlugin(), player, gui.getReturnPage(), gui.getReturnFilter()).open();
                return;
            }

            // Teleportation (admin)
            if (slot == SLOT_TELEPORT) {
                if (player.hasPermission("zombiez.admin")) {
                    Zone zone = gui.getZone();

                    // Particules avant TP
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);

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
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    player.sendMessage("§c§l[ZombieZ] §cReserve aux administrateurs!");
                }
                return;
            }

            // Zone precedente
            if (slot == SLOT_PREV_ZONE) {
                Zone prevZone = gui.getZone().getId() > 0 ?
                    gui.getPlugin().getZoneManager().getZoneById(gui.getZone().getId() - 1) : null;
                if (prevZone != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    new ZoneDetailGUI(gui.getPlugin(), player, prevZone, gui.getReturnPage(), gui.getReturnFilter()).open();
                }
                return;
            }

            // Zone suivante
            if (slot == SLOT_NEXT_ZONE) {
                Zone nextZone = gui.getPlugin().getZoneManager().getZoneById(gui.getZone().getId() + 1);
                if (nextZone != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    new ZoneDetailGUI(gui.getPlugin(), player, nextZone, gui.getReturnPage(), gui.getReturnFilter()).open();
                }
                return;
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof ZoneDetailGUI) {
                event.setCancelled(true);
            }
        }
    }
}
