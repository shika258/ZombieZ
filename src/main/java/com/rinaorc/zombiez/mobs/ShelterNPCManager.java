package com.rinaorc.zombiez.mobs;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Refuge;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des PNJ survivants dans les refuges
 * Spawn des villageois et wandering traders pour habiller les zones de refuge
 * et créer de l'ambiance avec des phrases de lore
 */
public class ShelterNPCManager implements Listener {

    private final ZombieZPlugin plugin;

    // Tracking des NPCs actifs par refuge (refugeId -> Set<UUID>)
    private final Map<Integer, Set<UUID>> npcsByRefuge;

    // Données des NPCs (UUID -> NPCData)
    private final Map<UUID, NPCData> npcData;

    // Configuration
    private static final int MAX_NPCS_PER_REFUGE = 5;
    private static final int SPAWN_CHECK_INTERVAL_TICKS = 600; // 30 secondes
    private static final double SPAWN_CHANCE = 0.35; // 35% de chance par check
    private static final double PLAYER_NEARBY_RADIUS = 48.0;
    private static final long INTERACTION_COOLDOWN_MS = 3000; // 3 secondes entre chaque interaction

    private final Random random = new Random();

    // Cooldown des interactions par joueur (UUID joueur -> timestamp)
    private final Map<UUID, Long> interactionCooldowns = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // NOMS DES SURVIVANTS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String[] MALE_NAMES = {
        "Jacques", "Henri", "Pierre", "Jean", "Michel", "André", "François",
        "Louis", "Paul", "Marcel", "Émile", "René", "Georges", "Robert",
        "Bernard", "Claude", "Daniel", "Alain", "Yves", "Gérard", "Luc",
        "Marc", "Simon", "Victor", "Hugo", "Léon", "Arthur", "Gaston",
        "Édouard", "Charles", "Antoine", "Nicolas", "Maxime", "Thomas"
    };

    private static final String[] FEMALE_NAMES = {
        "Marie", "Jeanne", "Marguerite", "Hélène", "Suzanne", "Madeleine",
        "Germaine", "Louise", "Yvonne", "Odette", "Simone", "Paulette",
        "Denise", "Jacqueline", "Michèle", "Françoise", "Monique", "Nicole",
        "Claire", "Sophie", "Anne", "Catherine", "Isabelle", "Lucie",
        "Élise", "Charlotte", "Emma", "Léonie", "Alice", "Juliette"
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // PHRASES DE LORE (Ambiance post-apocalyptique)
    // ═══════════════════════════════════════════════════════════════════════════

    @Getter
    private static final String[][] LORE_PHRASES = {
        // Phrases générales sur la survie
        {
            "§7\"J'ai vu tant de choses... des horreurs que personne ne devrait voir.\"",
            "§7\"Avant l'épidémie, j'étais §e%PROFESSION%§7. Maintenant, je survis.\"",
            "§7\"Chaque jour est un miracle. Chaque nuit, un cauchemar.\"",
            "§7\"Les murs de ce refuge sont tout ce qui nous sépare de l'enfer.\"",
            "§7\"J'ai perdu ma famille au début... Je refuse de perdre espoir.\"",
        },
        // Phrases sur le danger
        {
            "§7\"Ne t'éloigne pas trop des refuges, les zombies sont partout.\"",
            "§7\"J'ai entendu dire que les zombies au nord sont... différents.\"",
            "§7\"La nuit, ils sont plus nombreux. Fais attention.\"",
            "§7\"Certains zombies peuvent courir. §cCourir§7. Tu imagines ?\"",
            "§7\"Plus tu t'enfonces vers le nord, plus ils sont dangereux.\"",
        },
        // Phrases de conseil
        {
            "§7\"Équipe-toi bien avant de sortir. C'est une jungle là-dehors.\"",
            "§7\"Les autres survivants parlent d'une §eOrigine§7 au nord...\"",
            "§7\"Garde toujours de la nourriture sur toi. La faim tue autant que les morts.\"",
            "§7\"Si tu trouves des armes, ne les gaspille pas.\"",
            "§7\"Les refuges sont nos seuls havres de paix. Protège-les.\"",
        },
        // Phrases nostalgiques
        {
            "§7\"Tu te souviens de l'époque où on pouvait... vivre normalement ?\"",
            "§7\"Ma maison était quelque part par là... avant tout ça.\"",
            "§7\"Parfois je me demande s'il y a d'autres survivants ailleurs.\"",
            "§7\"Les enfants d'aujourd'hui ne connaîtront jamais le monde d'avant.\"",
            "§7\"J'avais une vie, un travail, une famille... Tout a disparu.\"",
        },
        // Phrases d'espoir
        {
            "§7\"Tant qu'il y a des survivants, il y a de l'espoir.\"",
            "§7\"Un jour, on reprendra ce monde. J'en suis sûr.\"",
            "§7\"Des gens comme toi nous donnent du courage. Merci.\"",
            "§7\"Ensemble, on est plus forts. N'oublie jamais ça.\"",
            "§7\"Je crois qu'on peut reconstruire. Un jour...\"",
        },
        // Phrases mystérieuses
        {
            "§7\"J'ai entendu des rumeurs sur l'§cOrigine§7 du virus...\"",
            "§7\"Certains disent que tout a commencé dans la zone 50...\"",
            "§7\"Il paraît qu'il y a des §dsecrets§7 cachés dans les ruines.\"",
            "§7\"Méfie-toi des ombres. Elles cachent plus que des zombies.\"",
            "§7\"Quelque chose de §cpuissant§7 se cache au nord. Je le sens.\"",
        },
        // Phrases sur le refuge
        {
            "§7\"Ce refuge est notre maison maintenant. On le défendra.\"",
            "§7\"Les murs tiennent bon pour l'instant. Espérons que ça dure.\"",
            "§7\"Ici, au moins, on peut dormir tranquille. Presque.\"",
            "§7\"Chaque refuge sauvé est une victoire contre les ténèbres.\"",
            "§7\"Bienvenue, voyageur. Repose-toi un peu.\"",
        }
    };

    private static final String[] PROFESSIONS_BEFORE = {
        "boulanger", "médecin", "professeur", "ingénieur", "fermier",
        "mécanicien", "cuisinier", "musicien", "artiste", "policier",
        "pompier", "infirmier", "architecte", "électricien", "jardinier",
        "bibliothécaire", "comptable", "avocat", "journaliste", "scientifique"
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // TYPES DE NPCs
    // ═══════════════════════════════════════════════════════════════════════════

    public enum NPCType {
        VILLAGER_ARMORER("§6Armurier", Villager.Profession.ARMORER),
        VILLAGER_BUTCHER("§c Boucher", Villager.Profession.BUTCHER),
        VILLAGER_FARMER("§a Fermier", Villager.Profession.FARMER),
        VILLAGER_LIBRARIAN("§f Bibliothécaire", Villager.Profession.LIBRARIAN),
        VILLAGER_CLERIC("§5 Guérisseur", Villager.Profession.CLERIC),
        VILLAGER_TOOLSMITH("§7 Forgeron", Villager.Profession.TOOLSMITH),
        VILLAGER_MASON("§8 Maçon", Villager.Profession.MASON),
        VILLAGER_SHEPHERD("§f Berger", Villager.Profession.SHEPHERD),
        VILLAGER_FISHERMAN("§b Pêcheur", Villager.Profession.FISHERMAN),
        VILLAGER_NONE("§7 Réfugié", Villager.Profession.NONE),
        WANDERING_TRADER("§e Voyageur", null);

        @Getter private final String titleSuffix;
        @Getter private final Villager.Profession profession;

        NPCType(String titleSuffix, Villager.Profession profession) {
            this.titleSuffix = titleSuffix;
            this.profession = profession;
        }

        public boolean isWanderingTrader() {
            return this == WANDERING_TRADER;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTEUR ET INITIALISATION
    // ═══════════════════════════════════════════════════════════════════════════

    public ShelterNPCManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.npcsByRefuge = new ConcurrentHashMap<>();
        this.npcData = new ConcurrentHashMap<>();

        startSpawnTask();
        startCleanupTask();

        plugin.log(Level.INFO, "§a✓ ShelterNPCManager initialisé");
    }

    /**
     * Démarre la tâche de spawn périodique des NPCs
     */
    private void startSpawnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndSpawnNPCs();
            }
        }.runTaskTimer(plugin, 200L, SPAWN_CHECK_INTERVAL_TICKS);
    }

    /**
     * Démarre la tâche de nettoyage des NPCs invalides
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupInvalidNPCs();
            }
        }.runTaskTimer(plugin, 100L, 400L); // Toutes les 20 secondes
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SPAWN DES NPCs
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie et spawn des NPCs pour chaque refuge avec des joueurs à proximité
     */
    private void checkAndSpawnNPCs() {
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null) return;

        for (Refuge refuge : refugeManager.getAllRefuges()) {
            // Vérifier si un joueur est dans le refuge
            if (!hasPlayerInRefuge(refuge)) continue;

            // Compter les NPCs actuels
            Set<UUID> currentNPCs = npcsByRefuge.getOrDefault(refuge.getId(), ConcurrentHashMap.newKeySet());
            if (currentNPCs.size() >= MAX_NPCS_PER_REFUGE) continue;

            // Chance de spawn
            if (random.nextDouble() < SPAWN_CHANCE) {
                spawnRandomNPC(refuge);
            }
        }
    }

    /**
     * Vérifie si un joueur est dans ou proche d'un refuge
     */
    private boolean hasPlayerInRefuge(Refuge refuge) {
        World world = Bukkit.getWorlds().get(0);
        if (world == null) return false;

        Location center = new Location(world,
            (refuge.getProtectedMinX() + refuge.getProtectedMaxX()) / 2.0,
            (refuge.getProtectedMinY() + refuge.getProtectedMaxY()) / 2.0,
            (refuge.getProtectedMinZ() + refuge.getProtectedMaxZ()) / 2.0
        );

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= PLAYER_NEARBY_RADIUS * PLAYER_NEARBY_RADIUS) {
                return true;
            }
            // Vérifier aussi si le joueur est dans la zone protégée
            if (refuge.isInProtectedArea(player.getLocation())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Spawn un NPC aléatoire dans un refuge
     */
    private void spawnRandomNPC(Refuge refuge) {
        World world = Bukkit.getWorlds().get(0);
        if (world == null) return;

        // Trouver une position de spawn dans la zone protégée
        Location spawnLoc = findSpawnLocation(refuge, world);
        if (spawnLoc == null) return;

        // Choisir un type de NPC aléatoire
        NPCType type = NPCType.values()[random.nextInt(NPCType.values().length)];

        // Choisir un nom aléatoire
        boolean isFemale = random.nextBoolean();
        String firstName = isFemale
            ? FEMALE_NAMES[random.nextInt(FEMALE_NAMES.length)]
            : MALE_NAMES[random.nextInt(MALE_NAMES.length)];

        String prefix = isFemale ? "§aSurvivante" : "§aSurvivant";

        // Créer le NPC
        Entity npc = spawnNPC(spawnLoc, type, prefix, firstName);
        if (npc == null) return;

        // Générer les phrases de lore pour ce NPC
        List<String> phrases = generateLorePhrases();

        // Enregistrer le NPC
        NPCData data = new NPCData(npc.getUniqueId(), refuge.getId(), type, firstName, isFemale, phrases);
        npcData.put(npc.getUniqueId(), data);
        npcsByRefuge.computeIfAbsent(refuge.getId(), k -> ConcurrentHashMap.newKeySet()).add(npc.getUniqueId());
    }

    /**
     * Trouve une position de spawn sûre dans un refuge
     */
    private Location findSpawnLocation(Refuge refuge, World world) {
        int attempts = 10;

        while (attempts-- > 0) {
            // Position aléatoire dans la zone protégée
            int x = refuge.getProtectedMinX() + random.nextInt(Math.max(1, refuge.getProtectedMaxX() - refuge.getProtectedMinX()));
            int z = refuge.getProtectedMinZ() + random.nextInt(Math.max(1, refuge.getProtectedMaxZ() - refuge.getProtectedMinZ()));

            // Trouver le sol
            int y = world.getHighestBlockYAt(x, z);

            // Vérifier que c'est dans les limites Y du refuge
            if (y < refuge.getProtectedMinY() || y > refuge.getProtectedMaxY()) continue;

            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

            // Vérifier que c'est un endroit sûr
            if (loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable()) {
                if (!loc.clone().add(0, -1, 0).getBlock().isLiquid()) {
                    return loc;
                }
            }
        }

        return null;
    }

    /**
     * Spawn un NPC à une position donnée
     */
    private Entity spawnNPC(Location location, NPCType type, String prefix, String firstName) {
        World world = location.getWorld();
        if (world == null) return null;

        Entity entity;

        if (type.isWanderingTrader()) {
            entity = world.spawnEntity(location, EntityType.WANDERING_TRADER);
            WanderingTrader trader = (WanderingTrader) entity;

            // Désactiver le despawn naturel
            trader.setDespawnDelay(Integer.MAX_VALUE);
            trader.setPersistent(false);
            trader.setRemoveWhenFarAway(true);
        } else {
            entity = world.spawnEntity(location, EntityType.VILLAGER);
            Villager villager = (Villager) entity;

            // Configurer le villageois
            if (type.getProfession() != null) {
                villager.setProfession(type.getProfession());
            }
            villager.setVillagerLevel(random.nextInt(5) + 1);
            villager.setVillagerExperience(0);
            villager.setPersistent(false);
            villager.setRemoveWhenFarAway(true);

            // Empêcher le trading
            villager.setRecipes(Collections.emptyList());
        }

        // Configuration commune
        LivingEntity living = (LivingEntity) entity;
        living.setAI(true);
        living.setCanPickupItems(false);
        living.setInvulnerable(true);
        living.setSilent(false);

        // Nom personnalisé avec Adventure API
        Component displayName = Component.text(prefix + " ")
            .append(Component.text(firstName).color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
            .append(Component.text(" " + type.getTitleSuffix()).color(NamedTextColor.GRAY));

        entity.customName(displayName);
        entity.setCustomNameVisible(true);

        // Métadonnées pour identification
        entity.setMetadata("zombiez_shelter_npc", new FixedMetadataValue(plugin, true));
        entity.setMetadata("zombiez_npc_type", new FixedMetadataValue(plugin, type.name()));
        entity.addScoreboardTag("shelter_npc");
        entity.addScoreboardTag("no_trading");

        return entity;
    }

    /**
     * Génère une liste de phrases de lore aléatoires pour un NPC
     */
    private List<String> generateLorePhrases() {
        List<String> phrases = new ArrayList<>();
        Set<Integer> usedCategories = new HashSet<>();

        // Sélectionner 3-5 phrases de catégories différentes
        int phraseCount = 3 + random.nextInt(3);

        while (phrases.size() < phraseCount && usedCategories.size() < LORE_PHRASES.length) {
            int category = random.nextInt(LORE_PHRASES.length);
            if (usedCategories.contains(category)) continue;
            usedCategories.add(category);

            String[] categoryPhrases = LORE_PHRASES[category];
            String phrase = categoryPhrases[random.nextInt(categoryPhrases.length)];

            // Remplacer les variables
            String profession = PROFESSIONS_BEFORE[random.nextInt(PROFESSIONS_BEFORE.length)];
            phrase = phrase.replace("%PROFESSION%", profession);

            phrases.add(phrase);
        }

        return phrases;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERACTION AVEC LES NPCs
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gère l'interaction avec un NPC survivant
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Entity entity = event.getRightClicked();

        // Vérifier si c'est un NPC de refuge
        if (!entity.hasMetadata("zombiez_shelter_npc")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Vérifier le cooldown
        long now = System.currentTimeMillis();
        Long lastInteraction = interactionCooldowns.get(playerId);
        if (lastInteraction != null && now - lastInteraction < INTERACTION_COOLDOWN_MS) {
            return;
        }
        interactionCooldowns.put(playerId, now);

        // Récupérer les données du NPC
        NPCData data = npcData.get(entity.getUniqueId());
        if (data == null) {
            // NPC sans données - message générique
            player.sendMessage("§7*Le survivant vous regarde silencieusement*");
            return;
        }

        // Obtenir une phrase aléatoire
        if (data.phrases.isEmpty()) {
            player.sendMessage("§7*" + data.name + " hoche la tête en silence*");
            return;
        }

        String phrase = data.phrases.get(random.nextInt(data.phrases.size()));

        // Afficher la phrase avec le nom du NPC
        String genderPrefix = data.isFemale ? "Survivante" : "Survivant";
        player.sendMessage("");
        player.sendMessage("§a§l" + genderPrefix + " " + data.name + "§r§7:");
        player.sendMessage(phrase);
        player.sendMessage("");

        // Effet sonore
        player.playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.8f, 0.9f + random.nextFloat() * 0.2f);

        // Faire regarder le joueur
        if (entity instanceof LivingEntity living) {
            Location npcLoc = living.getLocation();
            Location playerLoc = player.getLocation();
            npcLoc.setDirection(playerLoc.toVector().subtract(npcLoc.toVector()));
            living.setRotation(npcLoc.getYaw(), npcLoc.getPitch());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NETTOYAGE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Nettoie les NPCs invalides ou morts
     */
    private void cleanupInvalidNPCs() {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, NPCData> entry : npcData.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());

            if (entity == null || !entity.isValid() || entity.isDead()) {
                toRemove.add(entry.getKey());
            } else if (!entity.getLocation().getChunk().isLoaded()) {
                // Chunk non chargé - supprimer l'entité
                entity.remove();
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            NPCData data = npcData.remove(id);
            if (data != null) {
                Set<UUID> refugeNPCs = npcsByRefuge.get(data.refugeId);
                if (refugeNPCs != null) {
                    refugeNPCs.remove(id);
                }
            }
        }
    }

    /**
     * Force le nettoyage de tous les NPCs (appelé au shutdown)
     */
    public void shutdown() {
        for (UUID id : npcData.keySet()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        npcData.clear();
        npcsByRefuge.clear();
        interactionCooldowns.clear();

        plugin.log(Level.INFO, "§7ShelterNPCManager arrêté, NPCs nettoyés");
    }

    /**
     * Obtient le nombre total de NPCs actifs
     */
    public int getTotalNPCCount() {
        return npcData.size();
    }

    /**
     * Obtient le nombre de NPCs dans un refuge
     */
    public int getNPCCountInRefuge(int refugeId) {
        Set<UUID> npcs = npcsByRefuge.get(refugeId);
        return npcs != null ? npcs.size() : 0;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DONNÉES NPC
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Données d'un NPC survivant
     */
    public static class NPCData {
        public final UUID entityId;
        public final int refugeId;
        public final NPCType type;
        public final String name;
        public final boolean isFemale;
        public final List<String> phrases;
        public final long spawnTime;

        public NPCData(UUID entityId, int refugeId, NPCType type, String name, boolean isFemale, List<String> phrases) {
            this.entityId = entityId;
            this.refugeId = refugeId;
            this.type = type;
            this.name = name;
            this.isFemale = isFemale;
            this.phrases = phrases;
            this.spawnTime = System.currentTimeMillis();
        }
    }
}
