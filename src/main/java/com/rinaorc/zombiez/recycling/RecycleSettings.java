package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Paramètres de recyclage automatique pour un joueur
 * Stocke les préférences de recyclage par rareté et les statistiques
 */
@Getter
public class RecycleSettings {

    // Activation globale du recyclage automatique
    @Setter
    private boolean autoRecycleEnabled = false;

    // Activation du recyclage des consommables
    @Setter
    private boolean recycleConsumablesEnabled = false;

    // Protection de la hotbar (slots 0-8) contre le recyclage automatique
    @Setter
    private boolean protectHotbarEnabled = true;

    // Activation par rareté (par défaut: uniquement COMMON et UNCOMMON activés)
    private final Map<Rarity, Boolean> recycleByRarity;

    // Statistiques de session
    private final AtomicLong sessionPointsEarned = new AtomicLong(0);
    private final AtomicLong sessionItemsRecycled = new AtomicLong(0);

    // Statistiques totales
    private final AtomicLong totalPointsEarned = new AtomicLong(0);
    private final AtomicLong totalItemsRecycled = new AtomicLong(0);

    // Points gagnés dans la dernière minute (pour le résumé)
    private final AtomicLong lastMinutePoints = new AtomicLong(0);
    private final AtomicLong lastMinuteItems = new AtomicLong(0);

    // Milestones débloqués
    private final Set<RecycleMilestone> unlockedMilestones = EnumSet.noneOf(RecycleMilestone.class);

    // Meilleur recyclage unique (pour milestone SINGLE_RECYCLE)
    @Setter
    private int bestSingleRecycle = 0;

    public RecycleSettings() {
        this.recycleByRarity = new EnumMap<>(Rarity.class);

        // Par défaut: COMMON et UNCOMMON auto-recyclés
        for (Rarity rarity : Rarity.values()) {
            recycleByRarity.put(rarity, rarity == Rarity.COMMON || rarity == Rarity.UNCOMMON);
        }
    }

    /**
     * Vérifie si une rareté doit être auto-recyclée
     */
    public boolean shouldRecycle(Rarity rarity) {
        if (!autoRecycleEnabled) return false;
        return recycleByRarity.getOrDefault(rarity, false);
    }

    /**
     * Active/désactive le recyclage pour une rareté
     */
    public void setRecycleRarity(Rarity rarity, boolean enabled) {
        recycleByRarity.put(rarity, enabled);
    }

    /**
     * Toggle le recyclage pour une rareté
     * @return le nouvel état
     */
    public boolean toggleRecycleRarity(Rarity rarity) {
        boolean newState = !recycleByRarity.getOrDefault(rarity, false);
        recycleByRarity.put(rarity, newState);
        return newState;
    }

    /**
     * Vérifie si une rareté est configurée pour le recyclage
     */
    public boolean isRarityEnabled(Rarity rarity) {
        return recycleByRarity.getOrDefault(rarity, false);
    }

    /**
     * Ajoute des points aux statistiques de recyclage (1 item)
     */
    public void addRecycledItem(long points) {
        sessionPointsEarned.addAndGet(points);
        sessionItemsRecycled.incrementAndGet();
        totalPointsEarned.addAndGet(points);
        totalItemsRecycled.incrementAndGet();
        lastMinutePoints.addAndGet(points);
        lastMinuteItems.incrementAndGet();
    }

    /**
     * Ajoute des points aux statistiques de recyclage en batch (optimisé)
     * @param itemCount Nombre d'items recyclés
     * @param totalPoints Points totaux gagnés
     */
    public void addRecycledItemsBatch(int itemCount, long totalPoints) {
        sessionPointsEarned.addAndGet(totalPoints);
        sessionItemsRecycled.addAndGet(itemCount);
        totalPointsEarned.addAndGet(totalPoints);
        totalItemsRecycled.addAndGet(itemCount);
        lastMinutePoints.addAndGet(totalPoints);
        lastMinuteItems.addAndGet(itemCount);
    }

    /**
     * Réinitialise les compteurs de la dernière minute et retourne les valeurs
     * @return [points, items]
     */
    public long[] resetLastMinuteStats() {
        long points = lastMinutePoints.getAndSet(0);
        long items = lastMinuteItems.getAndSet(0);
        return new long[]{points, items};
    }

    /**
     * Réinitialise les stats de session
     */
    public void resetSessionStats() {
        sessionPointsEarned.set(0);
        sessionItemsRecycled.set(0);
    }

    /**
     * Obtient le nombre de raretés activées pour le recyclage
     */
    public int getEnabledRaritiesCount() {
        return (int) recycleByRarity.values().stream().filter(Boolean::booleanValue).count();
    }

    // ==================== MILESTONES ====================

    /**
     * Vérifie si un milestone est débloqué
     */
    public boolean isMilestoneUnlocked(RecycleMilestone milestone) {
        return unlockedMilestones.contains(milestone);
    }

    /**
     * Débloque un milestone
     * @return true si le milestone vient d'être débloqué (était verrouillé avant)
     */
    public boolean unlockMilestone(RecycleMilestone milestone) {
        return unlockedMilestones.add(milestone);
    }

    /**
     * Obtient le nombre de milestones débloqués
     */
    public int getUnlockedMilestonesCount() {
        return unlockedMilestones.size();
    }

    /**
     * Obtient le nombre total de milestones
     */
    public int getTotalMilestonesCount() {
        return RecycleMilestone.values().length;
    }

    /**
     * Obtient tous les milestones débloqués
     */
    public Set<RecycleMilestone> getUnlockedMilestones() {
        if (unlockedMilestones.isEmpty()) {
            return EnumSet.noneOf(RecycleMilestone.class);
        }
        return EnumSet.copyOf(unlockedMilestones);
    }

    /**
     * Met à jour le meilleur recyclage unique si nécessaire
     * @return true si c'est un nouveau record
     */
    public boolean updateBestSingleRecycle(int points) {
        if (points > bestSingleRecycle) {
            bestSingleRecycle = points;
            return true;
        }
        return false;
    }

    // ==================== SÉRIALISATION ====================

    /**
     * Sérialise les paramètres en chaîne pour stockage BDD
     * Format: "enabled;totalPoints;totalItems;rarities;milestones;bestSingle;consumablesEnabled;protectHotbar"
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(autoRecycleEnabled ? "1" : "0");
        sb.append(";");
        sb.append(totalPointsEarned.get());
        sb.append(";");
        sb.append(totalItemsRecycled.get());
        sb.append(";");

        // Raretés
        for (Rarity rarity : Rarity.values()) {
            sb.append(rarity.name()).append(":").append(recycleByRarity.getOrDefault(rarity, false) ? "1" : "0").append(",");
        }
        sb.append(";");

        // Milestones débloqués
        for (RecycleMilestone milestone : unlockedMilestones) {
            sb.append(milestone.getId()).append(",");
        }
        sb.append(";");

        // Meilleur recyclage unique
        sb.append(bestSingleRecycle);
        sb.append(";");

        // Recyclage des consommables
        sb.append(recycleConsumablesEnabled ? "1" : "0");
        sb.append(";");

        // Protection de la hotbar
        sb.append(protectHotbarEnabled ? "1" : "0");

        return sb.toString();
    }

    /**
     * Désérialise depuis une chaîne stockée en BDD
     */
    public static RecycleSettings deserialize(String data) {
        RecycleSettings settings = new RecycleSettings();

        if (data == null || data.isEmpty()) {
            return settings;
        }

        try {
            String[] parts = data.split(";", -1); // -1 pour garder les parties vides

            if (parts.length >= 1) {
                settings.setAutoRecycleEnabled(parts[0].equals("1"));
            }

            if (parts.length >= 2 && !parts[1].isEmpty()) {
                settings.totalPointsEarned.set(Long.parseLong(parts[1]));
            }

            if (parts.length >= 3 && !parts[2].isEmpty()) {
                settings.totalItemsRecycled.set(Long.parseLong(parts[2]));
            }

            if (parts.length >= 4 && !parts[3].isEmpty()) {
                String[] rarityParts = parts[3].split(",");
                for (String rarityPart : rarityParts) {
                    if (rarityPart.isEmpty()) continue;
                    String[] kv = rarityPart.split(":");
                    if (kv.length == 2) {
                        try {
                            Rarity rarity = Rarity.valueOf(kv[0]);
                            settings.recycleByRarity.put(rarity, kv[1].equals("1"));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }

            // Charger les milestones débloqués
            if (parts.length >= 5 && !parts[4].isEmpty()) {
                String[] milestoneParts = parts[4].split(",");
                for (String milestoneId : milestoneParts) {
                    if (milestoneId.isEmpty()) continue;
                    RecycleMilestone milestone = RecycleMilestone.fromId(milestoneId);
                    if (milestone != null) {
                        settings.unlockedMilestones.add(milestone);
                    }
                }
            }

            // Charger le meilleur recyclage unique
            if (parts.length >= 6 && !parts[5].isEmpty()) {
                settings.bestSingleRecycle = Integer.parseInt(parts[5]);
            }

            // Charger le recyclage des consommables
            if (parts.length >= 7 && !parts[6].isEmpty()) {
                settings.setRecycleConsumablesEnabled(parts[6].equals("1"));
            }

            // Charger la protection de la hotbar (défaut: true pour sécurité)
            if (parts.length >= 8 && !parts[7].isEmpty()) {
                settings.setProtectHotbarEnabled(parts[7].equals("1"));
            }
        } catch (Exception e) {
            // En cas d'erreur, retourner les paramètres par défaut
        }

        return settings;
    }

    /**
     * Vérifie si les consommables doivent être auto-recyclés
     */
    public boolean shouldRecycleConsumables() {
        return autoRecycleEnabled && recycleConsumablesEnabled;
    }
}
