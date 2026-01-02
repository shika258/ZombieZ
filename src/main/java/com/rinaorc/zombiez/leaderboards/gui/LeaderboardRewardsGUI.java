package com.rinaorc.zombiez.leaderboards.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI pour voir et réclamer les récompenses de leaderboard
 */
public class LeaderboardRewardsGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private List<LeaderboardManager.PendingReward> rewards = new ArrayList<>();
    private final Set<Long> claimingRewards = new HashSet<>(); // Anti double-clic
    private boolean isClaimingAll = false; // Empêche les clics pendant claimAll
    private boolean isLoading = true; // Indicateur de chargement

    private static final int GUI_SIZE = 54;

    public LeaderboardRewardsGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text("🎁 Récompenses de Classement").color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        loadRewards();
    }

    private void loadRewards() {
        // Afficher le chargement
        isLoading = true;
        build();

        plugin.getNewLeaderboardManager().getPendingRewards(player.getUniqueId())
            .thenAccept(pending -> {
                this.rewards = pending;
                this.isLoading = false;
                Bukkit.getScheduler().runTask(plugin, this::build);
            })
            .exceptionally(ex -> {
                this.isLoading = false;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c✗ Erreur lors du chargement des récompenses.");
                    build();
                });
                return null;
            });
    }

    private void build() {
        inventory.clear();

        // Bordure
        fillBorder();

        // Titre
        inventory.setItem(4, createTitleItem());

        // Indicateur de chargement
        if (isLoading) {
            inventory.setItem(22, createLoadingItem());
            inventory.setItem(45, createBackItem());
            return;
        }

        if (rewards.isEmpty()) {
            // Message "aucune récompense"
            inventory.setItem(22, createNoRewardsItem());
        } else {
            // Afficher les récompenses
            int slot = 10;
            for (LeaderboardManager.PendingReward reward : rewards) {
                if (slot == 17) slot = 19;
                if (slot == 26) slot = 28;
                if (slot == 35) slot = 37;
                if (slot > 43) break;

                inventory.setItem(slot, createRewardItem(reward));
                slot++;
            }

            // Bouton "Tout réclamer"
            if (rewards.size() > 1) {
                inventory.setItem(49, createClaimAllItem());
            }
        }

        // Retour
        inventory.setItem(45, createBackItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l🎁 Tes Récompenses"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Récompenses gagnées grâce à"));
        lore.add(Component.text("§7tes performances dans les classements!"));
        lore.add(Component.empty());

        if (!rewards.isEmpty()) {
            // Calculer les totaux
            long totalPoints = rewards.stream().mapToLong(LeaderboardManager.PendingReward::getPoints).sum();
            int totalGems = rewards.stream().mapToInt(LeaderboardManager.PendingReward::getGems).sum();
            long totalTitles = rewards.stream().filter(r -> r.getTitle() != null && !r.getTitle().isEmpty()).count();
            long totalCosmetics = rewards.stream().filter(r -> r.getCosmetic() != null && !r.getCosmetic().isEmpty()).count();

            lore.add(Component.text("§e§l" + rewards.size() + " §7récompense(s) en attente"));
            lore.add(Component.empty());
            lore.add(Component.text("§8─────────────────"));
            lore.add(Component.text("§6§lTotal disponible:"));
            if (totalPoints > 0) {
                lore.add(Component.text("  §f▸ §e" + formatNumber(totalPoints) + " points"));
            }
            if (totalGems > 0) {
                lore.add(Component.text("  §f▸ §d" + totalGems + " gemmes"));
            }
            if (totalTitles > 0) {
                lore.add(Component.text("  §f▸ §6" + totalTitles + " titre(s)"));
            }
            if (totalCosmetics > 0) {
                lore.add(Component.text("  §f▸ §b" + totalCosmetics + " cosmétique(s)"));
            }
            lore.add(Component.text("§8─────────────────"));
        } else {
            lore.add(Component.text("§7Aucune récompense en attente"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLoadingItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§l⟳ Chargement..."));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Récupération de tes récompenses"));
        lore.add(Component.text("§7en cours..."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoRewardsItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§lAucune Récompense"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Tu n'as pas de récompenses"));
        lore.add(Component.text("§7en attente pour le moment."));
        lore.add(Component.empty());
        lore.add(Component.text("§eGrimpe dans les classements"));
        lore.add(Component.text("§epour gagner des récompenses!"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardItem(LeaderboardManager.PendingReward reward) {
        Material material = reward.getType().getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Titre avec rang
        String rankIcon = switch (reward.getRank()) {
            case 1 -> "§6§l🥇";
            case 2 -> "§f§l🥈";
            case 3 -> "§c§l🥉";
            default -> "§7#" + reward.getRank();
        };

        meta.displayName(Component.text(rankIcon + " §e" + reward.getType().getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§8" + reward.getFormattedDate()));
        lore.add(Component.empty());
        lore.add(Component.text("§7Période: §f" + reward.getPeriod().getDisplayName()));
        lore.add(Component.text("§7Rang obtenu: §f#" + reward.getRank()));
        lore.add(Component.empty());
        lore.add(Component.text("§8─────────────────"));

        lore.add(Component.text("§6§lContenu:"));
        if (reward.getPoints() > 0) {
            lore.add(Component.text("  §f▸ §e+" + formatNumber(reward.getPoints()) + " points"));
        }
        if (reward.getGems() > 0) {
            lore.add(Component.text("  §f▸ §d+" + reward.getGems() + " gemmes"));
        }
        if (reward.getTitle() != null && !reward.getTitle().isEmpty()) {
            lore.add(Component.text("  §f▸ §6Titre: " + reward.getTitle()));
        }
        if (reward.getCosmetic() != null && !reward.getCosmetic().isEmpty()) {
            lore.add(Component.text("  §f▸ §bCosmétique: " + reward.getCosmetic()));
        }
        lore.add(Component.text("§8─────────────────"));

        lore.add(Component.empty());
        lore.add(Component.text("§a§l➤ Clique pour réclamer!"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClaimAllItem() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§a§l✓ TOUT RÉCLAMER ✓"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Réclame toutes les §e§l" + rewards.size() + " §7récompenses"));
        lore.add(Component.text("§7en un seul clic!"));
        lore.add(Component.empty());

        // Calculer le total
        long totalPoints = rewards.stream().mapToLong(LeaderboardManager.PendingReward::getPoints).sum();
        int totalGems = rewards.stream().mapToInt(LeaderboardManager.PendingReward::getGems).sum();
        long totalTitles = rewards.stream().filter(r -> r.getTitle() != null && !r.getTitle().isEmpty()).count();
        long totalCosmetics = rewards.stream().filter(r -> r.getCosmetic() != null && !r.getCosmetic().isEmpty()).count();

        lore.add(Component.text("§8─────────────────"));
        lore.add(Component.text("§6§lTu vas recevoir:"));
        if (totalPoints > 0) {
            lore.add(Component.text("  §f▸ §e§l" + formatNumber(totalPoints) + " §epoints"));
        }
        if (totalGems > 0) {
            lore.add(Component.text("  §f▸ §d§l" + totalGems + " §dgemmes"));
        }
        if (totalTitles > 0) {
            lore.add(Component.text("  §f▸ §6§l" + totalTitles + " §6titre(s)"));
        }
        if (totalCosmetics > 0) {
            lore.add(Component.text("  §f▸ §b§l" + totalCosmetics + " §bcosmétique(s)"));
        }
        lore.add(Component.text("§8─────────────────"));

        lore.add(Component.empty());
        lore.add(Component.text("§a§l➤ CLIC GAUCHE POUR RÉCLAMER!"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c§l← Retour"));
        item.setItemMeta(meta);
        return item;
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Bloquer les actions pendant le chargement (sauf retour)
        if (isLoading && slot != 45) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Retour
        if (slot == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new LeaderboardMainGUI(plugin, player).open();
            return;
        }

        // Tout réclamer
        if (slot == 49 && rewards.size() > 1) {
            claimAll();
            return;
        }

        // Réclamer une récompense spécifique
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() != Material.GRAY_STAINED_GLASS_PANE &&
            clicked.getType() != Material.BARRIER && clicked.getType() != Material.CHEST &&
            clicked.getType() != Material.CLOCK) {

            // Trouver la récompense correspondante
            int rewardIndex = getRewardIndexFromSlot(slot);
            if (rewardIndex >= 0 && rewardIndex < rewards.size()) {
                claimReward(rewards.get(rewardIndex));
            }
        }
    }

    private int getRewardIndexFromSlot(int slot) {
        int index = 0;
        int currentSlot = 10;

        for (int i = 0; i < rewards.size(); i++) {
            if (currentSlot == slot) return i;

            currentSlot++;
            if (currentSlot == 17) currentSlot = 19;
            if (currentSlot == 26) currentSlot = 28;
            if (currentSlot == 35) currentSlot = 37;
        }

        return -1;
    }

    private void claimReward(LeaderboardManager.PendingReward reward) {
        // Vérification anti double-clic
        if (isClaimingAll || claimingRewards.contains(reward.getId())) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        claimingRewards.add(reward.getId());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        plugin.getNewLeaderboardManager().claimReward(player.getUniqueId(), reward.getId())
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    claimingRewards.remove(reward.getId());
                    if (success) {
                        rewards.remove(reward);
                        build();
                    } else {
                        // Son d'erreur si échec
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                        player.sendMessage("§c✗ Impossible de réclamer cette récompense.");
                    }
                });
            })
            .exceptionally(ex -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    claimingRewards.remove(reward.getId());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                    player.sendMessage("§c✗ Erreur lors de la réclamation.");
                });
                return null;
            });
    }

    private void claimAll() {
        // Vérification anti double-clic
        if (isClaimingAll || rewards.isEmpty()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        isClaimingAll = true;
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        player.sendMessage("§e⟳ §7Réclamation en cours...");

        List<LeaderboardManager.PendingReward> toProcess = new ArrayList<>(rewards);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (LeaderboardManager.PendingReward reward : toProcess) {
            plugin.getNewLeaderboardManager().claimReward(player.getUniqueId(), reward.getId())
                .thenAccept(success -> {
                    if (success) successCount.incrementAndGet();
                    int processed = processedCount.incrementAndGet();

                    // Quand toutes les récompenses sont traitées
                    if (processed >= toProcess.size()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            isClaimingAll = false;
                            rewards.clear();
                            loadRewards(); // Recharger pour s'assurer de la cohérence

                            int claimed = successCount.get();
                            if (claimed == toProcess.size()) {
                                player.sendMessage("§a§l✓ §a" + claimed + " récompense(s) réclamée(s) avec succès!");
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            } else {
                                player.sendMessage("§e⚠ §7" + claimed + "/" + toProcess.size() + " récompenses réclamées.");
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
                            }
                        });
                    }
                })
                .exceptionally(ex -> {
                    processedCount.incrementAndGet();
                    return null;
                });
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
