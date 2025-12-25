package com.rinaorc.zombiez.mobs;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Refuge;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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
import org.bukkit.util.Vector;

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

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION - Limites et comportement du spawn
    // ═══════════════════════════════════════════════════════════════════════════

    // Limites de NPCs par refuge
    private static final int MIN_NPCS_PER_REFUGE = 4;        // Minimum garanti par refuge (x2)
    private static final int MAX_NPCS_PER_REFUGE = 10;       // Maximum par refuge (x2)
    private static final int GLOBAL_MAX_NPCS = 80;           // Maximum total sur le serveur (x2)

    // Timing et chances de spawn
    private static final int SPAWN_CHECK_INTERVAL_TICKS = 400;  // 20 secondes (plus rapide)
    private static final double SPAWN_CHANCE = 0.50;            // 50% de chance par check (augmenté)
    private static final double SPAWN_CHANCE_BELOW_MIN = 0.95;  // 95% si en dessous du minimum (presque garanti)

    // Rayons de détection
    private static final double PLAYER_NEARBY_RADIUS = 48.0;          // Rayon pour spawn
    private static final double PLAYER_CLEANUP_RADIUS = 80.0;         // Rayon au-delà duquel nettoyer

    // Cooldowns et durées
    private static final long INTERACTION_COOLDOWN_MS = 3000;         // 3 secondes entre interactions
    private static final long NPC_MAX_LIFETIME_MS = 10 * 60 * 1000;   // 10 minutes max de vie
    private static final long CLEANUP_NO_PLAYER_MS = 2 * 60 * 1000;   // 2 min sans joueur = cleanup

    // Tracking du temps sans joueur par refuge
    private final Map<Integer, Long> lastPlayerSeenInRefuge = new ConcurrentHashMap<>();

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
            "§7\"On fait ce qu'on peut avec ce qu'on a. C'est la loi de la survie.\"",
            "§7\"J'ai appris à dormir d'un œil. On s'adapte ou on meurt.\"",
            "§7\"Les premiers jours ont été les pires. Maintenant... on survit.\"",
        },
        // Phrases sur le danger
        {
            "§7\"Ne t'éloigne pas trop des refuges, les zombies sont partout.\"",
            "§7\"J'ai entendu dire que les zombies au nord sont... différents.\"",
            "§7\"La nuit, ils sont plus nombreux. Fais attention.\"",
            "§7\"Certains zombies peuvent courir. §cCourir§7. Tu imagines ?\"",
            "§7\"Plus tu t'enfonces vers le nord, plus ils sont dangereux.\"",
            "§7\"J'ai vu un groupe entier se faire décimer en quelques secondes...\"",
            "§7\"Les §cHordes§7... quand tu les entends arriver, cours. Juste... cours.\"",
            "§7\"Il y a des zones où même les plus braves n'osent pas aller.\"",
            "§7\"Par temps de pluie, ils semblent plus... agressifs.\"",
        },
        // Phrases de conseil
        {
            "§7\"Équipe-toi bien avant de sortir. C'est une jungle là-dehors.\"",
            "§7\"Les autres survivants parlent d'une §eOrigine§7 au nord...\"",
            "§7\"Garde toujours de la nourriture sur toi. La faim tue autant que les morts.\"",
            "§7\"Si tu trouves des armes, ne les gaspille pas.\"",
            "§7\"Les refuges sont nos seuls havres de paix. Protège-les.\"",
            "§7\"Voyage en groupe si tu peux. Seul, tu es vulnérable.\"",
            "§7\"Les armes §6dorées§7 et §dviolettes§7 sont rares. Garde-les précieusement.\"",
            "§7\"Repère toujours les sorties avant d'entrer quelque part.\"",
            "§7\"Ne sous-estime jamais un zombie. Même le plus faible peut te surprendre.\"",
        },
        // Phrases nostalgiques
        {
            "§7\"Tu te souviens de l'époque où on pouvait... vivre normalement ?\"",
            "§7\"Ma maison était quelque part par là... avant tout ça.\"",
            "§7\"Parfois je me demande s'il y a d'autres survivants ailleurs.\"",
            "§7\"Les enfants d'aujourd'hui ne connaîtront jamais le monde d'avant.\"",
            "§7\"J'avais une vie, un travail, une famille... Tout a disparu.\"",
            "§7\"Je rêve encore de l'odeur du pain frais le matin...\"",
            "§7\"Les rues étaient pleines de vie avant. Maintenant... que des morts.\"",
            "§7\"Je garde cette photo dans ma poche. C'est tout ce qu'il me reste d'eux.\"",
            "§7\"Parfois j'entends encore leurs voix dans mes rêves...\"",
        },
        // Phrases d'espoir
        {
            "§7\"Tant qu'il y a des survivants, il y a de l'espoir.\"",
            "§7\"Un jour, on reprendra ce monde. J'en suis sûr.\"",
            "§7\"Des gens comme toi nous donnent du courage. Merci.\"",
            "§7\"Ensemble, on est plus forts. N'oublie jamais ça.\"",
            "§7\"Je crois qu'on peut reconstruire. Un jour...\"",
            "§7\"Chaque survivant qui arrive ici, c'est une victoire.\"",
            "§7\"J'ai vu des héros naître dans cette apocalypse. Tu en fais partie.\"",
            "§7\"L'humanité a survécu à pire. On s'en sortira.\"",
            "§7\"Continue à te battre. Pour ceux qui ne peuvent plus.\"",
        },
        // Phrases mystérieuses
        {
            "§7\"J'ai entendu des rumeurs sur l'§cOrigine§7 du virus...\"",
            "§7\"Certains disent que tout a commencé dans la zone 50...\"",
            "§7\"Il paraît qu'il y a des §dsecrets§7 cachés dans les ruines.\"",
            "§7\"Méfie-toi des ombres. Elles cachent plus que des zombies.\"",
            "§7\"Quelque chose de §cpuissant§7 se cache au nord. Je le sens.\"",
            "§7\"J'ai entendu parler de... §5créatures§7 qui ne sont pas des zombies.\"",
            "§7\"Certains prétendent avoir vu le §cPatient Zéro§7. Toujours vivant...\"",
            "§7\"Les scientifiques savaient. Ils savaient et n'ont rien dit.\"",
            "§7\"Des World Boss apparaissent parfois... des abominations.\"",
            "§7\"Il y a des coffres cachés partout. Si tu cherches bien...\"",
        },
        // Phrases sur le refuge
        {
            "§7\"Ce refuge est notre maison maintenant. On le défendra.\"",
            "§7\"Les murs tiennent bon pour l'instant. Espérons que ça dure.\"",
            "§7\"Ici, au moins, on peut dormir tranquille. Presque.\"",
            "§7\"Chaque refuge sauvé est une victoire contre les ténèbres.\"",
            "§7\"Bienvenue, voyageur. Repose-toi un peu.\"",
            "§7\"§e%REFUGE_NAME%§7... c'est chez nous maintenant.\"",
            "§7\"On a tout reconstruit ici. Pierre par pierre.\"",
            "§7\"Les premiers temps, on n'était que trois. Maintenant regarde-nous.\"",
        },
        // Phrases sur la communauté
        {
            "§7\"On partage tout ici. C'est comme ça qu'on survit.\"",
            "§7\"Chacun a son rôle. Même les plus petites contributions comptent.\"",
            "§7\"On a perdu des amis pour arriver ici. On n'oubliera jamais.\"",
            "§7\"Tu as l'air fatigué. Prends le temps de te reposer.\"",
            "§7\"Si tu as besoin de provisions, parle aux autres survivants.\"",
            "§7\"On organise des expéditions vers le nord. Tu veux te joindre à nous ?\"",
            "§7\"La nuit dernière, on a entendu des cris au loin... Personne n'est sorti.\"",
        },
        // Phrases sur le monde extérieur
        {
            "§7\"Les ruines des villes sont remplies de trésors... et de dangers.\"",
            "§7\"J'ai exploré jusqu'à la zone 15. Au-delà, je n'ose pas.\"",
            "§7\"Il paraît que certaines zones ont des effets... étranges.\"",
            "§7\"Le froid au nord est mortel. Équipe-toi en conséquence.\"",
            "§7\"Les tempêtes apportent parfois des choses bizarres.\"",
            "§7\"J'ai vu des animaux survivre. Des cochons, des vaches... La vie persiste.\"",
            "§7\"Les routes sont dangereuses, mais c'est le seul moyen d'avancer.\"",
        }
    };

    // Phrases contextuelles selon l'heure
    private static final String[] NIGHT_PHRASES = {
        "§7\"Fais attention dehors. La nuit, ils sont partout.\"",
        "§7\"Tu ne devrais pas sortir maintenant. Attends l'aube.\"",
        "§7\"Je n'arrive jamais à dormir ces nuits-là...\"",
        "§7\"Écoute... Tu entends ça ? Ils rodent autour des murs.\"",
        "§7\"La lune est haute. Mauvais présage.\"",
        "§7\"Reste près du feu. La chaleur les repousse... un peu.\"",
    };

    private static final String[] DAY_PHRASES = {
        "§7\"Le soleil est levé. Un jour de plus à survivre.\"",
        "§7\"C'est le meilleur moment pour sortir explorer.\"",
        "§7\"Belle journée pour tuer des zombies, non ?\"",
        "§7\"Profite de la lumière. Elle ne dure jamais assez longtemps.\"",
        "§7\"J'aime voir le soleil. Ça me rappelle que le monde existe encore.\"",
    };

    // Phrases d'actions/gestes
    private static final String[] ACTION_PHRASES = {
        "§8*%NPC_NAME% vous regarde avec un mélange de fatigue et d'espoir*",
        "§8*%NPC_NAME% hoche lentement la tête*",
        "§8*%NPC_NAME% soupire profondément*",
        "§8*%NPC_NAME% esquisse un faible sourire*",
        "§8*%NPC_NAME% observe nerveusement les alentours*",
        "§8*%NPC_NAME% frotte ses mains calleuses*",
        "§8*%NPC_NAME% ajuste ses vêtements usés*",
        "§8*%NPC_NAME% vous fait signe d'approcher*",
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
        startIdleBehaviorTask();

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

    /**
     * Démarre la tâche de comportements idle pour les NPCs
     * Les NPCs regardent autour d'eux, font des petits gestes, etc.
     */
    private void startIdleBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                performIdleBehaviors();
            }
        }.runTaskTimer(plugin, 300L, 200L); // Toutes les 10 secondes
    }

    /**
     * Exécute des comportements idle aléatoires pour les NPCs
     */
    private void performIdleBehaviors() {
        for (Map.Entry<UUID, NPCData> entry : npcData.entrySet()) {
            // 25% de chance par tick de faire une action idle
            if (random.nextDouble() > 0.25) continue;

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity living)) continue;

            // Vérifier qu'il n'y a pas de joueur très proche (éviter les interruptions)
            boolean playerNearby = false;
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(entity.getLocation()) < 16) { // 4 blocs
                    playerNearby = true;
                    break;
                }
            }

            if (playerNearby) continue;

            // Choisir une action idle aléatoire
            int action = random.nextInt(5);
            switch (action) {
                case 0 -> lookAround(living);
                case 1 -> lookAtSky(living);
                case 2 -> lookAtGround(living);
                case 3 -> shakeHead(living);
                case 4 -> playIdleSound(living);
            }
        }
    }

    /**
     * Fait regarder le NPC autour de lui
     */
    private void lookAround(LivingEntity npc) {
        Location loc = npc.getLocation();
        float currentYaw = loc.getYaw();

        // Tourner de -45 à +45 degrés
        float newYaw = currentYaw + (random.nextFloat() * 90 - 45);
        npc.setRotation(newYaw, 0);

        // Revenir à la position originale après un délai
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) {
                npc.setRotation(currentYaw, 0);
            }
        }, 40L + random.nextInt(20));
    }

    /**
     * Fait regarder le NPC vers le ciel
     */
    private void lookAtSky(LivingEntity npc) {
        Location loc = npc.getLocation();
        float originalPitch = loc.getPitch();

        npc.setRotation(loc.getYaw(), -30 - random.nextInt(20));

        // Revenir à la position originale
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) {
                npc.setRotation(npc.getLocation().getYaw(), originalPitch);
            }
        }, 30L + random.nextInt(20));
    }

    /**
     * Fait regarder le NPC vers le sol
     */
    private void lookAtGround(LivingEntity npc) {
        Location loc = npc.getLocation();
        float originalPitch = loc.getPitch();

        npc.setRotation(loc.getYaw(), 30 + random.nextInt(20));

        // Revenir à la position originale
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) {
                npc.setRotation(npc.getLocation().getYaw(), originalPitch);
            }
        }, 25L + random.nextInt(15));
    }

    /**
     * Fait hocher la tête au NPC
     */
    private void shakeHead(LivingEntity npc) {
        Location loc = npc.getLocation();
        float originalYaw = loc.getYaw();

        // Animation de hochement de tête
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) npc.setRotation(originalYaw - 15, loc.getPitch());
        }, 3L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) npc.setRotation(originalYaw + 15, loc.getPitch());
        }, 8L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) npc.setRotation(originalYaw, loc.getPitch());
        }, 13L);
    }

    /**
     * Joue un son idle pour le NPC
     */
    private void playIdleSound(LivingEntity npc) {
        Location loc = npc.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Jouer le son pour les joueurs proches
        Sound[] idleSounds = {
            Sound.ENTITY_VILLAGER_AMBIENT,
            Sound.ENTITY_VILLAGER_AMBIENT,
            Sound.BLOCK_GRASS_STEP,
            Sound.ENTITY_PLAYER_BREATH
        };

        Sound sound = idleSounds[random.nextInt(idleSounds.length)];
        float volume = 0.3f + random.nextFloat() * 0.2f;
        float pitch = 0.8f + random.nextFloat() * 0.4f;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= 256) { // 16 blocs
                player.playSound(loc, sound, volume, pitch);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SPAWN DES NPCs
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie et spawn des NPCs pour chaque refuge avec des joueurs à proximité
     * Optimisé avec:
     * - Limite globale (GLOBAL_MAX_NPCS)
     * - Minimum garanti par refuge (MIN_NPCS_PER_REFUGE)
     * - Maximum par refuge (MAX_NPCS_PER_REFUGE)
     * - Cleanup des refuges sans joueurs
     */
    private void checkAndSpawnNPCs() {
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null) return;

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 1: Limite globale
        // ═══════════════════════════════════════════════════════════════════
        int totalNPCs = getValidNPCCount();
        if (totalNPCs >= GLOBAL_MAX_NPCS) {
            return; // Cap global atteint, pas de spawn
        }

        long now = System.currentTimeMillis();

        for (Refuge refuge : refugeManager.getAllRefuges()) {
            int refugeId = refuge.getId();

            // ═══════════════════════════════════════════════════════════════
            // VÉRIFICATION 2: Présence de joueur
            // ═══════════════════════════════════════════════════════════════
            boolean playerPresent = hasPlayerInRefuge(refuge);

            if (playerPresent) {
                // Mettre à jour le timestamp de dernière présence
                lastPlayerSeenInRefuge.put(refugeId, now);
            } else {
                // Vérifier si le refuge est inactif depuis trop longtemps
                Long lastSeen = lastPlayerSeenInRefuge.get(refugeId);
                if (lastSeen != null && (now - lastSeen) > CLEANUP_NO_PLAYER_MS) {
                    // Nettoyer les NPCs de ce refuge inactif
                    cleanupRefugeNPCs(refugeId);
                }
                continue; // Pas de joueur, pas de spawn
            }

            // ═══════════════════════════════════════════════════════════════
            // VÉRIFICATION 3: Compter les NPCs valides du refuge
            // ═══════════════════════════════════════════════════════════════
            int currentCount = getValidNPCCountForRefuge(refugeId);

            // Maximum atteint pour ce refuge
            if (currentCount >= MAX_NPCS_PER_REFUGE) {
                continue;
            }

            // Re-vérifier le cap global
            if (totalNPCs >= GLOBAL_MAX_NPCS) {
                continue;
            }

            // ═══════════════════════════════════════════════════════════════
            // LOGIQUE DE SPAWN: Priorité au minimum garanti
            // ═══════════════════════════════════════════════════════════════
            if (currentCount < MIN_NPCS_PER_REFUGE) {
                // En dessous du minimum: spawn multiple pour remplir rapidement
                int toSpawn = Math.min(MIN_NPCS_PER_REFUGE - currentCount, 3); // Max 3 d'un coup
                for (int i = 0; i < toSpawn && totalNPCs < GLOBAL_MAX_NPCS; i++) {
                    if (random.nextDouble() < SPAWN_CHANCE_BELOW_MIN) {
                        spawnRandomNPC(refuge);
                        totalNPCs++;
                    }
                }
            } else if (currentCount < MAX_NPCS_PER_REFUGE) {
                // Au-dessus du minimum mais en dessous du max: chance normale
                if (random.nextDouble() < SPAWN_CHANCE) {
                    spawnRandomNPC(refuge);
                    totalNPCs++;
                }
            }
        }
    }

    /**
     * Compte le nombre total de NPCs valides (entités existantes)
     */
    private int getValidNPCCount() {
        int count = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (UUID npcId : npcData.keySet()) {
            Entity entity = Bukkit.getEntity(npcId);
            if (entity != null && entity.isValid() && !entity.isDead()) {
                count++;
            } else {
                toRemove.add(npcId);
            }
        }

        // Nettoyer les entrées invalides
        for (UUID id : toRemove) {
            removeNPCData(id);
        }

        return count;
    }

    /**
     * Compte le nombre de NPCs valides dans un refuge spécifique
     */
    private int getValidNPCCountForRefuge(int refugeId) {
        Set<UUID> npcs = npcsByRefuge.get(refugeId);
        if (npcs == null || npcs.isEmpty()) {
            return 0;
        }

        int count = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (UUID npcId : npcs) {
            Entity entity = Bukkit.getEntity(npcId);
            if (entity != null && entity.isValid() && !entity.isDead()) {
                count++;
            } else {
                toRemove.add(npcId);
            }
        }

        // Nettoyer les entrées invalides
        for (UUID id : toRemove) {
            removeNPCData(id);
        }

        return count;
    }

    /**
     * Supprime les données d'un NPC de toutes les maps
     */
    private void removeNPCData(UUID npcId) {
        NPCData data = npcData.remove(npcId);
        if (data != null) {
            Set<UUID> refugeNPCs = npcsByRefuge.get(data.refugeId);
            if (refugeNPCs != null) {
                refugeNPCs.remove(npcId);
            }
        }
    }

    /**
     * Nettoie tous les NPCs d'un refuge inactif
     */
    private void cleanupRefugeNPCs(int refugeId) {
        Set<UUID> npcs = npcsByRefuge.get(refugeId);
        if (npcs == null || npcs.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>(npcs);
        for (UUID npcId : toRemove) {
            Entity entity = Bukkit.getEntity(npcId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
            npcData.remove(npcId);
        }

        npcs.clear();
        lastPlayerSeenInRefuge.remove(refugeId);
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
        List<String> phrases = generateLorePhrases(refuge.getName());

        // Enregistrer le NPC
        NPCData data = new NPCData(npc.getUniqueId(), refuge.getId(), refuge.getName(), type, firstName, isFemale, phrases);
        npcData.put(npc.getUniqueId(), data);
        npcsByRefuge.computeIfAbsent(refuge.getId(), k -> ConcurrentHashMap.newKeySet()).add(npc.getUniqueId());
    }

    // Distance de spawn des NPCs autour des joueurs
    private static final double NPC_SPAWN_MIN_DISTANCE = 6.0;   // Distance minimum
    private static final double NPC_SPAWN_MAX_DISTANCE = 14.0;  // Distance maximum

    /**
     * Trouve une position de spawn sûre dans un refuge, proche des joueurs
     */
    private Location findSpawnLocation(Refuge refuge, World world) {
        // D'abord, trouver un joueur dans le refuge pour spawn proche de lui
        Player nearestPlayer = findPlayerInRefuge(refuge, world);

        if (nearestPlayer != null) {
            // Spawn proche du joueur
            Location playerLoc = nearestPlayer.getLocation();
            int attempts = 15;

            while (attempts-- > 0) {
                // Distance aléatoire entre min et max
                double distance = NPC_SPAWN_MIN_DISTANCE + random.nextDouble() * (NPC_SPAWN_MAX_DISTANCE - NPC_SPAWN_MIN_DISTANCE);
                double angle = random.nextDouble() * 2 * Math.PI;

                int x = (int) (playerLoc.getX() + Math.cos(angle) * distance);
                int z = (int) (playerLoc.getZ() + Math.sin(angle) * distance);

                // Vérifier que c'est dans la zone protégée
                if (x < refuge.getProtectedMinX() || x > refuge.getProtectedMaxX()) continue;
                if (z < refuge.getProtectedMinZ() || z > refuge.getProtectedMaxZ()) continue;

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
        }

        // Fallback: spawn aléatoire dans la zone protégée si pas de joueur trouvé
        int attempts = 10;
        while (attempts-- > 0) {
            int x = refuge.getProtectedMinX() + random.nextInt(Math.max(1, refuge.getProtectedMaxX() - refuge.getProtectedMinX()));
            int z = refuge.getProtectedMinZ() + random.nextInt(Math.max(1, refuge.getProtectedMaxZ() - refuge.getProtectedMinZ()));

            int y = world.getHighestBlockYAt(x, z);

            if (y < refuge.getProtectedMinY() || y > refuge.getProtectedMaxY()) continue;

            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

            if (loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable()) {
                if (!loc.clone().add(0, -1, 0).getBlock().isLiquid()) {
                    return loc;
                }
            }
        }

        return null;
    }

    /**
     * Trouve un joueur dans un refuge
     */
    private Player findPlayerInRefuge(Refuge refuge, World world) {
        for (Player player : world.getPlayers()) {
            if (refuge.isInProtectedArea(player.getLocation())) {
                return player;
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
    private List<String> generateLorePhrases(String refugeName) {
        List<String> phrases = new ArrayList<>();
        Set<Integer> usedCategories = new HashSet<>();

        // Sélectionner 4-7 phrases de catégories différentes
        int phraseCount = 4 + random.nextInt(4);

        while (phrases.size() < phraseCount && usedCategories.size() < LORE_PHRASES.length) {
            int category = random.nextInt(LORE_PHRASES.length);
            if (usedCategories.contains(category)) continue;
            usedCategories.add(category);

            String[] categoryPhrases = LORE_PHRASES[category];
            String phrase = categoryPhrases[random.nextInt(categoryPhrases.length)];

            // Remplacer les variables
            String profession = PROFESSIONS_BEFORE[random.nextInt(PROFESSIONS_BEFORE.length)];
            phrase = phrase.replace("%PROFESSION%", profession);
            phrase = phrase.replace("%REFUGE_NAME%", refugeName);

            phrases.add(phrase);
        }

        // Ajouter quelques phrases jour/nuit
        phrases.add(DAY_PHRASES[random.nextInt(DAY_PHRASES.length)]);
        phrases.add(NIGHT_PHRASES[random.nextInt(NIGHT_PHRASES.length)]);

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
            player.sendMessage("§8*Le survivant vous regarde silencieusement*");
            playRandomVillagerSound(player, entity.getLocation());
            return;
        }

        // Incrémenter le compteur d'interactions
        data.interactionCount++;

        // Faire regarder le joueur immédiatement
        if (entity instanceof LivingEntity living) {
            makeNPCLookAtPlayer(living, player);
        }

        // Choisir la phrase appropriée selon le contexte
        String phrase = getContextualPhrase(data, entity.getWorld());

        // Afficher le dialogue de manière immersive
        displayDialogue(player, data, phrase, entity.getLocation());

        // Effets visuels
        spawnInteractionParticles(entity.getLocation());
    }

    /**
     * Obtient une phrase contextuelle selon l'heure du jour
     */
    private String getContextualPhrase(NPCData data, World world) {
        long time = world.getTime();
        boolean isNight = time >= 13000 && time < 23000;

        // 20% de chance d'avoir une phrase contextuelle jour/nuit
        if (random.nextDouble() < 0.20) {
            if (isNight) {
                return NIGHT_PHRASES[random.nextInt(NIGHT_PHRASES.length)];
            } else {
                return DAY_PHRASES[random.nextInt(DAY_PHRASES.length)];
            }
        }

        // Sinon, phrase normale
        if (data.phrases.isEmpty()) {
            return "§7*" + data.name + " hoche la tête en silence*";
        }

        return data.phrases.get(random.nextInt(data.phrases.size()));
    }

    /**
     * Affiche le dialogue de manière immersive
     */
    private void displayDialogue(Player player, NPCData data, String phrase, Location npcLoc) {
        String fullName = data.getFullName();

        // Séparateur visuel
        player.sendMessage("");
        player.sendMessage("§8§m──────────────────────────────");

        // 30% de chance d'avoir une action avant le dialogue
        if (random.nextDouble() < 0.30) {
            String action = ACTION_PHRASES[random.nextInt(ACTION_PHRASES.length)];
            action = action.replace("%NPC_NAME%", fullName);
            player.sendMessage(action);
            player.sendMessage("");
        }

        // Nom du NPC avec style
        String typeEmoji = getTypeEmoji(data.type);
        player.sendMessage("§a§l" + fullName + " §r" + typeEmoji + "§7:");

        // La phrase de dialogue
        player.sendMessage(phrase);

        player.sendMessage("§8§m──────────────────────────────");
        player.sendMessage("");

        // Jouer le son approprié
        playContextualSound(player, npcLoc, data);
    }

    /**
     * Obtient un emoji selon le type de NPC
     */
    private String getTypeEmoji(NPCType type) {
        return switch (type) {
            case VILLAGER_ARMORER -> "§6⚔";
            case VILLAGER_BUTCHER -> "§c🔪";
            case VILLAGER_FARMER -> "§a🌾";
            case VILLAGER_LIBRARIAN -> "§f📚";
            case VILLAGER_CLERIC -> "§5✚";
            case VILLAGER_TOOLSMITH -> "§7🔧";
            case VILLAGER_MASON -> "§8⛏";
            case VILLAGER_SHEPHERD -> "§f🐑";
            case VILLAGER_FISHERMAN -> "§b🐟";
            case VILLAGER_NONE -> "§7👤";
            case WANDERING_TRADER -> "§e🧭";
        };
    }

    /**
     * Joue un son contextuel selon le NPC
     */
    private void playContextualSound(Player player, Location loc, NPCData data) {
        float pitch = 0.85f + random.nextFloat() * 0.3f;
        float volume = 0.7f;

        // Son différent selon le type de NPC
        Sound sound = switch (data.type) {
            case WANDERING_TRADER -> Sound.ENTITY_WANDERING_TRADER_AMBIENT;
            case VILLAGER_CLERIC -> Sound.ENTITY_VILLAGER_CELEBRATE;
            default -> {
                // Varier les sons de villageois
                Sound[] villagerSounds = {
                    Sound.ENTITY_VILLAGER_AMBIENT,
                    Sound.ENTITY_VILLAGER_TRADE,
                    Sound.ENTITY_VILLAGER_YES
                };
                yield villagerSounds[random.nextInt(villagerSounds.length)];
            }
        };

        player.playSound(loc, sound, volume, pitch);

        // Son additionnel pour l'immersion (20% de chance)
        if (random.nextDouble() < 0.20) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(loc, Sound.ENTITY_VILLAGER_AMBIENT, 0.4f, pitch + 0.1f);
            }, 15L);
        }
    }

    /**
     * Joue un son de villageois aléatoire
     */
    private void playRandomVillagerSound(Player player, Location loc) {
        player.playSound(loc, Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 0.9f + random.nextFloat() * 0.2f);
    }

    /**
     * Fait regarder le NPC vers le joueur
     */
    private void makeNPCLookAtPlayer(LivingEntity npc, Player player) {
        Location npcLoc = npc.getLocation();
        Location playerLoc = player.getLocation();

        // Calculer la direction vers le joueur
        Vector direction = playerLoc.toVector().subtract(npcLoc.toVector()).normalize();
        npcLoc.setDirection(direction);

        // Appliquer la rotation
        npc.setRotation(npcLoc.getYaw(), npcLoc.getPitch());

        // Animation subtile - petit mouvement de tête
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npc.isValid()) {
                Location newLoc = npc.getLocation();
                Vector newDir = player.getLocation().toVector().subtract(newLoc.toVector()).normalize();
                newLoc.setDirection(newDir);
                npc.setRotation(newLoc.getYaw(), newLoc.getPitch());
            }
        }, 5L);
    }

    /**
     * Spawn des particules lors de l'interaction
     */
    private void spawnInteractionParticles(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Particules de "parole" au-dessus de la tête du NPC
        Location particleLoc = loc.clone().add(0, 2.2, 0);

        // Petites particules de notes/cœurs pour symboliser la communication
        world.spawnParticle(
            Particle.HAPPY_VILLAGER,
            particleLoc,
            3,
            0.2, 0.1, 0.2,
            0
        );

        // Parfois des particules supplémentaires (30% de chance)
        if (random.nextDouble() < 0.30) {
            world.spawnParticle(
                Particle.END_ROD,
                particleLoc,
                2,
                0.15, 0.1, 0.15,
                0.01
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NETTOYAGE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Nettoie les NPCs invalides, morts, trop vieux ou trop loin des joueurs
     */
    private void cleanupInvalidNPCs() {
        List<UUID> toRemove = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, NPCData> entry : npcData.entrySet()) {
            UUID npcId = entry.getKey();
            NPCData data = entry.getValue();
            Entity entity = Bukkit.getEntity(npcId);

            // ═══════════════════════════════════════════════════════════════
            // NETTOYAGE 1: Entité invalide ou morte
            // ═══════════════════════════════════════════════════════════════
            if (entity == null || !entity.isValid() || entity.isDead()) {
                toRemove.add(npcId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════
            // NETTOYAGE 2: Chunk non chargé
            // ═══════════════════════════════════════════════════════════════
            if (!entity.getLocation().getChunk().isLoaded()) {
                entity.remove();
                toRemove.add(npcId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════
            // NETTOYAGE 3: NPC trop vieux (durée de vie max atteinte)
            // ═══════════════════════════════════════════════════════════════
            long age = now - data.spawnTime;
            if (age > NPC_MAX_LIFETIME_MS) {
                entity.remove();
                toRemove.add(npcId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════
            // NETTOYAGE 4: Pas de joueur à proximité
            // ═══════════════════════════════════════════════════════════════
            if (!hasPlayerNearby(entity, PLAYER_CLEANUP_RADIUS)) {
                // Vérifier le temps depuis la dernière présence de joueur
                Long lastSeen = lastPlayerSeenInRefuge.get(data.refugeId);
                if (lastSeen == null || (now - lastSeen) > CLEANUP_NO_PLAYER_MS) {
                    entity.remove();
                    toRemove.add(npcId);
                }
            }
        }

        // Supprimer les données des NPCs nettoyés
        for (UUID id : toRemove) {
            removeNPCData(id);
        }

        // Log si beaucoup de NPCs nettoyés (debug)
        if (toRemove.size() > 3 && plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode()) {
            plugin.log(Level.INFO, "§7[ShelterNPC Cleanup] " + toRemove.size() + " NPCs nettoyés");
        }
    }

    /**
     * Vérifie si un joueur est proche d'une entité
     */
    private boolean hasPlayerNearby(Entity entity, double radius) {
        double radiusSq = radius * radius;
        World world = entity.getWorld();

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    /**
     * Force le nettoyage de tous les NPCs (appelé au shutdown)
     */
    public void shutdown() {
        int cleaned = 0;
        for (UUID id : npcData.keySet()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) {
                entity.remove();
                cleaned++;
            }
        }

        npcData.clear();
        npcsByRefuge.clear();
        interactionCooldowns.clear();
        lastPlayerSeenInRefuge.clear();

        plugin.log(Level.INFO, "§7ShelterNPCManager arrêté, " + cleaned + " NPCs nettoyés");
    }

    /**
     * Obtient les statistiques du système de NPCs
     */
    public String getStats() {
        int totalNPCs = getValidNPCCount();
        int activeRefuges = (int) npcsByRefuge.values().stream().filter(set -> !set.isEmpty()).count();

        return String.format("§7NPCs: §e%d§7/§6%d §8(§7Refuges actifs: §e%d§8)",
            totalNPCs, GLOBAL_MAX_NPCS, activeRefuges);
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
        public final String refugeName;
        public final NPCType type;
        public final String name;
        public final boolean isFemale;
        public final List<String> phrases;
        public final long spawnTime;
        public int interactionCount = 0;

        public NPCData(UUID entityId, int refugeId, String refugeName, NPCType type, String name, boolean isFemale, List<String> phrases) {
            this.entityId = entityId;
            this.refugeId = refugeId;
            this.refugeName = refugeName;
            this.type = type;
            this.name = name;
            this.isFemale = isFemale;
            this.phrases = phrases;
            this.spawnTime = System.currentTimeMillis();
        }

        public String getFullName() {
            return (isFemale ? "Survivante" : "Survivant") + " " + name;
        }
    }
}
