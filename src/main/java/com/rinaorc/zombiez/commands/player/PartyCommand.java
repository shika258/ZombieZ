package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.party.Party;
import com.rinaorc.zombiez.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /party pour le système de groupe
 */
public class PartyCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public PartyCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande nécessite d'être un joueur.");
            return true;
        }

        PartyManager partyManager = plugin.getPartyManager();
        if (partyManager == null) {
            player.sendMessage("§cLe système de groupe n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            // Afficher les infos ou l'aide
            if (partyManager.isInParty(player)) {
                partyManager.showPartyInfo(player);
            } else {
                sendHelp(player);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create", "creer" -> handleCreate(player, partyManager);
            case "invite", "inviter" -> handleInvite(player, args, partyManager);
            case "accept", "accepter" -> handleAccept(player, partyManager);
            case "deny", "refuser" -> handleDeny(player, partyManager);
            case "leave", "quitter" -> handleLeave(player, partyManager);
            case "kick", "expulser" -> handleKick(player, args, partyManager);
            case "disband", "dissoudre" -> handleDisband(player, partyManager);
            case "info" -> partyManager.showPartyInfo(player);
            case "settings", "options" -> handleSettings(player, args, partyManager);
            case "chat", "c" -> handleChat(player, args, partyManager);
            case "help", "aide" -> sendHelp(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, PartyManager partyManager) {
        if (partyManager.isInParty(player)) {
            player.sendMessage("§cTu es déjà dans un groupe!");
            return;
        }
        
        Party party = partyManager.createParty(player);
        if (party != null) {
            player.sendMessage("§aGroupe créé avec succès!");
        }
    }

    private void handleInvite(Player player, String[] args, PartyManager partyManager) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party invite <joueur>");
            return;
        }

        if (!partyManager.isInParty(player)) {
            player.sendMessage("§cTu dois créer un groupe d'abord! §e/party create");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§cTu ne peux pas t'inviter toi-même!");
            return;
        }

        partyManager.invitePlayer(player, target);
    }

    private void handleAccept(Player player, PartyManager partyManager) {
        if (partyManager.acceptInvite(player)) {
            player.sendMessage("§aTu as rejoint le groupe!");
        } else {
            player.sendMessage("§cTu n'as aucune invitation en attente ou le groupe est plein.");
        }
    }

    private void handleDeny(Player player, PartyManager partyManager) {
        partyManager.denyInvite(player);
    }

    private void handleLeave(Player player, PartyManager partyManager) {
        if (!partyManager.isInParty(player)) {
            player.sendMessage("§cTu n'es pas dans un groupe.");
            return;
        }

        partyManager.leaveParty(player);
    }

    private void handleKick(Player player, String[] args, PartyManager partyManager) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party kick <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        partyManager.kickFromParty(player, target);
    }

    private void handleDisband(Player player, PartyManager partyManager) {
        if (!partyManager.isInParty(player)) {
            player.sendMessage("§cTu n'es pas dans un groupe.");
            return;
        }

        partyManager.disbandParty(player);
    }

    private void handleSettings(Player player, String[] args, PartyManager partyManager) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage("§cTu n'es pas dans un groupe.");
            return;
        }

        if (!party.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut modifier les options!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§e=== Options du Groupe ===");
            player.sendMessage("§7/party settings sharexp §8- " + (party.isShareXP() ? "§aActivé" : "§cDésactivé"));
            player.sendMessage("§7/party settings shareloot §8- " + (party.isShareLoot() ? "§aActivé" : "§cDésactivé"));
            player.sendMessage("§7/party settings ff §8- " + (party.isFriendlyFire() ? "§aActivé" : "§cDésactivé"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "sharexp", "xp" -> {
                party.setShareXP(!party.isShareXP());
                party.broadcast("§7Partage XP: " + (party.isShareXP() ? "§aActivé" : "§cDésactivé"));
            }
            case "shareloot", "loot" -> {
                party.setShareLoot(!party.isShareLoot());
                party.broadcast("§7Partage Loot: " + (party.isShareLoot() ? "§aActivé" : "§cDésactivé"));
            }
            case "ff", "friendlyfire" -> {
                party.setFriendlyFire(!party.isFriendlyFire());
                party.broadcast("§7Friendly Fire: " + (party.isFriendlyFire() ? "§aActivé" : "§cDésactivé"));
            }
            default -> player.sendMessage("§cOption inconnue.");
        }
    }

    private void handleChat(Player player, String[] args, PartyManager partyManager) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage("§cTu n'es pas dans un groupe.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /party chat <message>");
            return;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        party.broadcast("§d" + player.getName() + "§7: §f" + message);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Commandes Party ===");
        player.sendMessage("§e/party create §7- Créer un groupe");
        player.sendMessage("§e/party invite <joueur> §7- Inviter un joueur");
        player.sendMessage("§e/party accept §7- Accepter une invitation");
        player.sendMessage("§e/party deny §7- Refuser une invitation");
        player.sendMessage("§e/party leave §7- Quitter le groupe");
        player.sendMessage("§e/party kick <joueur> §7- Expulser un joueur");
        player.sendMessage("§e/party disband §7- Dissoudre le groupe");
        player.sendMessage("§e/party info §7- Voir les infos du groupe");
        player.sendMessage("§e/party settings §7- Options du groupe");
        player.sendMessage("§e/party chat <msg> §7- Chat de groupe");
        player.sendMessage("");
        player.sendMessage("§7Bonus de proximité (30 blocs):");
        player.sendMessage("§a  2 joueurs: +15%");
        player.sendMessage("§a  3 joueurs: +35%");
        player.sendMessage("§a  4 joueurs: +60%");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("create", "invite", "accept", "deny", "leave", 
                "kick", "disband", "info", "settings", "chat", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("settings")) {
                completions.addAll(List.of("sharexp", "shareloot", "ff"));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
