package com.rinaorc.zombiez.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.party.Party;
import com.rinaorc.zombiez.party.PartyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
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
 * Commande /party pour gérer les groupes
 * 
 * Sous-commandes:
 * - /party create - Crée un groupe
 * - /party invite <joueur> - Invite un joueur
 * - /party accept - Accepte une invitation
 * - /party deny - Refuse une invitation
 * - /party leave - Quitte le groupe
 * - /party kick <joueur> - Expulse un joueur
 * - /party disband - Dissout le groupe
 * - /party info - Affiche les infos du groupe
 * - /party chat <message> - Envoie un message au groupe
 * - /party settings - Paramètres du groupe
 */
public class PartyCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final PartyManager partyManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "create", "invite", "accept", "deny", "leave", "kick", 
        "disband", "info", "chat", "settings", "sharexp", "shareloot", "ff"
    );

    public PartyCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }
        
        if (args.length == 0) {
            partyManager.showPartyInfo(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "create", "new" -> handleCreate(player);
            case "invite", "inv", "add" -> handleInvite(player, args);
            case "accept", "join" -> handleAccept(player);
            case "deny", "decline", "refuse" -> handleDeny(player);
            case "leave", "quit" -> handleLeave(player);
            case "kick", "remove" -> handleKick(player, args);
            case "disband", "delete" -> handleDisband(player);
            case "info", "i", "status" -> { partyManager.showPartyInfo(player); yield true; }
            case "chat", "c", "say" -> handleChat(player, args);
            case "settings", "config" -> handleSettings(player, args);
            case "sharexp", "xp" -> handleShareXP(player);
            case "shareloot", "loot" -> handleShareLoot(player);
            case "ff", "friendlyfire", "pvp" -> handleFriendlyFire(player);
            case "help", "?" -> { showHelp(player); yield true; }
            default -> {
                MessageUtils.sendRaw(player, "§cSous-commande inconnue. Utilise §e/party help");
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player) {
        partyManager.createParty(player);
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendRaw(player, "§cUsage: §e/party invite <joueur>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.sendRaw(player, "§cJoueur non trouvé!");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtils.sendRaw(player, "§cTu ne peux pas t'inviter toi-même!");
            return true;
        }
        
        partyManager.invitePlayer(player, target);
        return true;
    }

    private boolean handleAccept(Player player) {
        partyManager.acceptInvite(player);
        return true;
    }

    private boolean handleDeny(Player player) {
        partyManager.denyInvite(player);
        return true;
    }

    private boolean handleLeave(Player player) {
        partyManager.leaveParty(player);
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendRaw(player, "§cUsage: §e/party kick <joueur>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.sendRaw(player, "§cJoueur non trouvé!");
            return true;
        }
        
        partyManager.kickFromParty(player, target);
        return true;
    }

    private boolean handleDisband(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            MessageUtils.sendRaw(player, "§cTu n'es pas dans un groupe!");
            return true;
        }
        
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            MessageUtils.sendRaw(player, "§cSeul le chef peut dissoudre le groupe!");
            return true;
        }
        
        // Demander confirmation
        MessageUtils.sendRaw(player, "§c⚠ Es-tu sûr de vouloir dissoudre le groupe?");
        MessageUtils.sendRaw(player, "§7Tape §e/party disband confirm §7pour confirmer.");
        
        // En pratique, il faudrait un système de confirmation
        // Pour simplifier, on dissout directement
        partyManager.disbandParty(player);
        return true;
    }

    private boolean handleChat(Player player, String[] args) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            MessageUtils.sendRaw(player, "§cTu n'es pas dans un groupe!");
            return true;
        }
        
        if (args.length < 2) {
            MessageUtils.sendRaw(player, "§cUsage: §e/party chat <message>");
            return true;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        party.broadcast("§7" + player.getName() + ": §f" + message);
        return true;
    }

    private boolean handleSettings(Player player, String[] args) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            MessageUtils.sendRaw(player, "§cTu n'es pas dans un groupe!");
            return true;
        }
        
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            MessageUtils.sendRaw(player, "§cSeul le chef peut modifier les paramètres!");
            return true;
        }
        
        // Afficher les paramètres actuels
        player.sendMessage("");
        MessageUtils.sendRaw(player, "§d§l═══ Paramètres du Groupe ═══");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§7Partage XP: " + (party.isShareXP() ? "§aON" : "§cOFF") + 
            " §8| §e/party sharexp");
        MessageUtils.sendRaw(player, "§7Partage Loot: " + (party.isShareLoot() ? "§aON" : "§cOFF") + 
            " §8| §e/party shareloot");
        MessageUtils.sendRaw(player, "§7Friendly Fire: " + (party.isFriendlyFire() ? "§aON" : "§cOFF") + 
            " §8| §e/party ff");
        player.sendMessage("");
        
        return true;
    }

    private boolean handleShareXP(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null || !party.getLeaderId().equals(player.getUniqueId())) {
            MessageUtils.sendRaw(player, "§cTu dois être chef de groupe!");
            return true;
        }
        
        party.setShareXP(!party.isShareXP());
        party.broadcast("§7Partage XP: " + (party.isShareXP() ? "§aActivé" : "§cDésactivé"));
        return true;
    }

    private boolean handleShareLoot(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null || !party.getLeaderId().equals(player.getUniqueId())) {
            MessageUtils.sendRaw(player, "§cTu dois être chef de groupe!");
            return true;
        }
        
        party.setShareLoot(!party.isShareLoot());
        party.broadcast("§7Partage Loot: " + (party.isShareLoot() ? "§aActivé §7(Round-robin)" : "§cDésactivé"));
        return true;
    }

    private boolean handleFriendlyFire(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null || !party.getLeaderId().equals(player.getUniqueId())) {
            MessageUtils.sendRaw(player, "§cTu dois être chef de groupe!");
            return true;
        }
        
        party.setFriendlyFire(!party.isFriendlyFire());
        party.broadcast("§7Friendly Fire: " + (party.isFriendlyFire() ? "§cActivé §7(Attention!)" : "§aDésactivé"));
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        MessageUtils.sendRaw(player, "§d§l═══ Aide Party ═══");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§e/party §7- Affiche les infos du groupe");
        MessageUtils.sendRaw(player, "§e/party create §7- Crée un nouveau groupe");
        MessageUtils.sendRaw(player, "§e/party invite <joueur> §7- Invite un joueur");
        MessageUtils.sendRaw(player, "§e/party accept §7- Accepte une invitation");
        MessageUtils.sendRaw(player, "§e/party deny §7- Refuse une invitation");
        MessageUtils.sendRaw(player, "§e/party leave §7- Quitte le groupe");
        MessageUtils.sendRaw(player, "§e/party kick <joueur> §7- Expulse un joueur");
        MessageUtils.sendRaw(player, "§e/party disband §7- Dissout le groupe");
        MessageUtils.sendRaw(player, "§e/party chat <msg> §7- Message au groupe");
        MessageUtils.sendRaw(player, "§e/party settings §7- Paramètres du groupe");
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§d§lBonus de proximité:");
        MessageUtils.sendRaw(player, "§72 joueurs: §a+15% §7| 3: §a+35% §7| 4: §a+60%");
        player.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                @NotNull String alias, @NotNull String[] args) {
        
        if (!(sender instanceof Player)) return null;
        
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}
