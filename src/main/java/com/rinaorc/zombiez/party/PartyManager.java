package com.rinaorc.zombiez.party;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de groupe
 */
public class PartyManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerParties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> invitations = new ConcurrentHashMap<>();

    public PartyManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Crée un groupe
     */
    public Party createParty(Player leader) {
        if (isInParty(leader)) return null;
        
        Party party = new Party(leader.getUniqueId(), leader.getName());
        
        parties.put(party.getPartyId(), party);
        playerParties.put(leader.getUniqueId(), party.getPartyId());
        
        leader.sendMessage("§aGroupe créé! §7Invite des joueurs avec §e/party invite <joueur>");
        
        return party;
    }

    /**
     * Invite un joueur
     */
    public boolean invite(Player leader, Player target) {
        UUID partyId = playerParties.get(leader.getUniqueId());
        if (partyId == null) return false;
        
        Party party = parties.get(partyId);
        if (party == null || !party.getLeaderId().equals(leader.getUniqueId())) return false;
        
        if (isInParty(target)) {
            leader.sendMessage("§c" + target.getName() + " est déjà dans un groupe!");
            return false;
        }
        
        invitations.put(target.getUniqueId(), partyId);
        
        target.sendMessage("§e" + leader.getName() + " §7t'invite à rejoindre son groupe! §e/party accept");
        leader.sendMessage("§7Invitation envoyée à §e" + target.getName());
        
        // Expiration après 60 secondes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            invitations.remove(target.getUniqueId());
        }, 1200L);
        
        return true;
    }
    
    /**
     * Alias pour invite
     */
    public boolean invitePlayer(Player leader, Player target) {
        return invite(leader, target);
    }

    /**
     * Accepte une invitation
     */
    public boolean acceptInvite(Player player) {
        UUID partyId = invitations.remove(player.getUniqueId());
        if (partyId == null) return false;
        
        Party party = parties.get(partyId);
        if (party == null || party.isFull()) return false;
        
        party.addMember(player.getUniqueId());
        playerParties.put(player.getUniqueId(), partyId);
        
        // Notifier le groupe
        party.broadcast("§a" + player.getName() + " a rejoint le groupe!");
        
        return true;
    }
    
    /**
     * Refuse une invitation
     */
    public boolean denyInvite(Player player) {
        UUID partyId = invitations.remove(player.getUniqueId());
        if (partyId == null) {
            player.sendMessage("§cTu n'as aucune invitation en attente.");
            return false;
        }
        
        Party party = parties.get(partyId);
        if (party != null) {
            Player leader = Bukkit.getPlayer(party.getLeaderId());
            if (leader != null) {
                leader.sendMessage("§c" + player.getName() + " a refusé ton invitation.");
            }
        }
        
        player.sendMessage("§7Invitation refusée.");
        return true;
    }

    /**
     * Quitte le groupe
     */
    public boolean leave(Player player) {
        UUID partyId = playerParties.remove(player.getUniqueId());
        if (partyId == null) return false;
        
        Party party = parties.get(partyId);
        if (party == null) return false;
        
        // Si c'était le leader, dissoudre ou transférer
        if (party.getLeaderId().equals(player.getUniqueId())) {
            // Dissoudre le groupe
            disbandParty(player);
        } else {
            party.removeMember(player.getUniqueId());
            party.broadcast("§c" + player.getName() + " a quitté le groupe.");
        }
        
        player.sendMessage("§7Tu as quitté le groupe.");
        
        return true;
    }
    
    /**
     * Alias pour leave
     */
    public boolean leaveParty(Player player) {
        return leave(player);
    }
    
    /**
     * Expulse un joueur du groupe
     */
    public boolean kickFromParty(Player leader, Player target) {
        UUID partyId = playerParties.get(leader.getUniqueId());
        if (partyId == null) return false;
        
        Party party = parties.get(partyId);
        if (party == null || !party.getLeaderId().equals(leader.getUniqueId())) {
            leader.sendMessage("§cTu n'es pas le chef du groupe!");
            return false;
        }
        
        if (!party.isMember(target.getUniqueId())) {
            leader.sendMessage("§cCe joueur n'est pas dans ton groupe!");
            return false;
        }
        
        if (target.getUniqueId().equals(party.getLeaderId())) {
            leader.sendMessage("§cTu ne peux pas t'expulser toi-même!");
            return false;
        }
        
        party.removeMember(target.getUniqueId());
        playerParties.remove(target.getUniqueId());
        
        party.broadcast("§c" + target.getName() + " a été expulsé du groupe.");
        target.sendMessage("§cTu as été expulsé du groupe.");
        
        return true;
    }
    
    /**
     * Dissout le groupe
     */
    public boolean disbandParty(Player leader) {
        UUID partyId = playerParties.get(leader.getUniqueId());
        if (partyId == null) return false;
        
        Party party = parties.get(partyId);
        if (party == null) return false;
        
        if (!party.getLeaderId().equals(leader.getUniqueId())) {
            leader.sendMessage("§cTu n'es pas le chef du groupe!");
            return false;
        }
        
        // Retirer tous les membres
        for (UUID memberId : party.getMembers()) {
            playerParties.remove(memberId);
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage("§cLe groupe a été dissous.");
            }
        }
        
        party.disband();
        parties.remove(partyId);
        
        return true;
    }
    
    /**
     * Affiche les infos du groupe
     */
    public void showPartyInfo(Player player) {
        Party party = getParty(player);
        
        if (party == null) {
            player.sendMessage("§cTu n'es pas dans un groupe.");
            player.sendMessage("§7Crée un groupe avec §e/party create");
            return;
        }
        
        player.sendMessage("§6§l=== " + party.getPartyName() + " ===");
        player.sendMessage("§7Membres: §e" + party.getSize() + "/" + Party.MAX_SIZE);
        
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            String name = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberId).getName();
            String status = member != null && member.isOnline() ? "§a●" : "§c●";
            String role = memberId.equals(party.getLeaderId()) ? " §6★ Chef" : "";
            
            player.sendMessage("  " + status + " §f" + name + role);
        }
        
        player.sendMessage("");
        player.sendMessage("§7Options:");
        player.sendMessage("  §7XP partagé: " + (party.isShareXP() ? "§aOui" : "§cNon"));
        player.sendMessage("  §7Loot partagé: " + (party.isShareLoot() ? "§aOui" : "§cNon"));
        player.sendMessage("  §7Friendly fire: " + (party.isFriendlyFire() ? "§aOui" : "§cNon"));
        
        // Stats de session
        Party.PartyStats stats = party.getSessionStats();
        player.sendMessage("");
        player.sendMessage("§e§lStats de Session:");
        player.sendMessage("  §7Durée: §f" + stats.getFormattedDuration());
        player.sendMessage("  §7Kills: §c" + stats.getTotalKills());
        player.sendMessage("  §7XP gagné: §a" + stats.getTotalXPEarned());
        player.sendMessage("  §7Points gagnés: §6" + stats.getTotalPointsEarned());
    }

    /**
     * Vérifie si un joueur est dans un groupe
     */
    public boolean isInParty(Player player) {
        return playerParties.containsKey(player.getUniqueId());
    }

    /**
     * Obtient le groupe d'un joueur
     */
    public Party getParty(Player player) {
        UUID partyId = playerParties.get(player.getUniqueId());
        return partyId != null ? parties.get(partyId) : null;
    }
    
    /**
     * Obtient le groupe d'un joueur par UUID
     */
    public Party getParty(UUID playerId) {
        UUID partyId = playerParties.get(playerId);
        return partyId != null ? parties.get(partyId) : null;
    }

    /**
     * Obtient les membres proches du groupe
     */
    public List<Player> getNearbyMembers(Player player, double radius) {
        Party party = getParty(player);
        if (party == null) return Collections.emptyList();
        
        return party.getMembersNearby(player);
    }
    
    /**
     * Appelé quand un joueur se déconnecte
     */
    public void onPlayerQuit(Player player) {
        Party party = getParty(player);
        if (party == null) return;
        
        // Si c'est le leader, dissoudre le groupe
        if (party.getLeaderId().equals(player.getUniqueId())) {
            disbandParty(player);
        } else {
            // Sinon, juste le retirer
            party.removeMember(player.getUniqueId());
            playerParties.remove(player.getUniqueId());
            party.broadcast("§c" + player.getName() + " s'est déconnecté.");
        }
        
        // Supprimer les invitations
        invitations.remove(player.getUniqueId());
    }
    
    /**
     * Calcule le bonus de proximité pour un joueur
     */
    public double getProximityBonus(Player player) {
        Party party = getParty(player);
        if (party == null) return 0.0;
        
        return party.getProximityBonus(player);
    }
    
    /**
     * Enregistre un kill pour les stats de groupe
     */
    public void registerKill(Player player) {
        Party party = getParty(player);
        if (party != null) {
            party.getSessionStats().addKill();
        }
    }
    
    /**
     * Enregistre de l'XP pour les stats de groupe
     */
    public void registerXP(Player player, long xp) {
        Party party = getParty(player);
        if (party != null) {
            party.getSessionStats().addXP(xp);
        }
    }
    
    /**
     * Enregistre des points pour les stats de groupe
     */
    public void registerPoints(Player player, long points) {
        Party party = getParty(player);
        if (party != null) {
            party.getSessionStats().addPoints(points);
        }
    }
}
