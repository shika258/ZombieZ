package com.rinaorc.zombiez.pets.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.eggs.EggType;
import org.bukkit.Bukkit;
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
 * Commandes admin pour les pets
 *
 * /petadmin give <joueur> <pet> [niveau] [copies]
 * /petadmin giveegg <joueur> <type> [quantité]
 * /petadmin givefragments <joueur> <quantité>
 * /petadmin setlevel <joueur> <pet> <niveau>
 * /petadmin reset <joueur>
 * /petadmin spawnpet <pet> - Spawn visuel pour tests
 * /petadmin unlockall <joueur> - Débloque tous les pets pour un joueur
 */
public class PetAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public PetAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.pet.admin")) {
            sender.sendMessage("§c[Pet Admin] §7Vous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "giveegg" -> handleGiveEgg(sender, args);
            case "givefragments" -> handleGiveFragments(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "reset" -> handleReset(sender, args);
            case "spawnpet" -> handleSpawnPet(sender, args);
            case "unlockall" -> handleUnlockAll(sender, args);
            case "list" -> handleListPets(sender);
            case "eggs" -> handleListEggs(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin give <joueur> <pet> [niveau] [copies]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        PetType type = PetType.fromId(args[2]);
        if (type == null) {
            sender.sendMessage("§c[Pet Admin] §7Pet introuvable: §e" + args[2]);
            sender.sendMessage("§7Utilisez §e/petadmin list §7pour voir les IDs.");
            return;
        }

        int level = args.length > 3 ? parseInt(args[3], 1) : 1;
        int copies = args.length > 4 ? parseInt(args[4], 1) : 1;

        level = Math.max(1, Math.min(9, level));
        copies = Math.max(1, copies);

        plugin.getPetManager().givePet(target, type, level, copies);
        sender.sendMessage("§a[Pet Admin] §7Pet " + type.getColoredName() + " §7donné à §e" + target.getName() +
            " §7(Lv." + level + ", " + copies + " copies)");
    }

    private void handleGiveEgg(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin giveegg <joueur> <type> [quantité]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        EggType type = EggType.fromName(args[2]);
        if (type == null) {
            sender.sendMessage("§c[Pet Admin] §7Type d'oeuf inconnu: §e" + args[2]);
            sender.sendMessage("§7Types: " + Arrays.stream(EggType.values())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.joining(", ")));
            return;
        }

        int quantity = args.length > 3 ? parseInt(args[3], 1) : 1;
        quantity = Math.max(1, quantity);

        plugin.getPetManager().giveEgg(target, type, quantity);
        sender.sendMessage("§a[Pet Admin] §7Donné §ex" + quantity + " " + type.getColoredName() +
            " §7à §e" + target.getName());
    }

    private void handleGiveFragments(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin givefragments <joueur> <quantité>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        int amount = parseInt(args[2], 0);
        if (amount <= 0) {
            sender.sendMessage("§c[Pet Admin] §7Quantité invalide!");
            return;
        }

        PlayerPetData data = plugin.getPetManager().getOrLoadPlayerData(target.getUniqueId());
        data.addFragments(amount);

        sender.sendMessage("§a[Pet Admin] §7Donné §d" + amount + " fragments §7à §e" + target.getName());
        target.sendMessage("§a[Pet] §7Vous avez reçu §d" + amount + " fragments§7!");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin setlevel <joueur> <pet> <niveau>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        PetType type = PetType.fromId(args[2]);
        if (type == null) {
            sender.sendMessage("§c[Pet Admin] §7Pet introuvable: §e" + args[2]);
            return;
        }

        int level = parseInt(args[3], 1);
        level = Math.max(1, Math.min(9, level));

        PlayerPetData playerData = plugin.getPetManager().getOrLoadPlayerData(target.getUniqueId());
        if (!playerData.hasPet(type)) {
            sender.sendMessage("§c[Pet Admin] §7" + target.getName() + " ne possède pas ce pet!");
            return;
        }

        PetData petData = playerData.getPet(type);
        petData.setLevel(level);

        // Ajuster les copies pour correspondre au niveau
        int requiredCopies = type.getRarity().getTotalCopiesForLevel(level);
        if (petData.getCopies() < requiredCopies) {
            petData.setCopies(requiredCopies);
        }

        playerData.markDirty();

        sender.sendMessage("§a[Pet Admin] §7Pet " + type.getColoredName() + " §7de §e" + target.getName() +
            " §7mis au niveau §a" + level);
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin reset <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        // Créer de nouvelles données vierges
        PlayerPetData newData = new PlayerPetData(target.getUniqueId());

        // Note: En production, il faudrait aussi nettoyer la BDD
        sender.sendMessage("§c[Pet Admin] §7Données de pets de §e" + target.getName() + " §7réinitialisées!");
        target.sendMessage("§c[Pet] §7Vos données de pets ont été réinitialisées par un admin.");
    }

    private void handleSpawnPet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Pet Admin] §7Cette commande doit être exécutée par un joueur!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin spawnpet <pet>");
            return;
        }

        PetType type = PetType.fromId(args[1]);
        if (type == null) {
            sender.sendMessage("§c[Pet Admin] §7Pet introuvable: §e" + args[1]);
            return;
        }

        plugin.getPetManager().getDisplayManager().spawnPetDisplay(player, type);
        sender.sendMessage("§a[Pet Admin] §7Pet " + type.getColoredName() + " §7spawné!");
    }

    private void handleUnlockAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c[Pet Admin] §7Usage: §e/petadmin unlockall <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Pet Admin] §7Joueur introuvable: §e" + args[1]);
            return;
        }

        PlayerPetData playerData = plugin.getPetManager().getOrLoadPlayerData(target.getUniqueId());
        int unlockedCount = 0;

        // Débloquer tous les pets au niveau 1 avec 1 copie
        for (PetType type : PetType.values()) {
            if (!playerData.hasPet(type)) {
                playerData.addPet(type);
                unlockedCount++;
            }
        }

        playerData.markDirty();

        if (unlockedCount > 0) {
            sender.sendMessage("§a[Pet Admin] §7Débloqué §e" + unlockedCount + " pets §7pour §e" + target.getName() + "§7!");
            target.sendMessage("§a[Pet] §7Un admin vous a débloqué §e" + unlockedCount + " pets§7!");
        } else {
            sender.sendMessage("§e[Pet Admin] §7" + target.getName() + " possède déjà tous les pets!");
        }
    }

    private void handleListPets(CommandSender sender) {
        sender.sendMessage("§7═══════ §e🐾 Liste des Pets §7═══════");
        sender.sendMessage("");

        for (PetRarity rarity : PetRarity.values()) {
            PetType[] pets = PetType.getByRarity(rarity);
            if (pets.length > 0) {
                sender.sendMessage(rarity.getColoredName() + "§7 (" + pets.length + "):");
                for (PetType pet : pets) {
                    sender.sendMessage("  §7- §f" + pet.getId().toLowerCase() + " §7(" + pet.getDisplayName() + ")");
                }
            }
        }
    }

    private void handleListEggs(CommandSender sender) {
        sender.sendMessage("§7═══════ §e🥚 Types d'Oeufs §7═══════");
        sender.sendMessage("");

        for (EggType type : EggType.values()) {
            sender.sendMessage(type.getColoredName() + " §7- §f" + type.name().toLowerCase());
            if (type.getMinimumRarity() != null) {
                sender.sendMessage("  §7Minimum garanti: " + type.getMinimumRarity().getColoredName());
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7═══════ §e🐾 Pet Admin §7═══════");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin give <joueur> <pet> [niveau] [copies]");
        sender.sendMessage("§7  Donne un pet à un joueur");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin giveegg <joueur> <type> [quantité]");
        sender.sendMessage("§7  Donne des oeufs à un joueur");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin givefragments <joueur> <quantité>");
        sender.sendMessage("§7  Donne des fragments à un joueur");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin setlevel <joueur> <pet> <niveau>");
        sender.sendMessage("§7  Définit le niveau d'un pet");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin reset <joueur>");
        sender.sendMessage("§7  Réinitialise les données pets");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin spawnpet <pet>");
        sender.sendMessage("§7  Spawn un pet visuel pour tests");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin unlockall <joueur>");
        sender.sendMessage("§7  Débloque tous les pets pour un joueur");
        sender.sendMessage("");
        sender.sendMessage("§e/petadmin list §7- Liste tous les pets");
        sender.sendMessage("§e/petadmin eggs §7- Liste les types d'oeufs");
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "giveegg", "givefragments", "setlevel", "reset", "spawnpet", "unlockall", "list", "eggs"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("give") || sub.equals("giveegg") || sub.equals("givefragments") ||
                sub.equals("setlevel") || sub.equals("reset") || sub.equals("unlockall")) {
                // Liste des joueurs en ligne
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (sub.equals("spawnpet")) {
                // Liste des pets
                Arrays.stream(PetType.values())
                    .forEach(t -> completions.add(t.getId().toLowerCase()));
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();

            if (sub.equals("give") || sub.equals("setlevel")) {
                Arrays.stream(PetType.values())
                    .forEach(t -> completions.add(t.getId().toLowerCase()));
            } else if (sub.equals("giveegg")) {
                Arrays.stream(EggType.values())
                    .forEach(t -> completions.add(t.name().toLowerCase()));
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();

            if (sub.equals("give") || sub.equals("setlevel")) {
                // Niveaux 1-9
                for (int i = 1; i <= 9; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
