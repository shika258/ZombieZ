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
 * Offres, bundles, deals limités dans le temps
 */
public class PetShopSystem {

    private final ZombieZPlugin plugin;

    // Offres permanentes
    @Getter
    private final List<ShopOffer> permanentOffers = new ArrayList<>();

    // Offres limitées dans le temps (rotatives)
    @Getter
    private final List<TimedOffer> timedOffers = new ArrayList<>();

    // Offres "first purchase" bonus (une seule fois)
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
        // ==================== OFFRES PERMANENTES ====================

        // Oeufs individuels
        permanentOffers.add(new ShopOffer(
            "egg_standard_1", "§fOeuf Standard",
            "§71 oeuf standard", EggType.STANDARD, 1, 0,
            500, CurrencyType.POINTS, 0, false
        ));

        permanentOffers.add(new ShopOffer(
            "egg_standard_10", "§fPack 10 Oeufs Standards",
            "§7+1 oeuf bonus!", EggType.STANDARD, 11, 0,
            4500, CurrencyType.POINTS, 10, false // 10% réduction
        ));

        permanentOffers.add(new ShopOffer(
            "egg_zone_1", "§eOeuf de Zone",
            "§7Rare minimum garanti", EggType.ZONE, 1, 0,
            2000, CurrencyType.POINTS, 0, false
        ));

        permanentOffers.add(new ShopOffer(
            "egg_zone_5", "§ePack 5 Oeufs de Zone",
            "§7+1 oeuf bonus!", EggType.ZONE, 6, 0,
            9000, CurrencyType.POINTS, 10, false
        ));

        permanentOffers.add(new ShopOffer(
            "egg_elite_1", "§dOeuf Élite",
            "§7Épique minimum garanti", EggType.ELITE, 1, 0,
            5000, CurrencyType.POINTS, 0, false
        ));

        permanentOffers.add(new ShopOffer(
            "egg_elite_3", "§dPack 3 Oeufs Élite",
            "§7+1 oeuf bonus!", EggType.ELITE, 4, 0,
            13500, CurrencyType.POINTS, 10, false
        ));

        permanentOffers.add(new ShopOffer(
            "egg_legendary_1", "§6Oeuf Légendaire",
            "§7Légendaire garanti!", EggType.LEGENDARY, 1, 0,
            15000, CurrencyType.POINTS, 0, false
        ));

        // Packs de fragments
        permanentOffers.add(new ShopOffer(
            "fragments_100", "§ePetit Sac de Fragments",
            "§7100 fragments", null, 0, 100,
            800, CurrencyType.POINTS, 0, false
        ));

        permanentOffers.add(new ShopOffer(
            "fragments_500", "§6Sac de Fragments",
            "§7500 fragments + 50 bonus", null, 0, 550,
            3500, CurrencyType.POINTS, 12, false
        ));

        permanentOffers.add(new ShopOffer(
            "fragments_2000", "§c§lCoffre de Fragments",
            "§72000 fragments + 400 bonus!", null, 0, 2400,
            12000, CurrencyType.POINTS, 20, false
        ));

        // ==================== OFFRES PREMIERE ACHAT ====================
        // Double valeur pour le premier achat (conversion $$ -> points implicite)

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_starter", "§a§lPack Débutant",
            "§7Le meilleur départ!\n§e§lUNE SEULE FOIS!",
            Arrays.asList(
                new RewardItem(EggType.STANDARD, 10),
                new RewardItem(EggType.ZONE, 3),
                new RewardItem(EggType.ELITE, 1),
                new RewardItem(null, 500) // fragments
            ),
            2000, CurrencyType.POINTS, 75 // 75% de réduction affichée
        ));

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_elite", "§d§lPack Élite",
            "§7Pour les collectionneurs!\n§e§lUNE SEULE FOIS!",
            Arrays.asList(
                new RewardItem(EggType.ELITE, 5),
                new RewardItem(EggType.LEGENDARY, 1),
                new RewardItem(null, 1000)
            ),
            8000, CurrencyType.POINTS, 70
        ));

        firstPurchaseOffers.add(new FirstPurchaseOffer(
            "first_legendary", "§6§lPack Légendaire",
            "§7L'ultime pack!\n§c§lEXCLUSIF!",
            Arrays.asList(
                new RewardItem(EggType.LEGENDARY, 3),
                new RewardItem(EggType.ELITE, 5),
                new RewardItem(null, 3000)
            ),
            25000, CurrencyType.POINTS, 65
        ));

        // ==================== OFFRES TEMPORAIRES (EXEMPLES) ====================
        refreshTimedOffers();
    }

    /**
     * Rafraîchit les offres temporaires
     */
    public void refreshTimedOffers() {
        timedOffers.clear();
        Instant now = Instant.now();

        // Hot Deal - Change toutes les 8 heures
        timedOffers.add(new TimedOffer(
            "hot_deal_1", "§c§l🔥 HOT DEAL!",
            "§7Oeuf Élite à -40%!\n§c§lTEMPS LIMITÉ",
            EggType.ELITE, 1, 0,
            3000, CurrencyType.POINTS, 40,
            now.plus(ROTATION_INTERVAL)
        ));

        // Mega Pack rotatif
        Random random = new Random(now.toEpochMilli() / ROTATION_INTERVAL.toMillis());
        EggType[] megaTypes = {EggType.ZONE, EggType.ELITE, EggType.LEGENDARY};
        EggType megaType = megaTypes[random.nextInt(megaTypes.length)];
        int megaCount = megaType == EggType.LEGENDARY ? 2 : (megaType == EggType.ELITE ? 4 : 8);
        int megaPrice = megaType.getPointsCost() * megaCount / 2; // 50% off

        timedOffers.add(new TimedOffer(
            "mega_pack", "§6§l⭐ MEGA PACK!",
            "§7" + megaCount + "x " + megaType.getColoredName() + "\n§a§l-50%!",
            megaType, megaCount, megaCount * 50, // bonus fragments
            megaPrice, CurrencyType.POINTS, 50,
            now.plus(ROTATION_INTERVAL)
        ));

        // Bundle du jour
        timedOffers.add(new TimedOffer(
            "daily_bundle", "§e§l📦 Bundle du Jour",
            "§7Un peu de tout!",
            null, 0, 300, // 300 fragments
            1500, CurrencyType.POINTS, 25,
            now.plus(Duration.ofHours(24))
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

        // Pour l'instant, on utilise les fragments comme monnaie
        if (!petData.hasFragments(offer.price())) {
            return new PurchaseResult(false, "Pas assez de fragments! (Besoin: " + offer.price() + ")", null);
        }

        // Effectuer l'achat
        petData.removeFragments(offer.price());

        // Donner les récompenses
        StringBuilder rewards = new StringBuilder();
        for (RewardItem item : offer.rewards()) {
            if (item.eggType() != null) {
                petData.addEggs(item.eggType(), item.amount());
                rewards.append("§a+ ").append(item.amount()).append("x ")
                    .append(item.eggType().getColoredName()).append("\n");
            } else {
                petData.addFragments(item.amount());
                rewards.append("§a+ §e").append(item.amount()).append(" §7fragments\n");
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

        // Vérifier les fonds (pour l'instant juste les fragments/points)
        if (!petData.hasFragments(price)) {
            return new PurchaseResult(false, "Pas assez de " +
                (currency == CurrencyType.POINTS ? "points" : "fragments") +
                "! (Besoin: " + price + ")", null);
        }

        // Effectuer l'achat
        petData.removeFragments(price);

        StringBuilder rewards = new StringBuilder();

        if (eggType != null && eggCount > 0) {
            petData.addEggs(eggType, eggCount);
            rewards.append("§a+ ").append(eggCount).append("x ")
                .append(eggType.getColoredName()).append("\n");
        }

        if (fragments > 0) {
            petData.addFragments(fragments);
            rewards.append("§a+ §e").append(fragments).append(" §7fragments\n");
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
        POINTS,     // Points de jeu (zombies tués)
        FRAGMENTS,  // Fragments de pet
        PREMIUM     // Monnaie premium (si implémentée)
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
        int discountPercent,
        boolean featured
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
