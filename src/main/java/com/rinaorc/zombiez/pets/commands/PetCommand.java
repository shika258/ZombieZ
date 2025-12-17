package com.rinaorc.zombiez.pets.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.eggs.EggType;
import com.rinaorc.zombiez.pets.gui.PetMainGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /pet pour les joueurs
 *
 * Usage:
 * /pet - Ouvre le menu principal
 * /pet equip <nom> - Équipe un pet
 * /pet unequip - Déséquipe le pet actuel
 * /pet list - Liste tous les pets possédés
 * /pet info <nom> - Infos sur un pet
 * /pet egg - Ouvre un oeuf (si disponible)
 * /pet fragments - Affiche les fragments
 * /pet ability - Active la capacité du pet
 */
public class PetCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public PetCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Pet] §7Cette commande est réservée aux joueurs!");
            return true;
        }

        if (!player.hasPermission("zombiez.pet.use")) {
            player.sendMessage("§c[Pet] §7Vous n'avez pas la permission d'utiliser cette commande!");
            return true;
        }

        if (args.length == 0) {
            // Ouvrir le menu principal
            new PetMainGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "equip" -> handleEquip(player, args);
            case "unequip" -> handleUnequip(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "egg" -> handleEgg(player, args);
            case "fragments" -> handleFragments(player);
            case "ability", "skill", "activate", "r" -> handleAbility(player);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage("§c[Pet] §7Commande inconnue. Utilisez §e/pet help §7pour l'aide.");
            }
        }

        return true;
    }

    private void handleEquip(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c[Pet] §7Usage: §e/pet equip <nom>");
            return;
        }

        String petName = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toUpperCase();
        PetType type = PetType.fromId(petName);

        if (type == null) {
            // Chercher par nom d'affichage
            String searchName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            for (PetType t : PetType.values()) {
                if (t.getDisplayName().toLowerCase().contains(searchName)) {
                    type = t;
                    break;
                }
            }
        }

        if (type == null) {
            player.sendMessage("§c[Pet] §7Pet introuvable: §e" + args[1]);
            return;
        }

        plugin.getPetManager().equipPet(player, type);
    }

    private void handleUnequip(Player player) {
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedPet() == null) {
            player.sendMessage("§c[Pet] §7Vous n'avez aucun pet équipé!");
            return;
        }

        plugin.getPetManager().unequipPet(player);
    }

    private void handleList(Player player) {
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getPetCount() == 0) {
            player.sendMessage("§c[Pet] §7Vous ne possédez aucun pet!");
            player.sendMessage("§7Obtenez des pets via les oeufs de pet!");
            return;
        }

        player.sendMessage("§7═══════ §e🐾 Vos Pets §7═══════");
        player.sendMessage("");

        for (PetRarity rarity : PetRarity.values()) {
            List<PetData> petsOfRarity = data.getAllPets().stream()
                .filter(p -> p.getType().getRarity() == rarity)
                .collect(Collectors.toList());

            if (!petsOfRarity.isEmpty()) {
                player.sendMessage(rarity.getColoredName() + "§7:");
                for (PetData pet : petsOfRarity) {
                    String equipped = pet.getType() == data.getEquippedPet() ? " §a[ÉQUIPÉ]" : "";
                    String stars = pet.getStarPower() > 0 ? " §e" + "★".repeat(pet.getStarPower()) : "";
                    player.sendMessage("  §7- " + pet.getType().getColoredName() +
                        " §7[Lv." + pet.getLevel() + "]" + stars + equipped);
                }
            }
        }

        player.sendMessage("");
        player.sendMessage("§7Total: §e" + data.getPetCount() + "§7/§a" + PetType.values().length + " pets");
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            // Info sur le pet équipé
            PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
            if (data == null || data.getEquippedPet() == null) {
                player.sendMessage("§c[Pet] §7Usage: §e/pet info <nom>");
                return;
            }
            showPetInfo(player, data.getEquippedPet());
            return;
        }

        String petName = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toUpperCase();
        PetType type = PetType.fromId(petName);

        if (type == null) {
            String searchName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            for (PetType t : PetType.values()) {
                if (t.getDisplayName().toLowerCase().contains(searchName)) {
                    type = t;
                    break;
                }
            }
        }

        if (type == null) {
            player.sendMessage("§c[Pet] §7Pet introuvable: §e" + args[1]);
            return;
        }

        showPetInfo(player, type);
    }

    private void showPetInfo(Player player, PetType type) {
        PlayerPetData playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        PetData petData = playerData != null ? playerData.getPet(type) : null;

        player.sendMessage("§7═══════ " + type.getColoredName() + " §7═══════");
        player.sendMessage("");
        player.sendMessage("§7Rareté: " + type.getRarity().getColoredName());
        player.sendMessage("§7Thème: §f" + type.getTheme());
        player.sendMessage("§7Apparence: §8" + type.getAppearance());
        player.sendMessage("");

        if (petData != null) {
            String stars = petData.getStarPower() > 0 ? " §e" + "★".repeat(petData.getStarPower()) : "";
            player.sendMessage("§7Niveau: §a" + petData.getLevel() + "§7/9" + stars);
            player.sendMessage("§7Copies: §b" + petData.getCopies());
            player.sendMessage(petData.getProgressBar() + " §7" + String.format("%.1f", petData.getProgressPercent()) + "%");
        } else {
            player.sendMessage("§8Pet non possédé");
        }

        player.sendMessage("");
        player.sendMessage("§7═══ CAPACITÉS ═══");
        player.sendMessage("§7[Passif] §f" + type.getPassiveDescription());
        if (petData != null && petData.hasLevel5Bonus()) {
            player.sendMessage("§a[+Niv.5] §f" + type.getLevel5Bonus());
        }
        player.sendMessage("§b[Actif] " + type.getActiveName() + "§7: " + type.getActiveDescription());
        player.sendMessage("§7Cooldown: §e" + type.getActiveCooldown() + "s");
        player.sendMessage("");
        player.sendMessage("§e★ Star Power§7: " + type.getStarPowerDescription());
    }

    private void handleEgg(Player player, String[] args) {
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c[Pet] §7Erreur de chargement des données!");
            return;
        }

        if (args.length < 2) {
            // Ouvrir le premier oeuf disponible
            for (EggType type : EggType.values()) {
                if (data.getEggCount(type) > 0) {
                    PetType result = plugin.getPetManager().openEgg(player, type);
                    if (result != null) {
                        player.sendMessage("§a[Pet] §7Oeuf ouvert: " + result.getColoredName() + "§7!");
                    }
                    return;
                }
            }
            player.sendMessage("§c[Pet] §7Vous n'avez aucun oeuf!");
            return;
        }

        EggType type = EggType.fromName(args[1]);
        if (type == null) {
            player.sendMessage("§c[Pet] §7Type d'oeuf inconnu: §e" + args[1]);
            return;
        }

        if (data.getEggCount(type) <= 0) {
            player.sendMessage("§c[Pet] §7Vous n'avez pas d'oeuf de type " + type.getColoredName() + "§7!");
            return;
        }

        PetType result = plugin.getPetManager().openEgg(player, type);
        if (result != null) {
            player.sendMessage("§a[Pet] §7Oeuf ouvert: " + result.getColoredName() + "§7!");
        }
    }

    private void handleFragments(Player player) {
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
        int fragments = data != null ? data.getFragments() : 0;

        player.sendMessage("§7═══════ §d💎 Fragments §7═══════");
        player.sendMessage("");
        player.sendMessage("§7Fragments actuels: §d" + fragments);
        player.sendMessage("");
        player.sendMessage("§7Utilisez les fragments pour acheter");
        player.sendMessage("§7des copies de pets dans la boutique.");
        player.sendMessage("");
        player.sendMessage("§7Coûts par copie:");
        for (PetRarity rarity : PetRarity.values()) {
            if (rarity.getFragmentCost() > 0) {
                player.sendMessage("  " + rarity.getColoredName() + "§7: §d" + rarity.getFragmentCost() + " fragments");
            }
        }
    }

    private void handleAbility(Player player) {
        plugin.getPetManager().activateAbility(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7═══════ §e🐾 Pet Help §7═══════");
        player.sendMessage("");
        player.sendMessage("§e/pet §7- Ouvre le menu principal");
        player.sendMessage("§e/pet equip <nom> §7- Équipe un pet");
        player.sendMessage("§e/pet unequip §7- Déséquipe le pet actuel");
        player.sendMessage("§e/pet list §7- Liste vos pets");
        player.sendMessage("§e/pet info [nom] §7- Infos sur un pet");
        player.sendMessage("§e/pet egg [type] §7- Ouvre un oeuf");
        player.sendMessage("§e/pet fragments §7- Voir vos fragments");
        player.sendMessage("§e/pet ability §7- Active la capacité (ou touche R)");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("equip", "unequip", "list", "info", "egg", "fragments", "ability", "help"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("equip") || sub.equals("info")) {
                if (sender instanceof Player player) {
                    PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());
                    if (data != null) {
                        for (PetData pet : data.getAllPets()) {
                            completions.add(pet.getType().getId().toLowerCase());
                        }
                    }
                }
            } else if (sub.equals("egg")) {
                for (EggType type : EggType.values()) {
                    completions.add(type.name().toLowerCase());
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
