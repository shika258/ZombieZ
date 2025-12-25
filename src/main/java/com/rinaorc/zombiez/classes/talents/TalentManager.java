package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gestionnaire central des talents
 * - Registre de tous les talents par classe
 * - Récupération des talents actifs d'un joueur
 * - Vérification des prérequis
 */
@Getter
public class TalentManager {

    private final ZombieZPlugin plugin;

    // Registre des talents par classe
    private final Map<ClassType, List<Talent>> talentsByClass = new EnumMap<>(ClassType.class);

    // Index rapide par ID
    private final Map<String, Talent> talentsById = new HashMap<>();

    public TalentManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        registerAllTalents();
    }

    /**
     * Enregistre tous les talents de toutes les classes
     */
    private void registerAllTalents() {
        // Guerrier
        List<Talent> guerrierTalents = GuerrierTalents.getAll();
        talentsByClass.put(ClassType.GUERRIER, guerrierTalents);
        guerrierTalents.forEach(t -> talentsById.put(t.getId(), t));

        // Chasseur
        List<Talent> chasseurTalents = ChasseurTalents.getAll();
        talentsByClass.put(ClassType.CHASSEUR, chasseurTalents);
        chasseurTalents.forEach(t -> talentsById.put(t.getId(), t));

        // Occultiste
        List<Talent> occultisteTalents = OccultisteTalents.getAll();
        talentsByClass.put(ClassType.OCCULTISTE, occultisteTalents);
        occultisteTalents.forEach(t -> talentsById.put(t.getId(), t));

        plugin.getLogger().info("[Talents] Enregistré " + talentsById.size() + " talents pour " +
            talentsByClass.size() + " classe(s)");
    }

    // ==================== RÉCUPÉRATION DE TALENTS ====================

    /**
     * Obtient un talent par son ID
     */
    public Talent getTalent(String id) {
        return talentsById.get(id);
    }

    /**
     * Obtient tous les talents d'une classe
     */
    public List<Talent> getTalentsForClass(ClassType classType) {
        return talentsByClass.getOrDefault(classType, Collections.emptyList());
    }

    /**
     * Obtient les talents d'un palier pour une classe
     */
    public List<Talent> getTalentsForTier(ClassType classType, TalentTier tier) {
        return getTalentsForClass(classType).stream()
            .filter(t -> t.getTier() == tier)
            .sorted(Comparator.comparingInt(Talent::getSlotIndex))
            .toList();
    }

    /**
     * Obtient tous les IDs de talents enregistrés
     * Utilisé pour l'auto-complétion des commandes
     */
    public List<String> getAllTalentIds() {
        return new ArrayList<>(talentsById.keySet());
    }

    /**
     * Obtient tous les talents actifs d'un joueur
     * Ne retourne que les talents sélectionnés ET activés (pas dans disabledTalents)
     */
    public List<Talent> getActiveTalents(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass()) return Collections.emptyList();

        List<Talent> active = new ArrayList<>();
        for (Map.Entry<TalentTier, String> entry : data.getAllSelectedTalents().entrySet()) {
            String talentId = entry.getValue();
            Talent talent = talentsById.get(talentId);
            // Vérifier que le talent existe, que le palier est débloqué ET que le talent est activé
            if (talent != null && data.isTalentTierUnlocked(entry.getKey()) && data.isTalentEnabled(talentId)) {
                active.add(talent);
            }
        }
        return active;
    }

    /**
     * Obtient le talent actif d'un joueur pour un palier
     * Retourne null si le talent est désactivé
     */
    public Talent getActiveTalentForTier(Player player, TalentTier tier) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass()) return null;
        if (!data.isTalentTierUnlocked(tier)) return null;

        String talentId = data.getSelectedTalentId(tier);
        if (talentId == null) return null;

        // Vérifier que le talent est activé (pas dans disabledTalents)
        if (!data.isTalentEnabled(talentId)) return null;

        return talentsById.get(talentId);
    }

    // ==================== VÉRIFICATION ====================

    /**
     * Vérifie si un joueur a un talent actif spécifique
     */
    public boolean hasTalent(Player player, String talentId) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data.hasTalent(talentId);
    }

    /**
     * Vérifie si un joueur a un type d'effet de talent actif
     */
    public boolean hasTalentEffect(Player player, Talent.TalentEffectType effectType) {
        return getActiveTalents(player).stream()
            .anyMatch(t -> t.getEffectType() == effectType);
    }

    /**
     * Obtient le talent actif avec un type d'effet spécifique
     */
    public Talent getActiveTalentWithEffect(Player player, Talent.TalentEffectType effectType) {
        return getActiveTalents(player).stream()
            .filter(t -> t.getEffectType() == effectType)
            .findFirst()
            .orElse(null);
    }

    /**
     * Alias pour getActiveTalentWithEffect (compatibilité)
     */
    public Talent getActiveTalentByEffect(Player player, Talent.TalentEffectType effectType) {
        return getActiveTalentWithEffect(player, effectType);
    }

    /**
     * Vérifie si un joueur peut sélectionner un talent
     */
    public boolean canSelectTalent(Player player, Talent talent) {
        ClassData data = plugin.getClassManager().getClassData(player);

        // Doit avoir une classe
        if (!data.hasClass()) return false;

        // Le talent doit être de la même classe
        if (talent.getClassType() != data.getSelectedClass()) return false;

        // Le palier doit être débloqué
        if (!data.isTalentTierUnlocked(talent.getTier())) return false;

        // Pas de cooldown si c'est un changement
        String currentTalentId = data.getSelectedTalentId(talent.getTier());
        if (currentTalentId != null && !currentTalentId.equals(talent.getId())) {
            if (data.isOnTalentChangeCooldown(talent.getTier())) return false;
        }

        return true;
    }

    // ==================== SÉLECTION ====================

    /**
     * Sélectionne un talent pour un joueur
     * @return true si la sélection a réussi
     */
    public boolean selectTalent(Player player, Talent talent) {
        if (!canSelectTalent(player, talent)) return false;

        ClassData data = plugin.getClassManager().getClassData(player);
        boolean success = data.selectTalent(talent.getTier(), talent.getId());

        if (success) {
            // Notification (si activée)
            if (data.isTalentMessagesEnabled()) {
                player.sendMessage("");
                player.sendMessage("§a§l+ TALENT SÉLECTIONNÉ +");
                player.sendMessage("§7" + talent.getTier().getDisplayName() + ": " + talent.getColoredName());
                player.sendMessage("");
            }

            // Effet sonore
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

            // === BRANCHE BÊTE: Invoquer les bêtes si talent de bête sélectionné ===
            if (talent.getId().contains("beast_") && plugin.getBeastManager() != null) {
                // Délai de 2 ticks pour laisser le temps à la BD de se mettre à jour
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getBeastManager().summonBeastsForPlayer(player);
                }, 2L);
            }

            // Notifier le système de Parcours (Journey)
            if (plugin.getJourneyListener() != null) {
                int totalTalents = data.getSelectedTalentCount();
                plugin.getJourneyListener().onUnlockTalent(player, totalTalents);
            }
        }

        return success;
    }

    /**
     * Sélectionne un talent par son ID pour un joueur
     * @return true si la sélection a réussi
     */
    public boolean selectTalentById(Player player, String talentId) {
        Talent talent = getTalent(talentId);
        if (talent == null) return false;
        return selectTalent(player, talent);
    }

    // ==================== UTILITAIRES ====================

    /**
     * Compte le nombre total de talents enregistrés
     */
    public int getTotalTalentCount() {
        return talentsById.size();
    }

    /**
     * Obtient les stats de talents d'un joueur sous forme de texte
     */
    public String getTalentSummary(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass()) return "§7Pas de classe";

        int unlocked = data.getUnlockedTierCount();
        int selected = data.getSelectedTalentCount();

        return String.format("§e%d§7/§e%d§7 paliers débloqués, §a%d§7 talents actifs",
            unlocked, TalentTier.values().length, selected);
    }

    /**
     * Obtient les talents non sélectionnés pour un joueur (paliers débloqués sans sélection)
     */
    public List<TalentTier> getUnselectedTiers(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass()) return Collections.emptyList();

        List<TalentTier> unselected = new ArrayList<>();
        for (TalentTier tier : TalentTier.values()) {
            if (data.isTalentTierUnlocked(tier) && data.getSelectedTalentId(tier) == null) {
                unselected.add(tier);
            }
        }
        return unselected;
    }

    /**
     * Notifie un joueur s'il a des talents à sélectionner
     */
    public void notifyUnselectedTalents(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.isTalentMessagesEnabled()) return;

        List<TalentTier> unselected = getUnselectedTiers(player);
        if (!unselected.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§6§l+ TALENTS DISPONIBLES +");
            player.sendMessage("§7Vous avez §e" + unselected.size() + "§7 palier(s) de talent");
            player.sendMessage("§7à sélectionner! §8(/class)");
            player.sendMessage("");
        }
    }
}
