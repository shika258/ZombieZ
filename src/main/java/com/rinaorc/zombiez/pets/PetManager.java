package com.rinaorc.zombiez.pets;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.abilities.impl.PetAbilityRegistry;
import com.rinaorc.zombiez.pets.display.PetDisplayManager;
import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gestionnaire principal du système de Pets
 * Gère le cache, la persistance, les cooldowns et les ticks passifs
 */
public class PetManager {

    private final ZombieZPlugin plugin;

    // Cache des données de pets par joueur
    private final Cache<UUID, PlayerPetData> petDataCache;

    // Cache des cooldowns des capacités ultimes: Map<UUID, Map<PetType, prochaine activation timestamp>>
    private final Map<UUID, Map<PetType, Long>> abilityCooldowns = new ConcurrentHashMap<>();

    // Tâche d'activation automatique des ultimes
    private BukkitTask ultimateTickTask;

    // Registre des capacités
    @Getter
    private final PetAbilityRegistry abilityRegistry;

    // Gestionnaire d'affichage des pets
    @Getter
    private final PetDisplayManager displayManager;

    // Tâches planifiées
    private BukkitTask passiveTickTask;
    private BukkitTask autoSaveTask;

    public PetManager(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Cache avec expiration après 30 minutes d'inactivité
        this.petDataCache = Caffeine.newBuilder()
            .maximumSize(300)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((uuid, data, cause) -> {
                if (data != null && ((PlayerPetData) data).isDirty()) {
                    savePlayerDataAsync((UUID) uuid, (PlayerPetData) data);
                }
            })
            .build();

        // Initialiser le registre des capacités
        this.abilityRegistry = new PetAbilityRegistry(plugin);

        // Initialiser le gestionnaire d'affichage
        this.displayManager = new PetDisplayManager(plugin, this);

        // Créer les tables de base de données
        createTables();

        // Démarrer les tâches planifiées
        startTasks();

        plugin.log(Level.INFO, "§a✓ PetManager initialisé!");
    }

    // ==================== DONNÉES JOUEUR ====================

    /**
     * Charge les données de pet d'un joueur (async)
     */
    public CompletableFuture<PlayerPetData> loadPlayerData(UUID uuid) {
        // Vérifier le cache
        PlayerPetData cached = petDataCache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            PlayerPetData data = loadFromDatabase(uuid);
            if (data == null) {
                data = new PlayerPetData(uuid);
            }
            petDataCache.put(uuid, data);
            return data;
        });
    }

    /**
     * Obtient les données de pet d'un joueur (synchrone, cache uniquement)
     */
    public PlayerPetData getPlayerData(UUID uuid) {
        return petDataCache.getIfPresent(uuid);
    }

    /**
     * Obtient les données de pet d'un joueur (synchrone, avec chargement)
     */
    public PlayerPetData getOrLoadPlayerData(UUID uuid) {
        PlayerPetData data = petDataCache.getIfPresent(uuid);
        if (data == null) {
            data = loadFromDatabase(uuid);
            if (data == null) {
                data = new PlayerPetData(uuid);
            }
            petDataCache.put(uuid, data);
        }
        return data;
    }

    /**
     * Sauvegarde les données d'un joueur (async)
     */
    public void savePlayerDataAsync(UUID uuid, PlayerPetData data) {
        if (data == null || !data.isDirty()) return;

        CompletableFuture.runAsync(() -> {
            saveToDatabase(uuid, data);
            data.clearDirty();
        });
    }

    /**
     * Sauvegarde les données d'un joueur (sync)
     */
    public void savePlayerDataSync(UUID uuid, PlayerPetData data) {
        if (data == null) return;
        saveToDatabase(uuid, data);
        data.clearDirty();
    }

    /**
     * Sauvegarde toutes les données (sync) - pour shutdown
     */
    public void saveAllSync() {
        petDataCache.asMap().forEach(this::savePlayerDataSync);
    }

    /**
     * Sauvegarde toutes les données dirty (async) - pour auto-save
     */
    public void saveAllAsync() {
        petDataCache.asMap().forEach((uuid, data) -> {
            if (data.isDirty()) {
                savePlayerDataAsync(uuid, data);
            }
        });
    }

    // ==================== GESTION DES PETS ====================

    /**
     * Équipe un pet pour un joueur
     */
    public boolean equipPet(Player player, PetType type) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null) return false;

        if (!data.hasPet(type)) {
            player.sendMessage("§c[Pet] §7Vous ne possédez pas ce pet!");
            return false;
        }

        // Déséquiper l'ancien pet
        if (data.getEquippedPet() != null) {
            unequipPet(player);
        }

        // Équiper le nouveau
        data.equipPet(type);
        PetData petData = data.getPet(type);

        // Appliquer les effets passifs d'équipement
        PetAbility passive = abilityRegistry.getPassive(type);
        if (passive != null) {
            passive.onEquip(player, petData);
        }

        // Spawn l'entité visuelle si l'option est activée
        if (data.isShowPetEntity()) {
            displayManager.spawnPetDisplay(player, type);
        }

        player.sendMessage("§a[Pet] §7Vous avez équipé " + type.getColoredName() + "§7!");
        return true;
    }

    /**
     * Déséquipe le pet actuel
     */
    public void unequipPet(Player player) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedPet() == null) return;

        PetType oldType = data.getEquippedPet();
        PetData petData = data.getPet(oldType);

        // Retirer les effets passifs
        PetAbility passive = abilityRegistry.getPassive(oldType);
        if (passive != null && petData != null) {
            passive.onUnequip(player, petData);
        }

        // Retirer l'entité visuelle
        displayManager.removePetDisplay(player);

        data.unequipPet();
        player.sendMessage("§a[Pet] §7Vous avez déséquipé votre pet.");
    }

    /**
     * @deprecated Les capacités ultimes s'activent automatiquement maintenant
     */
    @Deprecated
    public boolean activateAbility(Player player) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedPet() == null) {
            player.sendMessage("§c[Pet] §7Aucun pet équipé!");
            return false;
        }

        PetType type = data.getEquippedPet();
        int cooldown = type.getUltimateCooldown();
        long remaining = getCooldownRemaining(player.getUniqueId(), type);

        if (remaining > 0) {
            player.sendMessage("§c[Pet] §7L'ultime s'activera automatiquement dans §e" + (remaining / 1000) + "s§7!");
        } else {
            player.sendMessage("§a[Pet] §7L'ultime §b" + type.getUltimateName() + " §7est prête et s'activera bientôt!");
        }
        player.sendMessage("§7[§eInfo§7] Les ultimes s'activent automatiquement toutes les §e" + cooldown + "s§7.");

        return false;
    }

    /**
     * Active automatiquement l'ultime d'un joueur (appelé par le système)
     */
    private void autoActivateUltimate(Player player) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedPet() == null) return;

        PetType type = data.getEquippedPet();
        PetData petData = data.getPet(type);
        PetAbility ultimate = abilityRegistry.getUltimate(type);

        if (ultimate == null) return;

        // Vérifier le cooldown
        if (isOnCooldown(player.getUniqueId(), type)) return;

        // Vérifier si l'ultime peut s'activer
        if (!ultimate.canAutoActivate(player, petData)) return;

        // Activer l'ultime
        if (ultimate.activate(player, petData)) {
            // Message au joueur si l'option est activée
            if (data.isShowAbilityMessages()) {
                player.sendMessage("§6§l[ULTIME] §e" + type.getUltimateName() + " §7activée!");
            }

            // Effet sonore si l'option est activée
            if (data.isPlayPetSounds()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
            }

            // Appliquer le cooldown pour la prochaine activation
            int cooldown = ultimate.getAdjustedCooldown(petData);
            setCooldown(player.getUniqueId(), type, cooldown * 1000L);
        }
    }

    // ==================== OEUFS ====================

    /**
     * Ouvre un oeuf et donne le pet (roll aléatoire)
     */
    public PetType openEgg(Player player, EggType eggType) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null) return null;

        // Vérifier le pity
        PetRarity pityGuarantee = data.checkPityGuarantee(eggType);

        // Tirer le pet
        double luckBonus = plugin.getPlayerDataManager().getPlayer(player).getLootLuckBonus();
        PetType pet = eggType.rollPet(luckBonus, pityGuarantee);

        // Utiliser la méthode commune
        return openEggWithResult(player, eggType, pet);
    }

    /**
     * Ouvre un oeuf avec un résultat pré-déterminé (pour l'animation qui pré-calcule le résultat)
     */
    public PetType openEggWithResult(Player player, EggType eggType, PetType predeterminedPet) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null) return null;

        if (!data.removeEgg(eggType)) {
            player.sendMessage("§c[Pet] §7Vous n'avez pas d'oeuf de ce type!");
            return null;
        }

        // Ajouter le pet pré-déterminé
        boolean isNew = data.addPet(predeterminedPet);

        // Mettre à jour les stats
        data.incrementEggsOpened();

        // Gérer le pity
        if (predeterminedPet.getRarity().isAtLeast(PetRarity.RARE)) {
            data.resetPity(eggType);
        } else {
            data.incrementPity(eggType);
        }

        // Annonces
        if (isNew) {
            player.sendMessage("§a[Pet] §7Nouveau pet obtenu: " + predeterminedPet.getColoredName() + "§7!");

            // Annonce serveur pour légendaires+
            if (predeterminedPet.getRarity().isAtLeast(PetRarity.LEGENDARY)) {
                Bukkit.broadcastMessage("§6§l[PET] §e" + player.getName() + " §7a obtenu " +
                    predeterminedPet.getColoredName() + "§7!");
            }
        } else {
            int fragments = predeterminedPet.getRarity().getFragmentsPerDuplicate();
            player.sendMessage("§a[Pet] §7Duplicata: " + predeterminedPet.getColoredName() + "§7! §7(+§d" + fragments + " fragments§7)");
        }

        return isNew ? predeterminedPet : null; // Retourne null si c'est un duplicata (pour le flag isNew)
    }

    /**
     * Donne un oeuf à un joueur
     */
    public void giveEgg(Player player, EggType type, int amount) {
        PlayerPetData data = getOrLoadPlayerData(player.getUniqueId());
        data.addEggs(type, amount);
        player.sendMessage("§a[Pet] §7Vous avez reçu §ex" + amount + " " + type.getColoredName() + "§7!");
    }

    // ==================== COOLDOWNS ====================

    private void setCooldown(UUID uuid, PetType type, long durationMs) {
        abilityCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(type, System.currentTimeMillis() + durationMs);
    }

    private boolean isOnCooldown(UUID uuid, PetType type) {
        Map<PetType, Long> cooldowns = abilityCooldowns.get(uuid);
        if (cooldowns == null) return false;

        Long expiry = cooldowns.get(type);
        if (expiry == null) return false;

        return System.currentTimeMillis() < expiry;
    }

    private long getCooldownRemaining(UUID uuid, PetType type) {
        Map<PetType, Long> cooldowns = abilityCooldowns.get(uuid);
        if (cooldowns == null) return 0;

        Long expiry = cooldowns.get(type);
        if (expiry == null) return 0;

        return Math.max(0, expiry - System.currentTimeMillis());
    }

    public int getCooldownRemainingSeconds(UUID uuid, PetType type) {
        return (int) (getCooldownRemaining(uuid, type) / 1000);
    }

    // ==================== BASE DE DONNÉES ====================

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS pet_data (
                uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                equipped_pet VARCHAR(64),
                fragments INT DEFAULT 0,
                total_eggs_opened INT DEFAULT 0,
                legendaries_obtained INT DEFAULT 0,
                mythics_obtained INT DEFAULT 0,
                total_fragments_earned BIGINT DEFAULT 0,
                last_equip_time BIGINT DEFAULT 0,
                show_pet_entity BOOLEAN DEFAULT TRUE,
                show_pet_particles BOOLEAN DEFAULT TRUE,
                show_ability_messages BOOLEAN DEFAULT TRUE,
                auto_equip_on_join BOOLEAN DEFAULT TRUE,
                play_pet_sounds BOOLEAN DEFAULT TRUE
            );

            CREATE TABLE IF NOT EXISTS pet_collection (
                uuid VARCHAR(36) NOT NULL,
                pet_type VARCHAR(64) NOT NULL,
                level INT DEFAULT 1,
                copies INT DEFAULT 1,
                star_power INT DEFAULT 0,
                is_favorite BOOLEAN DEFAULT FALSE,
                total_damage BIGINT DEFAULT 0,
                total_kills BIGINT DEFAULT 0,
                times_used INT DEFAULT 0,
                time_equipped BIGINT DEFAULT 0,
                obtained_at BIGINT,
                last_equipped_at BIGINT,
                PRIMARY KEY (uuid, pet_type)
            );

            CREATE TABLE IF NOT EXISTS pet_eggs (
                uuid VARCHAR(36) NOT NULL,
                egg_type VARCHAR(32) NOT NULL,
                quantity INT DEFAULT 0,
                PRIMARY KEY (uuid, egg_type)
            );

            CREATE TABLE IF NOT EXISTS pet_pity (
                uuid VARCHAR(36) NOT NULL,
                egg_type VARCHAR(32) NOT NULL,
                counter INT DEFAULT 0,
                PRIMARY KEY (uuid, egg_type)
            );
            """;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {

            for (String query : sql.split(";")) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query);
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur création tables pets: " + e.getMessage());
        }
    }

    private PlayerPetData loadFromDatabase(UUID uuid) {
        String uuidStr = uuid.toString();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Charger les données principales
            PlayerPetData data = new PlayerPetData(uuid);

            String mainSql = "SELECT * FROM pet_data WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(mainSql)) {
                stmt.setString(1, uuidStr);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String equipped = rs.getString("equipped_pet");
                    if (equipped != null && !equipped.isEmpty()) {
                        data.setEquippedPet(PetType.fromId(equipped));
                    }
                    data.setFragments(rs.getInt("fragments"));
                    data.setTotalEggsOpened(rs.getInt("total_eggs_opened"));
                    data.setLegendariesObtained(rs.getInt("legendaries_obtained"));
                    data.setMythicsObtained(rs.getInt("mythics_obtained"));
                    data.setTotalFragmentsEarned(rs.getLong("total_fragments_earned"));
                    data.setLastEquipTime(rs.getLong("last_equip_time"));
                    // Charger les options (avec gestion des colonnes manquantes)
                    try {
                        data.setShowPetEntity(rs.getBoolean("show_pet_entity"));
                        data.setShowPetParticles(rs.getBoolean("show_pet_particles"));
                        data.setShowAbilityMessages(rs.getBoolean("show_ability_messages"));
                        data.setAutoEquipOnJoin(rs.getBoolean("auto_equip_on_join"));
                        data.setPlayPetSounds(rs.getBoolean("play_pet_sounds"));
                    } catch (SQLException ignored) {
                        // Colonnes manquantes - garder les valeurs par défaut
                    }
                }
            }

            // Charger la collection
            String collSql = "SELECT * FROM pet_collection WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(collSql)) {
                stmt.setString(1, uuidStr);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    PetType type = PetType.fromId(rs.getString("pet_type"));
                    if (type == null) continue;

                    PetData petData = new PetData(type);
                    petData.setLevel(rs.getInt("level"));
                    petData.setCopies(rs.getInt("copies"));
                    petData.setStarPower(rs.getInt("star_power"));
                    petData.setFavorite(rs.getBoolean("is_favorite"));
                    petData.setTotalDamageDealt(rs.getLong("total_damage"));
                    petData.setTotalKills(rs.getLong("total_kills"));
                    petData.setTimesUsed(rs.getInt("times_used"));
                    petData.setTimeEquipped(rs.getLong("time_equipped"));
                    petData.setObtainedAt(rs.getLong("obtained_at"));
                    petData.setLastEquippedAt(rs.getLong("last_equipped_at"));

                    data.getOwnedPets().put(type, petData);
                }
            }

            // Charger les oeufs
            String eggsSql = "SELECT * FROM pet_eggs WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(eggsSql)) {
                stmt.setString(1, uuidStr);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    EggType type = EggType.fromName(rs.getString("egg_type"));
                    if (type != null) {
                        data.setEggCount(type, rs.getInt("quantity"));
                    }
                }
            }

            // Charger le pity
            String pitySql = "SELECT * FROM pet_pity WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(pitySql)) {
                stmt.setString(1, uuidStr);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    EggType type = EggType.fromName(rs.getString("egg_type"));
                    if (type != null) {
                        data.setPityCounter(type, rs.getInt("counter"));
                    }
                }
            }

            data.clearDirty();
            return data;

        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur chargement données pets pour " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    private void saveToDatabase(UUID uuid, PlayerPetData data) {
        String uuidStr = uuid.toString();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);

            // Sauvegarder les données principales
            String mainSql = """
                REPLACE INTO pet_data (uuid, equipped_pet, fragments, total_eggs_opened,
                    legendaries_obtained, mythics_obtained, total_fragments_earned, last_equip_time,
                    show_pet_entity, show_pet_particles, show_ability_messages, auto_equip_on_join, play_pet_sounds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(mainSql)) {
                stmt.setString(1, uuidStr);
                stmt.setString(2, data.getEquippedPet() != null ? data.getEquippedPet().getId() : null);
                stmt.setInt(3, data.getFragments());
                stmt.setInt(4, data.getTotalEggsOpened());
                stmt.setInt(5, data.getLegendariesObtained());
                stmt.setInt(6, data.getMythicsObtained());
                stmt.setLong(7, data.getTotalFragmentsEarned());
                stmt.setLong(8, data.getLastEquipTime());
                stmt.setBoolean(9, data.isShowPetEntity());
                stmt.setBoolean(10, data.isShowPetParticles());
                stmt.setBoolean(11, data.isShowAbilityMessages());
                stmt.setBoolean(12, data.isAutoEquipOnJoin());
                stmt.setBoolean(13, data.isPlayPetSounds());
                stmt.executeUpdate();
            }

            // Sauvegarder la collection
            String collSql = """
                REPLACE INTO pet_collection (uuid, pet_type, level, copies, star_power,
                    is_favorite, total_damage, total_kills, times_used, time_equipped,
                    obtained_at, last_equipped_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(collSql)) {
                for (PetData petData : data.getAllPets()) {
                    stmt.setString(1, uuidStr);
                    stmt.setString(2, petData.getType().getId());
                    stmt.setInt(3, petData.getLevel());
                    stmt.setInt(4, petData.getCopies());
                    stmt.setInt(5, petData.getStarPower());
                    stmt.setBoolean(6, petData.isFavorite());
                    stmt.setLong(7, petData.getTotalDamageDealt());
                    stmt.setLong(8, petData.getTotalKills());
                    stmt.setInt(9, petData.getTimesUsed());
                    stmt.setLong(10, petData.getTimeEquipped());
                    stmt.setLong(11, petData.getObtainedAt());
                    stmt.setLong(12, petData.getLastEquippedAt());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Sauvegarder les oeufs
            String eggsSql = "REPLACE INTO pet_eggs (uuid, egg_type, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(eggsSql)) {
                for (EggType type : EggType.values()) {
                    stmt.setString(1, uuidStr);
                    stmt.setString(2, type.name());
                    stmt.setInt(3, data.getEggCount(type));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Sauvegarder le pity
            String pitySql = "REPLACE INTO pet_pity (uuid, egg_type, counter) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(pitySql)) {
                for (EggType type : EggType.values()) {
                    stmt.setString(1, uuidStr);
                    stmt.setString(2, type.name());
                    stmt.setInt(3, data.getPityCounter(type));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur sauvegarde données pets pour " + uuid + ": " + e.getMessage());
        }
    }

    // ==================== TÂCHES PLANIFIÉES ====================

    private void startTasks() {
        // Tick des capacités passives toutes les 1 seconde
        passiveTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tickPassive(player);
            }
        }, 20L, 20L);

        // Tick des capacités ultimes toutes les 0.5 seconde (vérification rapide)
        ultimateTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                autoActivateUltimate(player);
            }
        }, 10L, 10L);

        // Auto-save toutes les 5 minutes
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            this::saveAllAsync, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void tickPassive(Player player) {
        PlayerPetData data = getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedPet() == null) return;

        PetType type = data.getEquippedPet();
        PetData petData = data.getPet(type);
        if (petData == null) return;

        PetAbility passive = abilityRegistry.getPassive(type);
        if (passive != null) {
            passive.applyPassive(player, petData);
        }

        // Mettre à jour l'affichage
        displayManager.updatePetDisplay(player);
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        // Arrêter les tâches
        if (passiveTickTask != null) {
            passiveTickTask.cancel();
        }
        if (ultimateTickTask != null) {
            ultimateTickTask.cancel();
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // Retirer tous les affichages
        displayManager.removeAllDisplays();

        // Sauvegarder toutes les données
        saveAllSync();

        plugin.log(Level.INFO, "§a✓ PetManager arrêté proprement.");
    }

    // ==================== UTILITAIRES ====================

    /**
     * Appelé quand un joueur se connecte
     */
    public void onPlayerJoin(Player player) {
        loadPlayerData(player.getUniqueId()).thenAccept(data -> {
            if (data.getEquippedPet() != null && data.isAutoEquipOnJoin()) {
                // Réactiver le pet
                Bukkit.getScheduler().runTask(plugin, () -> {
                    PetType type = data.getEquippedPet();
                    PetData petData = data.getPet(type);

                    PetAbility passive = abilityRegistry.getPassive(type);
                    if (passive != null) {
                        passive.onEquip(player, petData);
                    }

                    // Respecter l'option d'affichage de l'entité
                    if (data.isShowPetEntity()) {
                        displayManager.spawnPetDisplay(player, type);
                    }
                });
            }
        });
    }

    /**
     * Appelé quand un joueur se déconnecte
     */
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        // Retirer l'affichage
        displayManager.removePetDisplay(player);

        // Sauvegarder les données
        PlayerPetData data = petDataCache.getIfPresent(uuid);
        if (data != null) {
            // Mettre à jour le temps équipé
            if (data.getEquippedPet() != null) {
                PetData petData = data.getPet(data.getEquippedPet());
                if (petData != null && data.getLastEquipTime() > 0) {
                    long timeEquipped = System.currentTimeMillis() - data.getLastEquipTime();
                    petData.addEquippedTime(timeEquipped);
                }
            }

            savePlayerDataAsync(uuid, data);
        }

        // Nettoyer les cooldowns
        abilityCooldowns.remove(uuid);
    }

    /**
     * Donne un pet directement à un joueur (admin)
     */
    public void givePet(Player player, PetType type, int level, int copies) {
        PlayerPetData data = getOrLoadPlayerData(player.getUniqueId());

        if (data.hasPet(type)) {
            PetData petData = data.getPet(type);
            petData.addCopies(copies);
            petData.setLevel(level);
        } else {
            PetData petData = new PetData(type);
            petData.setLevel(level);
            petData.setCopies(copies);
            data.getOwnedPets().put(type, petData);
        }

        data.markDirty();
        player.sendMessage("§a[Pet Admin] §7Pet " + type.getColoredName() + " §7ajouté (Lv." + level + ", " + copies + " copies)!");
    }
}
