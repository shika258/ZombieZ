package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Génère le lore dynamique des éveils
 *
 * Le lore s'adapte au porteur:
 * - Éveil actif = couleurs vives + "✔ ACTIF"
 * - Éveil inactif = couleurs grises + raison de l'inactivité
 */
public class AwakenLoreBuilder {

    private final ZombieZPlugin plugin;

    public AwakenLoreBuilder(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Génère le lore d'un éveil pour un joueur spécifique
     *
     * @param awaken L'éveil à afficher
     * @param player Le joueur qui regarde l'item (peut être null)
     * @return Liste de lignes de lore
     */
    public List<String> buildLore(Awaken awaken, Player player) {
        List<String> lore = new ArrayList<>();

        if (awaken == null) return lore;

        // Déterminer si l'éveil est actif pour ce joueur
        boolean isActive = false;
        String inactiveReason = null;

        if (player != null) {
            var classManager = plugin.getClassManager();
            if (classManager != null) {
                ClassData classData = classManager.getClassData(player);
                if (classData.hasClass()) {
                    ClassType playerClass = classData.getSelectedClass();

                    if (!awaken.isClassCompatible(playerClass)) {
                        inactiveReason = "§c✖ Classe requise: " +
                            (awaken.getRequiredClass() != null ?
                                awaken.getRequiredClass().getColoredName() : "§7Inconnue");
                    } else if (!classData.hasTalent(awaken.getTargetTalentId())) {
                        inactiveReason = "§c✖ Talent non sélectionné";
                    } else {
                        isActive = true;
                    }
                } else {
                    inactiveReason = "§c✖ Aucune classe sélectionnée";
                }
            } else {
                inactiveReason = "§c✖ Système de classe indisponible";
            }
        } else {
            // Pas de joueur = affichage neutre
            inactiveReason = "§8(Équipez l'arme pour activer)";
        }

        // Couleurs selon l'état
        String nameColor = isActive ? "§d" : "§8";
        String effectColor = isActive ? "§a" : "§8";
        String talentColor = isActive ? "§e" : "§8";

        // Construire le lore
        lore.add("");
        lore.add("§8§m                    ");
        lore.add(buildHeader(isActive));
        lore.add(nameColor + awaken.getDisplayName());
        lore.add("§7Talent: " + talentColor + formatTalentName(awaken.getTargetTalentId()));
        lore.add("§7Effet: " + effectColor + awaken.getEffectDescription());

        if (!isActive && inactiveReason != null) {
            lore.add(inactiveReason);
        }

        lore.add("§8§m                    ");

        return lore;
    }

    /**
     * Génère le lore statique (sans contexte joueur)
     */
    public List<String> buildStaticLore(Awaken awaken) {
        return buildLore(awaken, null);
    }

    /**
     * Construit l'en-tête avec le statut
     */
    private String buildHeader(boolean isActive) {
        if (isActive) {
            return "§d§l✦ ÉVEIL §8- §a✔ ACTIF";
        } else {
            return "§d§l✦ ÉVEIL §8- §c✖ INACTIF";
        }
    }

    /**
     * Formate le nom d'un talent depuis son ID
     */
    private String formatTalentName(String talentId) {
        if (talentId == null || talentId.isEmpty()) {
            return "Inconnu";
        }

        // Essayer de récupérer le nom réel depuis le TalentManager
        var talentManager = plugin.getTalentManager();
        if (talentManager != null) {
            Talent talent = talentManager.getTalent(talentId);
            if (talent != null) {
                return talent.getName();
            }
        }

        // Fallback: formater l'ID
        String[] parts = talentId.split("_");
        if (parts.length > 1) {
            StringBuilder name = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (name.length() > 0) name.append(" ");
                String part = parts[i];
                if (!part.isEmpty()) {
                    name.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
                }
            }
            return name.toString();
        }

        return talentId;
    }

    /**
     * Génère un lore compact (2 lignes)
     */
    public List<String> buildCompactLore(Awaken awaken, boolean isActive) {
        List<String> lore = new ArrayList<>();

        if (awaken == null) return lore;

        String statusIcon = isActive ? "§a✔" : "§8✖";
        String nameColor = isActive ? "§d" : "§8";

        lore.add(nameColor + "✦ " + awaken.getDisplayName() + " " + statusIcon);
        lore.add("§7" + awaken.getEffectDescription());

        return lore;
    }
}
