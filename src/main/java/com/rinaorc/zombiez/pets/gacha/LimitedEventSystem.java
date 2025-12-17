package com.rinaorc.zombiez.pets.gacha;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetRarity;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système d'événements limités pour créer le FOMO
 * Double XP, Chance boost, Oeufs spéciaux, etc.
 */
public class LimitedEventSystem {

    private final ZombieZPlugin plugin;

    // Événement actif
    @Getter
    private LimitedEvent activeEvent = null;

    // Historique des participations
    private final Map<UUID, Map<String, EventParticipation>> participations = new ConcurrentHashMap<>();

    // Types d'événements prédéfinis
    private static final List<EventTemplate> EVENT_TEMPLATES = Arrays.asList(
        // Weekend events
        new EventTemplate(
            "weekend_luck", "§6§l✨ Weekend Chanceux!",
            "§7+25% de chance sur tous les drops!",
            EventType.LUCK_BOOST, 0.25, Duration.ofHours(48)
        ),
        new EventTemplate(
            "weekend_double", "§e§l⭐ Weekend Double!",
            "§7x2 fragments sur les duplicatas!",
            EventType.DOUBLE_FRAGMENTS, 2.0, Duration.ofHours(48)
        ),

        // Short events (6-12h)
        new EventTemplate(
            "flash_legendary", "§6§l🔥 FLASH LÉGENDAIRE!",
            "§7+50% chance de Légendaire!\n§cSEULEMENT 6 HEURES!",
            EventType.RARITY_BOOST, 0.50, Duration.ofHours(6)
        ),
        new EventTemplate(
            "flash_mythic", "§c§l⚡ FLASH MYTHIQUE!",
            "§7+100% chance de Mythique!\n§cSEULEMENT 3 HEURES!",
            EventType.MYTHIC_BOOST, 1.0, Duration.ofHours(3)
        ),
        new EventTemplate(
            "happy_hour", "§d§l🎉 Happy Hour!",
            "§7-50% prix sur la boutique!\n§cSEULEMENT 1 HEURE!",
            EventType.SHOP_DISCOUNT, 0.50, Duration.ofHours(1)
        ),

        // Seasonal events
        new EventTemplate(
            "halloween", "§6§l🎃 Halloween Event!",
            "§7Oeufs d'Halloween disponibles!\n§7Pets exclusifs spooky!",
            EventType.SPECIAL_EGG, 0, Duration.ofDays(7)
        ),
        new EventTemplate(
            "christmas", "§c§l🎄 Event de Noël!",
            "§7Cadeaux quotidiens!\n§7Pets de Noël exclusifs!",
            EventType.SPECIAL_EGG, 0, Duration.ofDays(14)
        ),
        new EventTemplate(
            "anniversary", "§d§l🎂 Anniversaire ZombieZ!",
            "§7x3 sur tout!\n§7Pets collectors limités!",
            EventType.MEGA_BOOST, 3.0, Duration.ofDays(3)
        )
    );

    // Pets exclusifs par événement (simulé - en prod ce serait dans PetType)
    private static final Map<String, List<String>> EVENT_EXCLUSIVE_PETS = Map.of(
        "halloween", Arrays.asList("CITROUILLE_MAUDITE", "FANTOME_ANTIQUE", "SORCIERE_MINIATURE"),
        "christmas", Arrays.asList("RENNE_MAGIQUE", "BONHOMME_NEIGE", "ESPRIT_NOEL"),
        "anniversary", Arrays.asList("PET_FONDATEUR", "DRAGON_DORE", "PHOENIX_ETERNEL")
    );

    public LimitedEventSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Démarre un événement
     */
    public void startEvent(String templateId) {
        EventTemplate template = EVENT_TEMPLATES.stream()
            .filter(t -> t.id().equals(templateId))
            .findFirst()
            .orElse(null);

        if (template == null) return;

        Instant now = Instant.now();
        activeEvent = new LimitedEvent(
            template.id(),
            template.name(),
            template.description(),
            template.type(),
            template.value(),
            now,
            now.plus(template.duration())
        );

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l════════════════════════════════");
        Bukkit.broadcastMessage("         " + template.name());
        Bukkit.broadcastMessage("§7" + template.description().replace("\n", "\n         §7"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e   Durée: §f" + formatDuration(template.duration()));
        Bukkit.broadcastMessage("§6§l════════════════════════════════");
        Bukkit.broadcastMessage("");

        // Son pour tous les joueurs
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    /**
     * Termine l'événement actif
     */
    public void endEvent() {
        if (activeEvent == null) return;

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l════════════════════════════════");
        Bukkit.broadcastMessage("         §c§lÉVÉNEMENT TERMINÉ!");
        Bukkit.broadcastMessage("         §7" + activeEvent.name() + " §7est fini.");
        Bukkit.broadcastMessage("§c§l════════════════════════════════");
        Bukkit.broadcastMessage("");

        activeEvent = null;
    }

    /**
     * Vérifie si un événement est actif
     */
    public boolean hasActiveEvent() {
        if (activeEvent == null) return false;

        // Vérifier si l'événement est expiré
        if (Instant.now().isAfter(activeEvent.endsAt())) {
            endEvent();
            return false;
        }
        return true;
    }

    /**
     * Obtient le bonus de chance actuel
     */
    public double getLuckBonus() {
        if (!hasActiveEvent()) return 0;

        return switch (activeEvent.type()) {
            case LUCK_BOOST, MEGA_BOOST -> activeEvent.value();
            default -> 0;
        };
    }

    /**
     * Obtient le bonus de rareté pour un type spécifique
     */
    public double getRarityBonus(PetRarity targetRarity) {
        if (!hasActiveEvent()) return 0;

        return switch (activeEvent.type()) {
            case RARITY_BOOST -> targetRarity.isAtLeast(PetRarity.LEGENDARY) ? activeEvent.value() : 0;
            case MYTHIC_BOOST -> targetRarity == PetRarity.MYTHIC ? activeEvent.value() : 0;
            case MEGA_BOOST -> activeEvent.value();
            default -> 0;
        };
    }

    /**
     * Obtient le multiplicateur de fragments
     */
    public double getFragmentMultiplier() {
        if (!hasActiveEvent()) return 1.0;

        return switch (activeEvent.type()) {
            case DOUBLE_FRAGMENTS -> activeEvent.value();
            case MEGA_BOOST -> activeEvent.value();
            default -> 1.0;
        };
    }

    /**
     * Obtient la réduction boutique
     */
    public double getShopDiscount() {
        if (!hasActiveEvent()) return 0;

        return activeEvent.type() == EventType.SHOP_DISCOUNT ? activeEvent.value() : 0;
    }

    /**
     * Vérifie si des oeufs spéciaux sont disponibles
     */
    public boolean hasSpecialEggs() {
        if (!hasActiveEvent()) return false;
        return activeEvent.type() == EventType.SPECIAL_EGG ||
               activeEvent.type() == EventType.MEGA_BOOST;
    }

    /**
     * Obtient les pets exclusifs de l'événement actuel
     */
    public List<String> getExclusivePets() {
        if (!hasActiveEvent()) return Collections.emptyList();
        return EVENT_EXCLUSIVE_PETS.getOrDefault(activeEvent.id(), Collections.emptyList());
    }

    /**
     * Obtient le temps restant de l'événement
     */
    public Duration getTimeRemaining() {
        if (!hasActiveEvent()) return Duration.ZERO;
        return Duration.between(Instant.now(), activeEvent.endsAt());
    }

    /**
     * Enregistre la participation d'un joueur à l'événement
     */
    public void recordParticipation(UUID playerUuid, int eggsOpened, int legendariesFound) {
        if (!hasActiveEvent()) return;

        Map<String, EventParticipation> playerParts =
            participations.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());

        EventParticipation participation = playerParts.computeIfAbsent(
            activeEvent.id(),
            k -> new EventParticipation(0, 0)
        );

        playerParts.put(activeEvent.id(), new EventParticipation(
            participation.eggsOpened() + eggsOpened,
            participation.legendariesFound() + legendariesFound
        ));
    }

    /**
     * Obtient les statistiques de participation d'un joueur
     */
    public EventParticipation getParticipation(UUID playerUuid, String eventId) {
        Map<String, EventParticipation> playerParts = participations.get(playerUuid);
        if (playerParts == null) return new EventParticipation(0, 0);
        return playerParts.getOrDefault(eventId, new EventParticipation(0, 0));
    }

    /**
     * Crée un message d'urgence FOMO
     */
    public String getFOMOMessage() {
        if (!hasActiveEvent()) return null;

        Duration remaining = getTimeRemaining();
        if (remaining.toMinutes() < 60) {
            return "§c§l⚠ " + activeEvent.name() + " §c§lse termine dans " +
                   remaining.toMinutes() + " minutes! §c§lDépêchez-vous!";
        } else if (remaining.toHours() < 6) {
            return "§6⏱ " + activeEvent.name() + " §6se termine dans " +
                   remaining.toHours() + " heures!";
        }
        return null;
    }

    /**
     * Obtient tous les templates d'événements
     */
    public List<EventTemplate> getEventTemplates() {
        return EVENT_TEMPLATES;
    }

    /**
     * Formate une durée lisiblement
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;

        if (days > 0) {
            return days + " jour" + (days > 1 ? "s" : "") + " " + hours + "h";
        } else {
            return hours + " heure" + (hours > 1 ? "s" : "");
        }
    }

    // ==================== RECORDS ET ENUMS ====================

    public enum EventType {
        LUCK_BOOST,         // Boost global de chance
        RARITY_BOOST,       // Boost sur raretés spécifiques
        MYTHIC_BOOST,       // Boost spécial mythique
        DOUBLE_FRAGMENTS,   // x2 fragments
        SHOP_DISCOUNT,      // Réduction boutique
        SPECIAL_EGG,        // Oeufs/pets spéciaux disponibles
        MEGA_BOOST          // Tout boosté
    }

    public record EventTemplate(
        String id,
        String name,
        String description,
        EventType type,
        double value,
        Duration duration
    ) {}

    public record LimitedEvent(
        String id,
        String name,
        String description,
        EventType type,
        double value,
        Instant startsAt,
        Instant endsAt
    ) {}

    public record EventParticipation(
        int eggsOpened,
        int legendariesFound
    ) {}
}
