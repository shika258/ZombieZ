package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Gestionnaire centralisé des NPCs Journey utilisant Citizens API.
 * Fournit une API simple pour créer, gérer et interagir avec les NPCs des quêtes.
 *
 * IMPORTANT: Tous les NPCs des Journey DOIVENT être créés via ce manager.
 *
 * RÈGLE D'AFFICHAGE: Les noms natifs des NPCs (Citizens ou vanilla) sont TOUJOURS
 * cachés. Toutes les informations sont affichées via des TextDisplays au-dessus
 * du NPC pour un rendu plus grand et plus riche.
 */
public class JourneyNPCManager implements Listener {

    private final ZombieZPlugin plugin;
    private final NamespacedKey NPC_ID_KEY;

    // Cache des NPCs par ID unique (ex: "chapter1_farmer", "chapter2_igor")
    private final Map<String, NPC> npcCache = new ConcurrentHashMap<>();

    // Cache des TextDisplays associés aux NPCs
    private final Map<String, TextDisplay> displayCache = new ConcurrentHashMap<>();

    // Handlers d'interaction par NPC ID
    private final Map<String, Consumer<PlayerInteractEntityEvent>> interactionHandlers = new ConcurrentHashMap<>();

    // État de Citizens
    private boolean citizensEnabled = false;
    private NPCRegistry npcRegistry;

    // Constantes pour les TextDisplays
    private static final float DEFAULT_DISPLAY_SCALE = 2.0f;
    private static final double DEFAULT_DISPLAY_HEIGHT = 2.5;
    private static final float DISPLAY_VIEW_RANGE = 0.6f;

    public JourneyNPCManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.NPC_ID_KEY = new NamespacedKey(plugin, "journey_npc_id");

        // Enregistrer le listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Vérifier si Citizens est disponible
        checkCitizens();

        // Nettoyage des NPCs orphelins après initialisation
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    cleanupOrphanedNPCs(world);
                }
            }
        }.runTaskLater(plugin, 60L); // 3 secondes après le démarrage
    }

    /**
     * Vérifie si Citizens est disponible et l'initialise
     */
    private void checkCitizens() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            try {
                npcRegistry = CitizensAPI.getNPCRegistry();
                citizensEnabled = true;
                plugin.log(Level.INFO, "§a✓ Citizens détecté - NPCs Journey via Citizens API");
            } catch (Exception e) {
                citizensEnabled = false;
                plugin.log(Level.WARNING, "§c✗ Citizens détecté mais non initialisé - Fallback Villager");
            }
        } else {
            citizensEnabled = false;
            plugin.log(Level.WARNING, "§e⚠ Citizens non installé - NPCs Journey via Villager vanilla");
        }
    }

    /**
     * Vérifie si Citizens est disponible
     */
    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }

    // ==================== API DE CRÉATION ====================

    /**
     * Configuration d'un NPC Journey
     */
    public static class NPCConfig {
        private final String id;
        private final String displayName;
        private final Location location;
        private EntityType entityType = EntityType.PLAYER;
        private String skinName = null;
        private String skinSignature = null;
        private String skinValue = null;
        private Villager.Profession profession = null;
        private boolean lookClose = true;
        private ItemStack helmet = null;
        private ItemStack chestplate = null;
        private ItemStack leggings = null;
        private ItemStack boots = null;
        private ItemStack mainHand = null;
        private Consumer<PlayerInteractEntityEvent> interactionHandler = null;
        private String[] displayLines = null;
        private float displayScale = 1.5f;
        private double displayHeight = 2.5;

        public NPCConfig(String id, String displayName, Location location) {
            this.id = id;
            this.displayName = displayName;
            this.location = location;
        }

        public NPCConfig entityType(EntityType type) {
            this.entityType = type;
            return this;
        }

        public NPCConfig skin(String playerName) {
            this.skinName = playerName;
            return this;
        }

        public NPCConfig skinData(String value, String signature) {
            this.skinValue = value;
            this.skinSignature = signature;
            return this;
        }

        public NPCConfig profession(Villager.Profession profession) {
            this.profession = profession;
            return this;
        }

        public NPCConfig lookClose(boolean enabled) {
            this.lookClose = enabled;
            return this;
        }

        public NPCConfig equipment(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            return this;
        }

        public NPCConfig mainHand(ItemStack item) {
            this.mainHand = item;
            return this;
        }

        public NPCConfig onInteract(Consumer<PlayerInteractEntityEvent> handler) {
            this.interactionHandler = handler;
            return this;
        }

        public NPCConfig display(String... lines) {
            this.displayLines = lines;
            return this;
        }

        public NPCConfig displayScale(float scale) {
            this.displayScale = scale;
            return this;
        }

        public NPCConfig displayHeight(double height) {
            this.displayHeight = height;
            return this;
        }

        // Getters
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Location getLocation() { return location; }
    }

    /**
     * Crée ou récupère un NPC Journey.
     * Si le NPC existe déjà, le réutilise.
     *
     * @param config Configuration du NPC
     * @return L'entité du NPC (NPC entity si Citizens, Villager sinon)
     */
    public Entity createOrGetNPC(NPCConfig config) {
        // Vérifier le cache
        if (citizensEnabled) {
            NPC cached = npcCache.get(config.id);
            if (cached != null && cached.isSpawned() && cached.getEntity() != null && cached.getEntity().isValid()) {
                return cached.getEntity();
            }
        }

        // Enregistrer le handler d'interaction
        if (config.interactionHandler != null) {
            interactionHandlers.put(config.id, config.interactionHandler);
        }

        if (citizensEnabled) {
            return createCitizensNPC(config);
        } else {
            return createVanillaNPC(config);
        }
    }

    /**
     * Crée un NPC via Citizens API.
     * IMPORTANT: Le nom natif du NPC est TOUJOURS caché - toutes les infos passent par TextDisplay.
     */
    private Entity createCitizensNPC(NPCConfig config) {
        // Chercher un NPC existant avec cet ID
        for (NPC npc : npcRegistry) {
            if (npc.data().has("journey_npc_id") && config.id.equals(npc.data().get("journey_npc_id"))) {
                // NPC trouvé, le respawn si nécessaire
                if (!npc.isSpawned()) {
                    npc.spawn(config.location);
                }
                npcCache.put(config.id, npc);
                setupCitizensNPCProperties(npc, config);
                // IMPORTANT: Cacher le nom natif Citizens
                hideNPCNativeName(npc);
                createDisplayForNPC(config);
                return npc.getEntity();
            }
        }

        // Créer un nouveau NPC avec un nom vide (le nom sera affiché via TextDisplay)
        // On garde le displayName interne pour l'identification mais il ne sera pas visible
        NPC npc = npcRegistry.createNPC(config.entityType, "");
        npc.data().set("journey_npc_id", config.id);
        npc.data().setPersistent("journey_npc_id", config.id);

        // Propriétés de base
        npc.setProtected(true); // Invulnérable

        // Spawn
        npc.spawn(config.location);

        // Configuration
        setupCitizensNPCProperties(npc, config);

        // IMPORTANT: Cacher le nom natif Citizens
        hideNPCNativeName(npc);

        // Cache
        npcCache.put(config.id, npc);

        // TextDisplay - toutes les informations sont affichées ici
        createDisplayForNPC(config);

        plugin.log(Level.INFO, "§a✓ NPC Citizens créé: " + config.id + " (nom via TextDisplay)");

        return npc.getEntity();
    }

    /**
     * Cache le nom natif d'un NPC Citizens.
     * Désactive la nameplate pour que seul le TextDisplay soit visible.
     */
    private void hideNPCNativeName(NPC npc) {
        // Désactiver l'affichage du nameplate Citizens
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);

        // Pour les entités vanilla sous-jacentes, cacher aussi le customName
        if (npc.getEntity() != null && npc.getEntity() instanceof LivingEntity living) {
            living.setCustomNameVisible(false);
        }
    }

    /**
     * Configure les propriétés d'un NPC Citizens
     */
    private void setupCitizensNPCProperties(NPC npc, NPCConfig config) {
        // LookClose trait
        if (config.lookClose) {
            LookClose lookClose = npc.getOrAddTrait(LookClose.class);
            lookClose.lookClose(true);
            lookClose.setRange(10);
        }

        // Skin (pour les NPCs PLAYER)
        if (config.entityType == EntityType.PLAYER) {
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            if (config.skinValue != null && config.skinSignature != null) {
                skinTrait.setSkinPersistent(config.id, config.skinSignature, config.skinValue);
            } else if (config.skinName != null) {
                skinTrait.setSkinName(config.skinName);
            }
        }

        // Équipement
        Equipment equipment = npc.getOrAddTrait(Equipment.class);
        if (config.helmet != null) equipment.set(Equipment.EquipmentSlot.HELMET, config.helmet);
        if (config.chestplate != null) equipment.set(Equipment.EquipmentSlot.CHESTPLATE, config.chestplate);
        if (config.leggings != null) equipment.set(Equipment.EquipmentSlot.LEGGINGS, config.leggings);
        if (config.boots != null) equipment.set(Equipment.EquipmentSlot.BOOTS, config.boots);
        if (config.mainHand != null) equipment.set(Equipment.EquipmentSlot.HAND, config.mainHand);

        // Profession pour Villager
        if (config.entityType == EntityType.VILLAGER && config.profession != null && npc.getEntity() instanceof Villager villager) {
            villager.setProfession(config.profession);
        }

        // Tag PDC sur l'entité pour identification rapide
        if (npc.getEntity() != null) {
            npc.getEntity().getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, config.id);
            npc.getEntity().addScoreboardTag("zombiez_journey_npc");
            npc.getEntity().addScoreboardTag("journey_" + config.id);
        }
    }

    /**
     * Crée un NPC vanilla (fallback si Citizens non disponible).
     * IMPORTANT: Le nom natif est TOUJOURS caché - toutes les infos passent par TextDisplay.
     */
    private Entity createVanillaNPC(NPCConfig config) {
        World world = config.location.getWorld();
        if (world == null) return null;

        // Chercher entité existante
        for (Entity entity : world.getNearbyEntities(config.location, 50, 30, 50)) {
            if (entity.getPersistentDataContainer().has(NPC_ID_KEY, PersistentDataType.STRING)) {
                String id = entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING);
                if (config.id.equals(id)) {
                    // S'assurer que le nom natif est caché
                    if (entity instanceof LivingEntity living) {
                        living.setCustomNameVisible(false);
                    }
                    createDisplayForNPC(config);
                    return entity;
                }
            }
        }

        // Créer un Villager vanilla SANS nom visible (tout via TextDisplay)
        Entity npcEntity = world.spawn(config.location, Villager.class, villager -> {
            // NE PAS afficher le nom natif - tout passe par TextDisplay
            villager.customName(null); // Pas de nom custom
            villager.setCustomNameVisible(false); // JAMAIS visible
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);

            if (config.profession != null) {
                villager.setProfession(config.profession);
            }

            // Tags et PDC
            villager.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, config.id);
            villager.addScoreboardTag("zombiez_journey_npc");
            villager.addScoreboardTag("journey_" + config.id);
            villager.addScoreboardTag("no_trading");

            // Orientation
            villager.setRotation(config.location.getYaw(), 0);
        });

        // TextDisplay - TOUTES les informations sont affichées ici
        createDisplayForNPC(config);

        plugin.log(Level.INFO, "§e⚠ NPC Vanilla créé: " + config.id + " (nom via TextDisplay)");

        return npcEntity;
    }

    /**
     * Crée le TextDisplay au-dessus du NPC.
     * C'est le SEUL endroit où les informations du NPC sont affichées (pas de nom natif).
     * Le TextDisplay est conçu pour être plus grand et plus visible qu'un hologramme standard.
     */
    private void createDisplayForNPC(NPCConfig config) {
        if (config.displayLines == null || config.displayLines.length == 0) return;

        // Vérifier si display existe déjà
        TextDisplay existing = displayCache.get(config.id);
        if (existing != null && existing.isValid()) return;

        World world = config.location.getWorld();
        if (world == null) return;

        // Utiliser les constantes par défaut si non spécifiées
        double height = config.displayHeight > 0 ? config.displayHeight : DEFAULT_DISPLAY_HEIGHT;
        float scale = config.displayScale > 0 ? config.displayScale : DEFAULT_DISPLAY_SCALE;

        Location displayLoc = config.location.clone().add(0, height, 0);

        // Construire le texte
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < config.displayLines.length; i++) {
            text.append(config.displayLines[i]);
            if (i < config.displayLines.length - 1) text.append("\n");
        }

        String finalText = text.toString();
        String displayId = config.id;
        float finalScale = scale;

        TextDisplay display = world.spawn(displayLoc, TextDisplay.class, d -> {
            d.setText(finalText);
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setSeeThrough(false);
            // Fond semi-transparent pour meilleure lisibilité
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            // Scale plus grande pour visibilité
            d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(finalScale, finalScale, finalScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
            // View range augmenté pour voir de plus loin
            d.setViewRange(DISPLAY_VIEW_RANGE);
            d.setPersistent(false);
            d.addScoreboardTag("journey_npc_display");
            d.addScoreboardTag("display_" + displayId);
        });

        displayCache.put(config.id, display);
    }

    // ==================== API DE GESTION ====================

    /**
     * Récupère un NPC par son ID
     */
    public Entity getNPC(String npcId) {
        if (citizensEnabled) {
            NPC npc = npcCache.get(npcId);
            if (npc != null && npc.isSpawned()) {
                return npc.getEntity();
            }
        }
        return null;
    }

    /**
     * Vérifie si un NPC existe et est valide
     */
    public boolean isNPCValid(String npcId) {
        Entity entity = getNPC(npcId);
        return entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Supprime un NPC
     */
    public void removeNPC(String npcId) {
        // Citizens NPC
        if (citizensEnabled) {
            NPC npc = npcCache.remove(npcId);
            if (npc != null) {
                npc.destroy();
            }
        }

        // TextDisplay
        TextDisplay display = displayCache.remove(npcId);
        if (display != null && display.isValid()) {
            display.remove();
        }

        // Handler
        interactionHandlers.remove(npcId);
    }

    /**
     * Met à jour le texte du TextDisplay d'un NPC
     */
    public void updateDisplay(String npcId, String... lines) {
        TextDisplay display = displayCache.get(npcId);
        if (display == null || !display.isValid()) return;

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            text.append(lines[i]);
            if (i < lines.length - 1) text.append("\n");
        }

        display.setText(text.toString());
    }

    /**
     * Nettoie tous les NPCs orphelins d'un monde
     */
    public void cleanupOrphanedNPCs(World world) {
        int removed = 0;

        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("zombiez_journey_npc")) {
                String id = entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING);
                if (id != null && !npcCache.containsKey(id)) {
                    entity.remove();
                    removed++;
                }
            }
            if (entity.getScoreboardTags().contains("journey_npc_display")) {
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            plugin.log(Level.INFO, "§7Nettoyé " + removed + " NPCs Journey orphelins");
        }
    }

    // ==================== ÉVÉNEMENTS ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        String npcId = null;

        // Méthode 1: Vérifier le PDC Bukkit
        if (entity.getPersistentDataContainer().has(NPC_ID_KEY, PersistentDataType.STRING)) {
            npcId = entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING);
        }

        // Méthode 2: Si Citizens est actif, vérifier les metadata Citizens
        if (npcId == null && citizensEnabled && entity.hasMetadata("NPC")) {
            // Chercher le NPC dans le cache par son entité
            for (Map.Entry<String, NPC> entry : npcCache.entrySet()) {
                NPC npc = entry.getValue();
                if (npc.isSpawned() && npc.getEntity() != null && npc.getEntity().equals(entity)) {
                    npcId = entry.getKey();
                    // Mettre à jour le PDC pour les prochaines fois
                    entity.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, npcId);
                    break;
                }
            }

            // Si toujours pas trouvé, chercher via les metadata Citizens
            if (npcId == null) {
                for (NPC npc : npcRegistry) {
                    if (npc.isSpawned() && npc.getEntity() != null && npc.getEntity().equals(entity)) {
                        if (npc.data().has("journey_npc_id")) {
                            npcId = (String) npc.data().get("journey_npc_id");
                            // Mettre à jour le PDC pour les prochaines fois
                            entity.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, npcId);
                        }
                        break;
                    }
                }
            }
        }

        // Méthode 3: Fallback par scoreboard tags
        if (npcId == null) {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("journey_") && !tag.equals("journey_npc_display")) {
                    npcId = tag.substring(8); // Enlever "journey_"
                    break;
                }
            }
        }

        if (npcId == null) return;

        // Annuler l'interaction par défaut (trading, etc.)
        event.setCancelled(true);

        // Appeler le handler
        Consumer<PlayerInteractEntityEvent> handler = interactionHandlers.get(npcId);
        if (handler != null) {
            handler.accept(event);
        }
    }

    // ==================== SHUTDOWN ====================

    /**
     * Arrête le manager et nettoie les ressources
     */
    public void shutdown() {
        // Supprimer tous les TextDisplays (non persistants)
        for (TextDisplay display : displayCache.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        displayCache.clear();

        // Les NPCs Citizens sont persistants, on ne les supprime pas
        npcCache.clear();
        interactionHandlers.clear();
    }
}
