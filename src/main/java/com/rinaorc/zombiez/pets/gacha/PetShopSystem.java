package com.rinaorc.zombiez.pets.gacha;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de boutique pour les Pets
 * Organisation claire : Points pour oeufs/conversion, Fragments pour offres spéciales
 */
public class PetShopSystem {

    private final ZombieZPlugin plugin;

    // Offres permanentes (oeufs + conversion fragments) - en POINTS
    @Getter
    private final List<ShopOffer> permanentOffers = new ArrayList<>();

    // Offres limitées dans le temps - en FRAGMENTS
    @Getter
    private final List<TimedOffer> timedOffers = new ArrayList<>();

    // Offres "first purchase" bonus - en FRAGMENTS
    @Getter
    private final List<FirstPurchaseOffer> firstPurchaseOffers = new ArrayList<>();

    // Tracking des achats des joueurs
    private final Map<UUID, Set<String>> playerPurchases = new ConcurrentHashMap<>();

    // Dernière rotation des offres
    private Instant lastRotation = Instant.now();
    private static final Duration ROTATION_INTERVAL = Duration.ofHours(8);

    public PetShopSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initializeOffers();
    }

    private void initializeOffers() {
        // ==================== ACHATS EN POINTS ====================
        // Oeufs et conversion points → fragments

        // --- OEUFS STANDARDS ---
        permanentOffers.add(new ShopOffer(
            "egg_standard_1", "§f🥚 Oeuf Standard",
            "1 oeuf standard\nToutes raretés possibles",
            EggType.STANDARD, 1, 0,
            500, CurrencyType.POINTS, 0
        ));

        permanentOffers.add(new ShopOffer(
            "egg_standard_10", "§f🥚 Pack 10 Standards",
            "10 oeufs + 1 bonus!\nÉconomisez 10%",
            EggType.STANDARD, 11, 0,
            4500, CurrencyType.POINTS, 10
        ));

        // --- OEUFS DE ZONE ---
        permanentOffers.add(new ShopOffer(
            "egg_zone_1", "§e🥚 Oeuf de Zone",
            "1 oeuf de zone\n§aRare minimum garanti!",
            EggType.ZONE, 1, 0,
            2000, CurrencyType.POINTS, 0
        ));

        permanentOffers.add(new ShopOffer(
            "egg_zone_5", "§e🥚 Pack 5 Zones",
            "5 oeufs + 1 bonus!\nÉconomisez 10%",
            EggType.ZONE, 6, 0,
            9000, CurrencyType.POINTS, 10
        ));

        // --- OEUFS ÉLITE ---
        permanentOffers.add(new ShopOffer(
            "egg_elite_1", "§d🥚 Oeuf Élite",
            "1 oeuf élite\n§dÉpique minimum garanti!",
            EggType.ELITE, 1, 0,
            5000, CurrencyType.POINTS, 0
        ));

        permanentOffers.add(new ShopOffer(
            "egg_elite_3", "§d🥚 Pack 3 Élite",
            "3 oeufs + 1 bonus!\nÉconomisez 10%",
            EggType.ELITE, 4, 0,
            13500, CurrencyType.POINTS, 10
        ));

        // --- OEUF LÉGENDAIRE ---
        permanentOffers.add(new ShopOffer(
            "egg_legendary_1", "§6🥚 Oeuf Légendaire",
            "1 oeuf légendaire\n§6§lLÉGENDAIRE GARANTI!",
            EggType.LEGENDARY, 1, 0,
            15000, CurrencyType.POINTS, 0
        ));

        // --- CONVERSION POINTS → FRAGMENTS ---
        permanentOffers.add(new ShopOffer(
            "fragments_100", "§d💎 100 Fragments",
            "Petit pack de fragments\nPour débuter",
            null, 0, 100,
            800, CurrencyType.POINTS, 0
        ));

        permanentOffers.add(new ShopOffer(
            "fragments_500", "§d💎 550 Fragments",
            "500 + 50 bonus!\n§a+10% gratuits",
            null, 0, 550,
            3500, CurrencyType.POINTS, 10
        ));

        permanentOffers.add(new ShopOffer(
            "fragments_2000", "§d💎 2400 Fragments",
            "2000 + 400 bonus!\n§a+20% gratuits",
            null, 0, 2400,
            12000, CurrencyType.POINTS, 20
        ));

        // ==================== ACHATS EN FRAGMENTS ====================
        // Offres first purchase - meilleur rapport qualité/prix

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_starter", "§a§lPack Débutant",
            "Le meilleur départ!",
            Arrays.asList(
                new RewardItem(EggType.STANDARD, 10),
                new RewardItem(EggType.ZONE, 3),
                new RewardItem(EggType.ELITE, 1)
            ),
            800, CurrencyType.FRAGMENTS, 75
        ));

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_elite", "§d§lPack Élite",
            "Pour les collectionneurs!",
            Arrays.asList(
                new RewardItem(EggType.ELITE, 5),
                new RewardItem(EggType.LEGENDARY, 1)
            ),
            2500, CurrencyType.FRAGMENTS, 70
        ));

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_legendary", "§6§lPack Légendaire",
            "L'ultime pack!",
            Arrays.asList(
                new RewardItem(EggType.LEGENDARY, 3),
                new RewardItem(EggType.ELITE, 5)
            ),
            6000, CurrencyType.FRAGMENTS, 65
        ));

        // Offres temporaires
        refreshTimedOffers();
    }

    /**
     * Rafraîchit les offres temporaires (en FRAGMENTS)
     */
    public void refreshTimedOffers() {
        timedOffers.clear();
        Instant now = Instant.now();

        // Hot Deal 1 - Oeuf Élite à prix réduit
        timedOffers.add(new TimedOffer(
            "hot_deal_elite", "§c🔥 Oeuf Élite -40%",
            "1 Oeuf Élite\n§d§lÉpique minimum!",
            EggType.ELITE, 1, 0,
            150, CurrencyType.FRAGMENTS, 40,
            now.plus(ROTATION_INTERVAL)
        ));

        // Hot Deal 2 - Pack rotatif selon le temps
        Random random = new Random(now.toEpochMilli() / ROTATION_INTERVAL.toMillis());
        EggType[] megaTypes = {EggType.ZONE, EggType.ELITE};
        EggType megaType = megaTypes[random.nextInt(megaTypes.length)];
        int megaCount = megaType == EggType.ELITE ? 3 : 5;
        int megaPrice = megaType == EggType.ELITE ? 350 : 300;

        timedOffers.add(new TimedOffer(
            "hot_deal_mega", "§6⭐ Mega Pack -50%",
            megaCount + "x " + megaType.getColoredName() + "\n§a§l-50% de réduction!",
            megaType, megaCount, 0,
            megaPrice, CurrencyType.FRAGMENTS, 50,
            now.plus(ROTATION_INTERVAL)
        ));

        // Hot Deal 3 - Oeuf Légendaire (rare deal)
        timedOffers.add(new TimedOffer(
            "hot_deal_legendary", "§6🔥 Légendaire -30%",
            "1 Oeuf Légendaire\n§6§lGARANTI LÉGENDAIRE!",
            EggType.LEGENDARY, 1, 0,
            700, CurrencyType.FRAGMENTS, 30,
            now.plus(ROTATION_INTERVAL)
        ));

        lastRotation = now;
    }

    /**
     * Vérifie et effectue la rotation des offres si nécessaire
     */
    public void checkRotation() {
        if (Instant.now().isAfter(lastRotation.plus(ROTATION_INTERVAL))) {
            refreshTimedOffers();
        }
    }

    /**
     * Achète une offre permanente
     */
    public PurchaseResult buyPermanentOffer(UUID playerUuid, String offerId) {
        ShopOffer offer = permanentOffers.stream()
            .filter(o -> o.id().equals(offerId))
            .findFirst()
            .orElse(null);

        if (offer == null) {
            return new PurchaseResult(false, "Offre introuvable!", null);
        }

        return processPurchase(playerUuid, offer.price(), offer.currency(),
            offer.eggType(), offer.eggCount(), offer.fragments());
    }

    /**
     * Achète une offre temporaire
     */
    public PurchaseResult buyTimedOffer(UUID playerUuid, String offerId) {
        checkRotation();

        TimedOffer offer = timedOffers.stream()
            .filter(o -> o.id().equals(offerId))
            .findFirst()
            .orElse(null);

        if (offer == null) {
            return new PurchaseResult(false, "Offre introuvable ou expirée!", null);
        }

        if (Instant.now().isAfter(offer.expiresAt())) {
            return new PurchaseResult(false, "Cette offre a expiré!", null);
        }

        return processPurchase(playerUuid, offer.price(), offer.currency(),
            offer.eggType(), offer.eggCount(), offer.fragments());
    }

    /**
     * Achète une offre first purchase
     */
    public PurchaseResult buyFirstPurchaseOffer(UUID playerUuid, String offerId) {
        // Vérifier si déjà acheté
        Set<String> purchases = playerPurchases.computeIfAbsent(playerUuid, k -> new HashSet<>());
        if (purchases.contains(offerId)) {
            return new PurchaseResult(false, "Vous avez déjà acheté cette offre!", null);
        }

        FirstPurchaseOffer offer = firstPurchaseOffers.stream()
            .filter(o -> o.id().equals(offerId))
            .findFirst()
            .orElse(null);

        if (offer == null) {
            return new PurchaseResult(false, "Offre introuvable!", null);
        }

        // Vérifier les fonds
        PlayerPetData petData = plugin.getPetManager().getPlayerData(playerUuid);
        if (petData == null) {
            return new PurchaseResult(false, "Données joueur introuvables!", null);
        }

        // Vérifier selon le type de monnaie
        boolean hasEnough;
        String currencyName;

        if (offer.currency() == CurrencyType.POINTS) {
            var playerData = plugin.getPlayerDataManager().getPlayer(playerUuid);
            if (playerData == null) {
                return new PurchaseResult(false, "Données joueur introuvables!", null);
            }
            hasEnough = playerData.hasPoints(offer.price());
            currencyName = "points";
            if (hasEnough) {
                playerData.removePoints(offer.price());
            }
        } else {
            hasEnough = petData.hasFragments(offer.price());
            currencyName = "fragments";
            if (hasEnough) {
                petData.removeFragments(offer.price());
            }
        }

        if (!hasEnough) {
            return new PurchaseResult(false, "Pas assez de " + currencyName + "! (Besoin: " + offer.price() + ")", null);
        }

        // Donner les récompenses
        StringBuilder rewards = new StringBuilder();
        for (RewardItem item : offer.rewards()) {
            if (item.eggType() != null) {
                petData.addEggs(item.eggType(), item.amount());
                rewards.append("§a+ ").append(item.amount()).append("x ")
                    .append(item.eggType().getColoredName()).append("\n");
            } else {
                petData.addFragments(item.amount());
                rewards.append("§a+ §d").append(item.amount()).append(" §7fragments\n");
            }
        }

        // Marquer comme acheté
        purchases.add(offerId);

        return new PurchaseResult(true, rewards.toString(), offer);
    }

    private PurchaseResult processPurchase(UUID playerUuid, int price, CurrencyType currency,
                                           EggType eggType, int eggCount, int fragments) {
        PlayerPetData petData = plugin.getPetManager().getPlayerData(playerUuid);
        if (petData == null) {
            return new PurchaseResult(false, "Données joueur introuvables!", null);
        }

        // Vérifier et retirer les fonds selon le type de monnaie
        boolean hasEnough;
        String currencyName;

        if (currency == CurrencyType.POINTS) {
            var playerData = plugin.getPlayerDataManager().getPlayer(playerUuid);
            if (playerData == null) {
                return new PurchaseResult(false, "Données joueur introuvables!", null);
            }
            hasEnough = playerData.hasPoints(price);
            currencyName = "points";
            if (hasEnough) {
                playerData.removePoints(price);
            }
        } else if (currency == CurrencyType.FRAGMENTS) {
            hasEnough = petData.hasFragments(price);
            currencyName = "fragments";
            if (hasEnough) {
                petData.removeFragments(price);
            }
        } else {
            return new PurchaseResult(false, "Type de monnaie non supporté!", null);
        }

        if (!hasEnough) {
            return new PurchaseResult(false, "Pas assez de " + currencyName + "! (Besoin: " + price + ")", null);
        }

        StringBuilder rewards = new StringBuilder();

        if (eggType != null && eggCount > 0) {
            petData.addEggs(eggType, eggCount);
            rewards.append("§a+ ").append(eggCount).append("x ")
                .append(eggType.getColoredName()).append("\n");
        }

        if (fragments > 0) {
            petData.addFragments(fragments);
            rewards.append("§a+ §d").append(fragments).append(" §7fragments\n");
        }

        return new PurchaseResult(true, rewards.toString(), null);
    }

    /**
     * Vérifie si une offre first purchase a été utilisée
     */
    public boolean hasUsedFirstPurchase(UUID playerUuid, String offerId) {
        Set<String> purchases = playerPurchases.get(playerUuid);
        return purchases != null && purchases.contains(offerId);
    }

    /**
     * Charge les achats d'un joueur
     */
    public void loadPlayerPurchases(UUID playerUuid, Set<String> purchases) {
        playerPurchases.put(playerUuid, new HashSet<>(purchases));
    }

    /**
     * Obtient les achats pour sauvegarde
     */
    public Set<String> getPlayerPurchases(UUID playerUuid) {
        return playerPurchases.getOrDefault(playerUuid, new HashSet<>());
    }

    /**
     * Obtient le temps restant pour une offre temporaire
     */
    public Duration getTimeRemaining(String offerId) {
        return timedOffers.stream()
            .filter(o -> o.id().equals(offerId))
            .findFirst()
            .map(o -> Duration.between(Instant.now(), o.expiresAt()))
            .orElse(Duration.ZERO);
    }

    // ==================== CLASSES INTERNES ====================

    public enum CurrencyType {
        POINTS,     // Points de jeu (zombies tués) - pour oeufs et conversion
        FRAGMENTS,  // Fragments de pet - pour offres spéciales
        PREMIUM     // Monnaie premium (réservé)
    }

    public record ShopOffer(
        String id,
        String name,
        String description,
        EggType eggType,
        int eggCount,
        int fragments,
        int price,
        CurrencyType currency,
        int discountPercent
    ) {}

    public record TimedOffer(
        String id,
        String name,
        String description,
        EggType eggType,
        int eggCount,
        int fragments,
        int price,
        CurrencyType currency,
        int discountPercent,
        Instant expiresAt
    ) {}

    public record FirstPurchaseOffer(
        String id,
        String name,
        String description,
        List<RewardItem> rewards,
        int price,
        CurrencyType currency,
        int discountPercent
    ) {}

    public record RewardItem(
        EggType eggType, // null = fragments
        int amount
    ) {}

    public record PurchaseResult(
        boolean success,
        String message,
        Object offer
    ) {}
}
