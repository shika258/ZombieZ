package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PlayerPetData;
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

import java.util.List;

/**
 * GUI des options de pets
 * Permet de configurer les préférences d'affichage et de comportement des pets
 */
public class PetOptionsGUI implements InventoryHolder {

    private static final String TITLE = "§8§l⚙ Options des Pets";
    private static final int SIZE = 45;

    // Slots pour les options
    private static final int SLOT_SHOW_ENTITY = 11;
    private static final int SLOT_SHOW_PARTICLES = 13;
    private static final int SLOT_SHOW_MESSAGES = 15;
    private static final int SLOT_AUTO_EQUIP = 29;
    private static final int SLOT_PLAY_SOUNDS = 31;
    private static final int SLOT_RESET = 33;
    private static final int SLOT_BACK = 40;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;

    public PetOptionsGUI(ZombieZPlugin plugin, Player player) {
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

        // Bordure décorative
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }

        // Titre de section
        inventory.setItem(4, new ItemBuilder(Material.COMPARATOR)
            .name("§6§l⚙ Paramètres")
            .lore(List.of(
                "",
                "§7Configurez vos préférences",
                "§7pour les pets."
            ))
            .build());

        // Option: Afficher l'entité du pet
        boolean showEntity = petData != null && petData.isShowPetEntity();
        inventory.setItem(SLOT_SHOW_ENTITY, createToggleItem(
            showEntity,
            Material.ARMOR_STAND,
            "§e🐾 Afficher le Pet",
            "§7Affiche l'entité de votre pet",
            "§7qui vous suit en jeu."
        ));

        // Option: Afficher les particules
        boolean showParticles = petData != null && petData.isShowPetParticles();
        inventory.setItem(SLOT_SHOW_PARTICLES, createToggleItem(
            showParticles,
            Material.BLAZE_POWDER,
            "§d✨ Particules",
            "§7Affiche les effets visuels",
            "§7et particules des pets."
        ));

        // Option: Messages de capacité
        boolean showMessages = petData != null && petData.isShowAbilityMessages();
        inventory.setItem(SLOT_SHOW_MESSAGES, createToggleItem(
            showMessages,
            Material.PAPER,
            "§b💬 Messages",
            "§7Affiche les messages lors",
            "§7de l'activation des capacités."
        ));

        // Option: Équiper automatiquement
        boolean autoEquip = petData != null && petData.isAutoEquipOnJoin();
        inventory.setItem(SLOT_AUTO_EQUIP, createToggleItem(
            autoEquip,
            Material.ENDER_PEARL,
            "§a🔄 Auto-équipement",
            "§7Ré-équipe automatiquement",
            "§7votre dernier pet à la connexion."
        ));

        // Option: Sons des pets
        boolean playSounds = petData != null && petData.isPlayPetSounds();
        inventory.setItem(SLOT_PLAY_SOUNDS, createToggleItem(
            playSounds,
            Material.NOTE_BLOCK,
            "§6🔊 Sons",
            "§7Active les effets sonores",
            "§7liés aux pets."
        ));

        // Bouton reset
        inventory.setItem(SLOT_RESET, new ItemBuilder(Material.TNT)
            .name("§c⚠ Réinitialiser")
            .lore(List.of(
                "",
                "§7Remet toutes les options",
                "§7à leurs valeurs par défaut.",
                "",
                "§eCliquez pour réinitialiser"
            ))
            .build());

        // Retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());
    }

    private ItemStack createToggleItem(boolean enabled, Material icon, String name, String... description) {
        ItemBuilder builder = new ItemBuilder(enabled ? icon : Material.GRAY_DYE)
            .name((enabled ? "§a✓ " : "§c✗ ") + name);

        builder.lore("");
        for (String line : description) {
            builder.lore(line);
        }
        builder.lore("");
        builder.lore(enabled ? "§a✓ Activé" : "§c✗ Désactivé");
        builder.lore("");
        builder.lore("§eCliquez pour " + (enabled ? "désactiver" : "activer"));

        if (enabled) {
            builder.glow(true);
        }

        return builder.build();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
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
            if (!(event.getInventory().getHolder() instanceof PetOptionsGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            PlayerPetData petData = gui.petData;

            if (petData == null) return;

            switch (slot) {
                case SLOT_SHOW_ENTITY -> {
                    petData.setShowPetEntity(!petData.isShowPetEntity());
                    petData.markDirty();
                    playToggleSound(player, petData.isShowPetEntity());

                    // Mettre à jour l'entité du pet si nécessaire
                    if (!petData.isShowPetEntity()) {
                        plugin.getPetManager().getDisplayManager().removePetDisplay(player);
                    } else if (petData.getEquippedPet() != null) {
                        plugin.getPetManager().getDisplayManager().spawnPetDisplay(player, petData.getEquippedPet());
                    }

                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_SHOW_PARTICLES -> {
                    petData.setShowPetParticles(!petData.isShowPetParticles());
                    petData.markDirty();
                    playToggleSound(player, petData.isShowPetParticles());
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_SHOW_MESSAGES -> {
                    petData.setShowAbilityMessages(!petData.isShowAbilityMessages());
                    petData.markDirty();
                    playToggleSound(player, petData.isShowAbilityMessages());
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_AUTO_EQUIP -> {
                    petData.setAutoEquipOnJoin(!petData.isAutoEquipOnJoin());
                    petData.markDirty();
                    playToggleSound(player, petData.isAutoEquipOnJoin());
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_PLAY_SOUNDS -> {
                    petData.setPlayPetSounds(!petData.isPlayPetSounds());
                    petData.markDirty();
                    playToggleSound(player, petData.isPlayPetSounds());
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_RESET -> {
                    // Réinitialiser toutes les options
                    petData.setShowPetEntity(true);
                    petData.setShowPetParticles(true);
                    petData.setShowAbilityMessages(true);
                    petData.setAutoEquipOnJoin(true);
                    petData.setPlayPetSounds(true);
                    petData.markDirty();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendMessage("§e[Pet] §7Options réinitialisées!");
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_BACK -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetMainGUI(gui.plugin, player).open();
                }
            }
        }

        private void playToggleSound(Player player, boolean enabled) {
            if (enabled) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof PetOptionsGUI) {
                event.setCancelled(true);
            }
        }
    }
}
