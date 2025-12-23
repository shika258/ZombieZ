package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;
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
     * Ajoute des points aux statistiques de recyclage
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

    /**
     * Sérialise les paramètres en chaîne pour stockage BDD
     * Format: "enabled;COMMON:true;UNCOMMON:true;..."
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(autoRecycleEnabled ? "1" : "0");
        sb.append(";");
        sb.append(totalPointsEarned.get());
        sb.append(";");
        sb.append(totalItemsRecycled.get());
        sb.append(";");

        for (Rarity rarity : Rarity.values()) {
            sb.append(rarity.name()).append(":").append(recycleByRarity.getOrDefault(rarity, false) ? "1" : "0").append(",");
        }

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
            String[] parts = data.split(";");

            if (parts.length >= 1) {
                settings.setAutoRecycleEnabled(parts[0].equals("1"));
            }

            if (parts.length >= 2) {
                settings.totalPointsEarned.set(Long.parseLong(parts[1]));
            }

            if (parts.length >= 3) {
                settings.totalItemsRecycled.set(Long.parseLong(parts[2]));
            }

            if (parts.length >= 4) {
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
        } catch (Exception e) {
            // En cas d'erreur, retourner les paramètres par défaut
        }

        return settings;
    }
}
