package com.rinaorc.zombiez.pets;

import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données des Pets d'un joueur
 * Gère la collection, les fragments, les oeufs et les statistiques
 */
@Data
public class PlayerPetData {

    // UUID du joueur
    @Getter
    private final UUID playerUuid;

    // Pets possédés: Map<PetType, PetData>
    private final Map<PetType, PetData> ownedPets = new ConcurrentHashMap<>();

    // Pet actuellement équipé
    @Setter
    private volatile PetType equippedPet = null;

    // Fragments
    private final AtomicInteger petFragments = new AtomicInteger(0);

    // Oeufs en attente: Map<EggType, quantité>
    private final Map<EggType, AtomicInteger> pendingEggs = new ConcurrentHashMap<>();

    // Compteurs Pity: Map<EggType, compteur>
    private final Map<EggType, AtomicInteger> pityCounters = new ConcurrentHashMap<>();

    // Statistiques globales
    private final AtomicInteger totalEggsOpened = new AtomicInteger(0);
    private final AtomicInteger legendariesObtained = new AtomicInteger(0);
    private final AtomicInteger mythicsObtained = new AtomicInteger(0);
    private final AtomicInteger exaltedObtained = new AtomicInteger(0);
    private final AtomicLong totalFragmentsEarned = new AtomicLong(0);

    // Timestamp dernière équipement
    @Setter
    private long lastEquipTime = 0;

    // Options du joueur
    @Getter @Setter
    private boolean showPetEntity = true;
    @Getter @Setter
    private boolean showPetParticles = true;
    @Getter @Setter
    private boolean showAbilityMessages = true;
    @Getter @Setter
    private boolean autoEquipOnJoin = true;
    @Getter @Setter
    private boolean playPetSounds = true;

    // Flag dirty
    private final transient AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerPetData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        // Initialiser les compteurs pity
        for (EggType type : EggType.values()) {
            pityCounters.put(type, new AtomicInteger(0));
            pendingEggs.put(type, new AtomicInteger(0));
        }
    }

    // ==================== PETS ====================

    /**
     * Vérifie si le joueur possède un pet
     */
    public boolean hasPet(PetType type) {
        return ownedPets.containsKey(type);
    }

    /**
     * Obtient les données d'un pet
     */
    public PetData getPet(PetType type) {
        return ownedPets.get(type);
    }

    /**
     * Ajoute un nouveau pet ou une copie
     * @return true si c'est un nouveau pet, false si duplicata
     */
    public boolean addPet(PetType type) {
        markDirty();

        if (ownedPets.containsKey(type)) {
            // Duplicata - ajouter une copie
            PetData data = ownedPets.get(type);
            data.addCopies(1);

            // Donner des fragments bonus
            int fragments = type.getRarity().getFragmentsPerDuplicate();
            addFragments(fragments);

            return false;
        } else {
            // Nouveau pet
            PetData data = new PetData(type);
            ownedPets.put(type, data);

            // Mettre à jour les stats de rareté
            if (type.getRarity() == PetRarity.LEGENDARY) {
                legendariesObtained.incrementAndGet();
            } else if (type.getRarity() == PetRarity.MYTHIC) {
                mythicsObtained.incrementAndGet();
            } else if (type.getRarity() == PetRarity.EXALTED) {
                exaltedObtained.incrementAndGet();
            }

            return true;
        }
    }

    /**
     * Équipe un pet
     */
    public boolean equipPet(PetType type) {
        if (!ownedPets.containsKey(type)) return false;

        // Mettre à jour le temps équipé de l'ancien pet
        if (equippedPet != null && ownedPets.containsKey(equippedPet)) {
            PetData oldPet = ownedPets.get(equippedPet);
            long timeEquipped = System.currentTimeMillis() - lastEquipTime;
            oldPet.addEquippedTime(timeEquipped);
        }

        equippedPet = type;
        lastEquipTime = System.currentTimeMillis();
        ownedPets.get(type).incrementUsage();
        markDirty();
        return true;
    }

    /**
     * Déséquipe le pet actuel
     */
    public void unequipPet() {
        if (equippedPet != null && ownedPets.containsKey(equippedPet)) {
            PetData pet = ownedPets.get(equippedPet);
            long timeEquipped = System.currentTimeMillis() - lastEquipTime;
            pet.addEquippedTime(timeEquipped);
        }
        equippedPet = null;
        lastEquipTime = 0;
        markDirty();
    }

    /**
     * Obtient le pet actuellement équipé
     */
    public PetData getEquippedPetData() {
        if (equippedPet == null) return null;
        return ownedPets.get(equippedPet);
    }

    /**
     * Obtient tous les pets possédés
     */
    public Collection<PetData> getAllPets() {
        return Collections.unmodifiableCollection(ownedPets.values());
    }

    /**
     * Obtient le nombre de pets possédés
     */
    public int getPetCount() {
        return ownedPets.size();
    }

    /**
     * Obtient le nombre de pets par rareté
     */
    public int getPetCountByRarity(PetRarity rarity) {
        return (int) ownedPets.values().stream()
            .filter(p -> p.getType().getRarity() == rarity)
            .count();
    }

    // ==================== FRAGMENTS ====================

    /**
     * Obtient le nombre de fragments
     */
    public int getFragments() {
        return petFragments.get();
    }

    /**
     * Ajoute des fragments
     */
    public void addFragments(int amount) {
        petFragments.addAndGet(amount);
        totalFragmentsEarned.addAndGet(amount);
        markDirty();
    }

    /**
     * Retire des fragments
     * @return true si succès
     */
    public boolean removeFragments(int amount) {
        int current, next;
        do {
            current = petFragments.get();
            if (current < amount) return false;
            next = current - amount;
        } while (!petFragments.compareAndSet(current, next));
        markDirty();
        return true;
    }

    /**
     * Vérifie si le joueur a assez de fragments
     */
    public boolean hasFragments(int amount) {
        return petFragments.get() >= amount;
    }

    // ==================== OEUFS ====================

    /**
     * Obtient le nombre d'oeufs d'un type
     */
    public int getEggCount(EggType type) {
        AtomicInteger count = pendingEggs.get(type);
        return count != null ? count.get() : 0;
    }

    /**
     * Ajoute des oeufs
     */
    public void addEggs(EggType type, int amount) {
        pendingEggs.computeIfAbsent(type, k -> new AtomicInteger(0)).addAndGet(amount);
        markDirty();
    }

    /**
     * Retire un oeuf
     * @return true si succès
     */
    public boolean removeEgg(EggType type) {
        AtomicInteger count = pendingEggs.get(type);
        if (count == null) return false;

        int current, next;
        do {
            current = count.get();
            if (current <= 0) return false;
            next = current - 1;
        } while (!count.compareAndSet(current, next));
        markDirty();
        return true;
    }

    /**
     * Obtient le total d'oeufs
     */
    public int getTotalEggs() {
        return pendingEggs.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
    }

    /**
     * Incrémente le compteur d'oeufs ouverts
     */
    public void incrementEggsOpened() {
        totalEggsOpened.incrementAndGet();
        markDirty();
    }

    // ==================== PITY SYSTEM ====================

    /**
     * Obtient le compteur pity pour un type d'oeuf
     */
    public int getPityCounter(EggType type) {
        AtomicInteger counter = pityCounters.get(type);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Incrémente le compteur pity
     */
    public void incrementPity(EggType type) {
        pityCounters.computeIfAbsent(type, k -> new AtomicInteger(0)).incrementAndGet();
        markDirty();
    }

    /**
     * Réinitialise le compteur pity
     */
    public void resetPity(EggType type) {
        AtomicInteger counter = pityCounters.get(type);
        if (counter != null) {
            counter.set(0);
            markDirty();
        }
    }

    /**
     * Vérifie si le pity garantit une rareté minimum
     * Chaque type d'oeuf a ses propres seuils de garantie
     */
    public PetRarity checkPityGuarantee(EggType eggType) {
        int pity = getPityCounter(eggType);

        return switch (eggType) {
            case STANDARD -> {
                // 50 → Rare, 100 → Épique, 200 → Légendaire
                if (pity >= 200) yield PetRarity.LEGENDARY;
                if (pity >= 100) yield PetRarity.EPIC;
                if (pity >= 50) yield PetRarity.RARE;
                yield null;
            }
            case ZONE -> {
                // 30 → Épique, 75 → Légendaire
                if (pity >= 75) yield PetRarity.LEGENDARY;
                if (pity >= 30) yield PetRarity.EPIC;
                yield null;
            }
            case ELITE -> {
                // 20 → Légendaire, 50 → Mythique
                if (pity >= 50) yield PetRarity.MYTHIC;
                if (pity >= 20) yield PetRarity.LEGENDARY;
                yield null;
            }
            case LEGENDARY -> {
                // 25 → Mythique (l'oeuf garantit déjà Légendaire)
                if (pity >= 25) yield PetRarity.MYTHIC;
                yield null;
            }
            case MYTHIC -> {
                // L'oeuf Mythique garantit déjà Mythique, pas de pity nécessaire
                yield null;
            }
        };
    }

    // ==================== STATISTIQUES ====================

    public int getTotalEggsOpened() {
        return totalEggsOpened.get();
    }

    public int getLegendariesObtained() {
        return legendariesObtained.get();
    }

    public int getMythicsObtained() {
        return mythicsObtained.get();
    }

    public int getExaltedObtained() {
        return exaltedObtained.get();
    }

    public long getTotalFragmentsEarned() {
        return totalFragmentsEarned.get();
    }

    /**
     * Calcule le pourcentage de complétion de la collection
     */
    public double getCollectionCompletion() {
        int total = PetType.values().length;
        return (ownedPets.size() * 100.0) / total;
    }

    /**
     * Obtient le nombre de pets max level
     */
    public int getMaxLevelPetCount() {
        return (int) ownedPets.values().stream()
            .filter(p -> p.getLevel() >= p.getType().getRarity().getMaxLevel())
            .count();
    }

    // ==================== SETTERS POUR BDD ====================

    public void setFragments(int amount) {
        petFragments.set(amount);
    }

    public void setTotalEggsOpened(int count) {
        totalEggsOpened.set(count);
    }

    public void setLegendariesObtained(int count) {
        legendariesObtained.set(count);
    }

    public void setMythicsObtained(int count) {
        mythicsObtained.set(count);
    }

    public void setExaltedObtained(int count) {
        exaltedObtained.set(count);
    }

    public void setTotalFragmentsEarned(long amount) {
        totalFragmentsEarned.set(amount);
    }

    public void setEggCount(EggType type, int count) {
        pendingEggs.computeIfAbsent(type, k -> new AtomicInteger(0)).set(count);
    }

    public void setPityCounter(EggType type, int count) {
        pityCounters.computeIfAbsent(type, k -> new AtomicInteger(0)).set(count);
    }

    // ==================== DIRTY FLAG ====================

    public void markDirty() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void clearDirty() {
        dirty.set(false);
        // Clear dirty sur tous les pets aussi
        ownedPets.values().forEach(PetData::clearDirty);
    }
}
