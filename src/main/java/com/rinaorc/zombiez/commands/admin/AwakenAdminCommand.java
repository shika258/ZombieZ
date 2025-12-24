package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.awaken.Awaken;
import com.rinaorc.zombiez.items.awaken.AwakenManager;
import com.rinaorc.zombiez.items.awaken.AwakenModifierType;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commandes admin pour le système d'éveil
 *
 * /awaken debug - Affiche l'éveil de l'item tenu
 * /awaken give <player> <talent_id> - Donne un item avec un éveil spécifique
 * /awaken reroll - Reroll l'éveil de l'item tenu
 * /awaken list - Liste tous les talents disponibles pour les éveils
 * /awaken stats - Affiche les statistiques du système d'éveil
 * /awaken chances - Affiche les chances d'éveil par rareté
 */
public class AwakenAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public AwakenAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombiez.admin.awaken")) {
            sender.sendMessage("§c§l✖ §cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "debug" -> handleDebug(sender, args);
            case "give" -> handleGive(sender, args);
            case "reroll" -> handleReroll(sender, args);
            case "list" -> handleList(sender, args);
            case "stats" -> handleStats(sender, args);
            case "chances" -> handleChances(sender, args);
            case "reload" -> handleReload(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    /**
     * /awaken debug - Affiche les détails de l'éveil de l'item tenu
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c§l✖ §cCette commande doit être utilisée en jeu.");
            return true;
        }

        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!ZombieZItem.isZombieZItem(mainHand)) {
            sender.sendMessage("§c§l✖ §cVous ne tenez pas un item ZombieZ.");
            return true;
        }

        // Vérifier si l'item a un éveil
        if (!awakenManager.hasAwaken(mainHand)) {
            sender.sendMessage("§e§l⚡ §eCet item n'a pas d'éveil.");
            return true;
        }

        // Récupérer l'éveil
        Awaken awaken = awakenManager.getAwakenFromItem(mainHand);
        if (awaken == null) {
            sender.sendMessage("§c§l✖ §cErreur lors de la lecture de l'éveil.");
            return true;
        }

        // Afficher les détails
        sender.sendMessage("");
        sender.sendMessage("§d§l✦ ÉVEIL DEBUG §d✦");
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§7ID: §f" + awaken.getId());
        sender.sendMessage("§7Nom: §d" + awaken.getDisplayName());
        sender.sendMessage("§7Classe: §e" + (awaken.getRequiredClass() != null ?
            awaken.getRequiredClass().getColoredName() : "§7Aucune"));
        sender.sendMessage("§7Branche: §e" + (awaken.getRequiredBranch() != null ?
            awaken.getRequiredBranch().getDisplayName() : "§7Aucune"));
        sender.sendMessage("§7Talent: §b" + awaken.getTargetTalentId());
        sender.sendMessage("§7Type d'effet: §c" + (awaken.getTargetEffectType() != null ?
            awaken.getTargetEffectType().name() : "§7Inconnu"));
        sender.sendMessage("§7Modificateur: §a" + awaken.getModifierType().name());
        sender.sendMessage("§7Valeur: §6" + String.format("%.2f", awaken.getModifierValue()));
        sender.sendMessage("§7Description: §f" + awaken.getEffectDescription());
        sender.sendMessage("§8§m                              ");

        // Vérifier si l'éveil est actif pour ce joueur
        boolean isActive = awakenManager.isAwakenActive(player, awaken);
        sender.sendMessage("§7Statut: " + (isActive ? "§a✔ ACTIF" : "§c✖ INACTIF"));

        // Afficher la raison si inactif
        if (!isActive) {
            ClassData classData = plugin.getClassManager().getClassData(player);
            if (!classData.hasClass()) {
                sender.sendMessage("§7Raison: §cPas de classe sélectionnée");
            } else if (!awaken.isClassCompatible(classData.getSelectedClass())) {
                sender.sendMessage("§7Raison: §cClasse incompatible (vous êtes " +
                    classData.getSelectedClass().getColoredName() + "§c)");
            } else if (!classData.hasTalent(awaken.getTargetTalentId())) {
                sender.sendMessage("§7Raison: §cTalent non actif ou non sélectionné");
            }
        }
        sender.sendMessage("");

        return true;
    }

    /**
     * /awaken give <player> <talent_id> [quality] - Donne un item avec un éveil
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /awaken give <player> <talent_id> [quality]");
            sender.sendMessage("§7Quality: 0.0 à 1.0 (défaut: 0.5)");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c§l✖ §cJoueur non trouvé: " + args[1]);
            return true;
        }

        String talentId = args[2];
        double quality = args.length > 3 ? parseDouble(args[3], 0.5) : 0.5;

        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        // Générer l'éveil
        Awaken awaken = awakenManager.generateAwakenForTalentId(talentId, quality);
        if (awaken == null) {
            sender.sendMessage("§c§l✖ §cTalent non trouvé: " + talentId);
            sender.sendMessage("§7Utilisez /awaken list pour voir les talents disponibles.");
            return true;
        }

        // Générer un item de test avec l'éveil
        var generator = com.rinaorc.zombiez.items.generator.ItemGenerator.getInstance();
        ZombieZItem zombieItem = generator.generate(10, Rarity.LEGENDARY,
            com.rinaorc.zombiez.items.types.ItemType.SWORD, 0.5);
        zombieItem.setAwakenId(awaken.getId());

        ItemStack item = zombieItem.toItemStack();

        // Stocker l'éveil dans le PDC
        awakenManager.storeAwakenInItem(item, awaken);

        // Donner l'item
        target.getInventory().addItem(item);

        sender.sendMessage("§a§l✔ §aÉveil généré: §d" + awaken.getDisplayName());
        sender.sendMessage("§7Talent: §b" + talentId);
        sender.sendMessage("§7Modificateur: §e" + awaken.getModifierType().name() +
            " §7(§6" + String.format("%.1f", awaken.getModifierValue()) + "§7)");
        sender.sendMessage("§7Donné à: §e" + target.getName());

        return true;
    }

    /**
     * /awaken reroll - Reroll l'éveil de l'item tenu
     */
    private boolean handleReroll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c§l✖ §cCette commande doit être utilisée en jeu.");
            return true;
        }

        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!ZombieZItem.isZombieZItem(mainHand)) {
            sender.sendMessage("§c§l✖ §cVous ne tenez pas un item ZombieZ.");
            return true;
        }

        // Récupérer les données de l'item
        ZombieZItem zombieItem = ZombieZItem.fromItemStack(mainHand);
        if (zombieItem == null) {
            sender.sendMessage("§c§l✖ §cErreur lors de la lecture de l'item.");
            return true;
        }

        // Générer un nouvel éveil aléatoire
        Awaken newAwaken = awakenManager.generateAwaken(zombieItem.getRarity(), zombieItem.getZoneLevel());
        if (newAwaken == null) {
            sender.sendMessage("§c§l✖ §cErreur lors de la génération de l'éveil.");
            return true;
        }

        // Mettre à jour l'éveil
        zombieItem.setAwakenId(newAwaken.getId());

        // Recréer l'item
        ItemStack newItem = zombieItem.toItemStack();
        awakenManager.storeAwakenInItem(newItem, newAwaken);

        // Remplacer l'item
        player.getInventory().setItemInMainHand(newItem);

        sender.sendMessage("§a§l✔ §aÉveil rerollé!");
        sender.sendMessage("§7Nouvel éveil: §d" + newAwaken.getDisplayName());
        sender.sendMessage("§7Talent: §b" + newAwaken.getTargetTalentId());
        sender.sendMessage("§7Effet: §e" + newAwaken.getEffectDescription());

        return true;
    }

    /**
     * /awaken list [class] [branch] - Liste les talents disponibles
     */
    private boolean handleList(CommandSender sender, String[] args) {
        var talentManager = plugin.getTalentManager();
        if (talentManager == null) {
            sender.sendMessage("§c§l✖ §cLe système de talents n'est pas initialisé.");
            return true;
        }

        // Filtrer par classe si spécifié
        ClassType filterClass = null;
        if (args.length > 1) {
            try {
                filterClass = ClassType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cClasse invalide: " + args[1]);
                sender.sendMessage("§7Classes disponibles: GUERRIER, CHASSEUR, OCCULTISTE");
                return true;
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§d§l✦ TALENTS DISPONIBLES POUR ÉVEILS §d✦");
        sender.sendMessage("§8§m                              ");

        for (ClassType classType : ClassType.values()) {
            if (filterClass != null && classType != filterClass) continue;

            sender.sendMessage("");
            sender.sendMessage(classType.getColoredName() + "§7:");

            List<Talent> talents = talentManager.getTalentsForClass(classType);
            if (talents.isEmpty()) {
                sender.sendMessage("  §8Aucun talent enregistré");
                continue;
            }

            // Grouper par branche
            Map<TalentBranch, List<Talent>> byBranch = new HashMap<>();
            for (Talent talent : talents) {
                TalentBranch[] branches = TalentBranch.getBranchesForClass(classType);
                int slot = talent.getSlotIndex();
                TalentBranch branch = (slot >= 0 && slot < branches.length) ? branches[slot] : null;
                byBranch.computeIfAbsent(branch, k -> new ArrayList<>()).add(talent);
            }

            for (var entry : byBranch.entrySet()) {
                TalentBranch branch = entry.getKey();
                String branchName = branch != null ? branch.getDisplayName() : "Général";
                sender.sendMessage("  §e" + branchName + "§7:");

                for (Talent talent : entry.getValue()) {
                    sender.sendMessage("    §8- §b" + talent.getId() + " §7(" + talent.getName() + ")");
                }
            }
        }

        sender.sendMessage("");
        return true;
    }

    /**
     * /awaken stats - Affiche les statistiques
     */
    private boolean handleStats(CommandSender sender, String[] args) {
        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage("§d§l✦ STATISTIQUES ÉVEILS §d✦");
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§7" + awakenManager.getStats());
        sender.sendMessage("");

        // Compter les types de modificateurs
        sender.sendMessage("§7Types de modificateurs disponibles:");
        for (AwakenModifierType type : AwakenModifierType.values()) {
            sender.sendMessage("  §8- §e" + type.name() + " §7(" + type.getDisplayName() + ")");
        }
        sender.sendMessage("");

        return true;
    }

    /**
     * /awaken chances - Affiche les chances par rareté
     */
    private boolean handleChances(CommandSender sender, String[] args) {
        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage("§d§l✦ CHANCES D'ÉVEIL PAR RARETÉ §d✦");
        sender.sendMessage("§8§m                              ");

        for (var entry : awakenManager.getAwakenChances().entrySet()) {
            Rarity rarity = entry.getKey();
            double chance = entry.getValue() * 100;
            String bar = generateChanceBar(chance);
            sender.sendMessage(rarity.getColorCode() + rarity.getDisplayName() +
                "§7: " + bar + " §f" + String.format("%.2f%%", chance));
        }

        sender.sendMessage("");
        sender.sendMessage("§7Note: Ces chances sont les chances de base.");
        sender.sendMessage("§7Des bonus peuvent s'appliquer (zone, luck).");
        sender.sendMessage("");

        return true;
    }

    /**
     * /awaken reload - Recharge la configuration
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        AwakenManager awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            sender.sendMessage("§c§l✖ §cLe système d'éveil n'est pas initialisé.");
            return true;
        }

        awakenManager.loadFromConfig(plugin.getConfigManager().loadConfig("awakens.yml"));
        sender.sendMessage("§a§l✔ §aConfiguration des éveils rechargée.");

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§d§l✦ AWAKEN - Commandes Admin §d✦");
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§e/awaken debug §7- Affiche l'éveil de l'item tenu");
        sender.sendMessage("§e/awaken give <player> <talent_id> [quality] §7- Donne un item avec un éveil");
        sender.sendMessage("§e/awaken reroll §7- Reroll l'éveil de l'item tenu");
        sender.sendMessage("§e/awaken list [class] §7- Liste les talents disponibles");
        sender.sendMessage("§e/awaken stats §7- Statistiques du système");
        sender.sendMessage("§e/awaken chances §7- Chances par rareté");
        sender.sendMessage("§e/awaken reload §7- Recharge la configuration");
        sender.sendMessage("");
    }

    private String generateChanceBar(double percent) {
        int filled = (int) Math.round(percent * 2); // 0-20 pour 0-10%
        int empty = 20 - filled;
        return "§a" + "▓".repeat(Math.max(0, filled)) + "§8" + "░".repeat(Math.max(0, empty));
    }

    private double parseDouble(String str, double defaultValue) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombiez.admin.awaken")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0],
                Arrays.asList("debug", "give", "reroll", "list", "stats", "chances", "reload"));
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return null; // Liste des joueurs en ligne
            }
            if (args[0].equalsIgnoreCase("list")) {
                return filterStartsWith(args[1],
                    Arrays.stream(ClassType.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Liste des IDs de talents
            var talentManager = plugin.getTalentManager();
            if (talentManager != null) {
                return filterStartsWith(args[2],
                    talentManager.getAllTalentIds());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filterStartsWith(args[3],
                Arrays.asList("0.0", "0.25", "0.5", "0.75", "1.0"));
        }

        return Collections.emptyList();
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .collect(Collectors.toList());
    }
}
