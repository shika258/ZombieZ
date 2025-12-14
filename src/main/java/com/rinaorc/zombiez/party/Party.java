package com.rinaorc.zombiez.party;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Représente un groupe de joueurs (max 4)
 * Gère les bonus de proximité et le partage de rewards
 */
@Getter
public class Party {

    public static final int MAX_SIZE = 4;
    public static final double PROXIMITY_RADIUS = 30.0; // Blocs
    
    private final UUID partyId;
    private final UUID leaderId;
    private final Set<UUID> members;
    private final long createdAt;
    
    @Setter
    private String partyName;
    
    @Setter
    private boolean friendlyFire = false;
    
    @Setter
    private boolean shareXP = true;
    
    @Setter
    private boolean shareLoot = false; // Round-robin loot
    
    // Statistiques de session du groupe
    private final PartyStats sessionStats;
    
    // Index pour le round-robin loot
    private int lootIndex = 0;

    public Party(UUID leaderId, String leaderName) {
        this.partyId = UUID.randomUUID();
        this.leaderId = leaderId;
        this.members = ConcurrentHashMap.newKeySet();
        this.members.add(leaderId);
        this.createdAt = System.currentTimeMillis();
        this.partyName = leaderName + "'s Party";
        this.sessionStats = new PartyStats();
    }

    /**
     * Ajoute un membre au groupe
     */
    public boolean addMember(UUID playerId) {
        if (members.size() >= MAX_SIZE) return false;
        if (members.contains(playerId)) return false;
        
        members.add(playerId);
        return true;
    }

    /**
     * Retire un membre du groupe
     */
    public boolean removeMember(UUID playerId) {
        if (playerId.equals(leaderId)) return false; // Le leader ne peut pas partir
        return members.remove(playerId);
    }

    /**
     * Vérifie si un joueur est membre
     */
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    /**
     * Vérifie si le groupe est plein
     */
    public boolean isFull() {
        return members.size() >= MAX_SIZE;
    }

    /**
     * Obtient le nombre de membres
     */
    public int getSize() {
        return members.size();
    }

    /**
     * Obtient les membres en ligne
     */
    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    /**
     * Obtient les membres à proximité d'un joueur
     */
    public List<Player> getMembersNearby(Player center) {
        List<Player> nearby = new ArrayList<>();
        
        for (Player member : getOnlineMembers()) {
            if (member.equals(center)) continue;
            if (!member.getWorld().equals(center.getWorld())) continue;
            
            if (member.getLocation().distance(center.getLocation()) <= PROXIMITY_RADIUS) {
                nearby.add(member);
            }
        }
        
        return nearby;
    }

    /**
     * Compte les membres à proximité (incluant le joueur central)
     */
    public int countMembersNearby(Player center) {
        return getMembersNearby(center).size() + 1; // +1 pour le joueur lui-même
    }

    /**
     * Calcule le bonus de proximité (synergie de groupe)
     * 1 joueur = 0%, 2 = +15%, 3 = +35%, 4 = +60%
     */
    public double getProximityBonus(Player player) {
        int nearby = countMembersNearby(player);
        return switch (nearby) {
            case 1 -> 0.0;
            case 2 -> 0.15;
            case 3 -> 0.35;
            case 4 -> 0.60;
            default -> 0.0;
        };
    }

    /**
     * Obtient le prochain joueur pour le round-robin loot
     */
    public Player getNextLootRecipient() {
        List<Player> online = getOnlineMembers();
        if (online.isEmpty()) return null;
        
        lootIndex = (lootIndex + 1) % online.size();
        return online.get(lootIndex);
    }

    /**
     * Envoie un message à tous les membres
     */
    public void broadcast(String message) {
        String formatted = "§d[Party] §f" + message;
        for (Player member : getOnlineMembers()) {
            member.sendMessage(formatted);
        }
    }

    /**
     * Transfère le leadership
     */
    public UUID transferLeadership(UUID newLeaderId) {
        if (!members.contains(newLeaderId)) return null;
        // Note: leaderId est final, donc on retourne le nouveau leader
        // Le PartyManager gère le remplacement de l'objet Party
        return newLeaderId;
    }

    /**
     * Dissout le groupe
     */
    public void disband() {
        broadcast("§cLe groupe a été dissous!");
        members.clear();
    }

    /**
     * Statistiques de groupe pour la session
     */
    @Getter
    public static class PartyStats {
        private long totalKills = 0;
        private long totalXPEarned = 0;
        private long totalPointsEarned = 0;
        private int bossesKilled = 0;
        private int highestCombo = 0;
        private long sessionStartTime = System.currentTimeMillis();

        public void addKill() { totalKills++; }
        public void addXP(long xp) { totalXPEarned += xp; }
        public void addPoints(long points) { totalPointsEarned += points; }
        public void addBossKill() { bossesKilled++; }
        
        public void updateCombo(int combo) {
            if (combo > highestCombo) highestCombo = combo;
        }

        public long getSessionDuration() {
            return System.currentTimeMillis() - sessionStartTime;
        }

        public String getFormattedDuration() {
            long seconds = getSessionDuration() / 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }
}
