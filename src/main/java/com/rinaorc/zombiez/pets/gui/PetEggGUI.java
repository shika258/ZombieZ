package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.eggs.EggType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI pour ouvrir les oeufs de pet
 */
public class PetEggGUI implements InventoryHolder {

    private static final String TITLE = "§8§l🥚 Oeufs de Pet";
    private static final int SIZE = 45;

    // Slots pour chaque type d'oeuf
    private static final int[] EGG_SLOTS = {10, 12, 14, 16, 22};
    private static final int SLOT_BACK = 40;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;

    public PetEggGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        setupGUI();
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }

        // Afficher les oeufs
        EggType[] types = EggType.values();
        for (int i = 0; i < types.length && i < EGG_SLOTS.length; i++) {
            inventory.setItem(EGG_SLOTS[i], createEggItem(types[i]));
        }

        // Pity info
        inventory.setItem(31, createPityInfoItem());

        // Retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());
    }

    private ItemStack createEggItem(EggType type) {
        int count = petData != null ? petData.getEggCount(type) : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Quantité: " + (count > 0 ? "§a" + count : "§c0"));
        lore.add("");
        lore.add("§7Contenu possible:");

        // Afficher les taux de drop
        PetRarity[] rarities = PetRarity.values();
        double[] rates = type.getRarityRates();
        for (int i = 0; i < rarities.length; i++) {
            if (rates[i] > 0) {
                lore.add("  " + rarities[i].getColoredName() + "§7: §e" + rates[i] + "%");
            }
        }

        if (type.getMinimumRarity() != null) {
            lore.add("");
            lore.add("§a✓ Garanti: " + type.getMinimumRarity().getColoredName() + "§a minimum");
        }

        lore.add("");
        if (count > 0) {
            lore.add("§eCliquez pour ouvrir!");
            lore.add("§eShift+Clic: Ouvrir x10");
        } else {
            lore.add("§8Aucun oeuf disponible");
        }

        Material icon = count > 0 ? type.getIcon() : Material.GRAY_DYE;

        return new ItemBuilder(icon)
            .name(type.getColoredName() + (count > 0 ? " §7x" + count : ""))
            .lore(lore)
            .glow(count > 0)
            .build();
    }

    private ItemStack createPityInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Système de protection contre");
        lore.add("§7la malchance. Plus vous ouvrez");
        lore.add("§7d'oeufs sans obtenir de rareté,");
        lore.add("§7plus vos chances augmentent!");
        lore.add("");

        if (petData != null) {
            lore.add("§7═══ COMPTEURS ═══");
            lore.add("");

            int standardPity = petData.getPityCounter(EggType.STANDARD);
            lore.add("§fOeuf Standard:");
            lore.add("  §7Pity: §e" + standardPity + "§7/50 (Rare)");
            lore.add("  §7Pity: §e" + standardPity + "§7/100 (Épique)");
            lore.add("  §7Pity: §e" + standardPity + "§7/200 (Légendaire)");

            int elitePity = petData.getPityCounter(EggType.ELITE);
            lore.add("");
            lore.add("§dOeuf Élite:");
            lore.add("  §7Pity: §e" + elitePity + "§7/20 (Légendaire)");
        }

        return new ItemBuilder(Material.KNOWLEDGE_BOOK)
            .name("§6📖 Système Pity")
            .lore(lore)
            .build();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Gestionnaire d'événements
     */
    public static class GUIListener implements Listener {

        private final ZombieZPlugin plugin;

        public GUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof PetEggGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            // Chercher quel oeuf a été cliqué
            EggType[] types = EggType.values();
            for (int i = 0; i < types.length && i < EGG_SLOTS.length; i++) {
                if (slot == EGG_SLOTS[i]) {
                    EggType eggType = types[i];
                    int count = gui.petData != null ? gui.petData.getEggCount(eggType) : 0;

                    if (count <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage("§c[Pet] §7Vous n'avez pas d'oeuf de ce type!");
                        return;
                    }

                    int toOpen = event.isShiftClick() ? Math.min(10, count) : 1;

                    if (toOpen == 1) {
                        // Animation d'ouverture
                        player.closeInventory();
                        playEggOpenAnimation(gui.plugin, player, eggType);
                    } else {
                        // Ouverture multiple sans animation
                        openMultipleEggs(gui.plugin, player, eggType, toOpen);
                        new PetEggGUI(gui.plugin, player).open();
                    }

                    return;
                }
            }
        }

        private void playEggOpenAnimation(ZombieZPlugin plugin, Player player, EggType eggType) {
            player.playSound(player.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, 1.0f, 1.0f);

            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick < 20) {
                        // Phase 1: L'oeuf tremble
                        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.5, 0),
                            3, 0.1, 0.1, 0.1, 0.02);
                        if (tick % 5 == 0) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                                0.5f, 0.5f + (tick * 0.05f));
                        }
                    } else if (tick == 20) {
                        // Phase 2: Explosion!
                        PetType result = plugin.getPetManager().openEgg(player, eggType);
                        if (result != null) {
                            // Particules selon la rareté
                            Particle particle = switch (result.getRarity()) {
                                case COMMON -> Particle.CLOUD;
                                case UNCOMMON -> Particle.HAPPY_VILLAGER;
                                case RARE -> Particle.ENCHANT;
                                case EPIC -> Particle.WITCH;
                                case LEGENDARY -> Particle.END_ROD;
                                case MYTHIC -> Particle.SOUL_FIRE_FLAME;
                            };

                            player.spawnParticle(particle, player.getLocation().add(0, 1.5, 0),
                                50, 0.5, 0.5, 0.5, 0.1);

                            Sound sound = result.getRarity().isAtLeast(PetRarity.LEGENDARY) ?
                                Sound.UI_TOAST_CHALLENGE_COMPLETE :
                                Sound.ENTITY_PLAYER_LEVELUP;
                            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                        }
                    } else if (tick >= 40) {
                        cancel();
                        // Rouvrir le GUI
                        Bukkit.getScheduler().runTask(plugin, () ->
                            new PetEggGUI(plugin, player).open());
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        private void openMultipleEggs(ZombieZPlugin plugin, Player player, EggType eggType, int count) {
            int newPets = 0;
            int duplicates = 0;

            for (int i = 0; i < count; i++) {
                PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
                if (data == null || data.getEggCount(eggType) <= 0) break;

                PetType result = plugin.getPetManager().openEgg(player, eggType);
                if (result != null) {
                    if (data.hasPet(result) && data.getPet(result).getCopies() == 1) {
                        newPets++;
                    } else {
                        duplicates++;
                    }
                }
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage("§a[Pet] §7Ouvert §e" + count + " §7oeufs!");
            player.sendMessage("§a[Pet] §7Nouveaux: §a" + newPets + " §7| Duplicatas: §e" + duplicates);
        }
    }
}
