package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.generator.ItemGenerator;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

/**
 * Listener pour les connexions et déconnexions des joueurs
 * Gère le chargement/sauvegarde des données et l'initialisation du HUD
 */
public class PlayerConnectionListener implements Listener {

    private final ZombieZPlugin plugin;

    public PlayerConnectionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Pré-chargement des données (async, avant que le joueur soit vraiment connecté)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // On pourrait pré-charger les données ici si nécessaire
        // Mais on préfère le faire au PlayerJoinEvent pour avoir accès au Player
    }

    /**
     * Connexion d'un joueur
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Supprimer le message de join par défaut
        event.joinMessage(null);

        // Charger les données du joueur de manière async
        plugin.getPlayerDataManager().loadPlayerAsync(player).thenAccept(data -> {
            // Une fois chargé, initialiser sur le main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                onPlayerDataLoaded(player, data);
            });
        }).exceptionally(e -> {
            plugin.log(Level.SEVERE, "§cErreur chargement données de " + player.getName() + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Appelé quand les données du joueur sont chargées
     */
    private void onPlayerDataLoaded(Player player, PlayerData data) {
        if (!player.isOnline()) return;

        // Vérifier la zone actuelle
        plugin.getZoneManager().checkPlayerZone(player);

        // Message de bienvenue
        boolean isNew = data.getKills().get() == 0 && data.getPlaytime().get() < 60;

        if (isNew) {
            // Nouveau joueur - donner le stuff de départ
            giveStarterKit(player);
            sendWelcomeMessage(player);
            MessageUtils.broadcast("§a+ §7Bienvenue à §e" + player.getName() + " §7dans l'apocalypse!");
        } else {
            // Joueur existant
            sendReturnMessage(player, data);
            MessageUtils.broadcast("§a+ §7" + player.getName() + " §7a rejoint le serveur");
        }

        // Appliquer les attributs basés sur l'équipement (ex: bonus de vie max)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemListener().applyPlayerAttributes(player);
        }, 5L);

        // Log
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.log(Level.INFO, "§7Joueur " + player.getName() + " chargé (Niveau " +
                data.getLevel().get() + ", Zone " + data.getCurrentZone().get() + ")");
        }
    }

    /**
     * Donne le stuff de départ aux nouveaux joueurs
     * Utilise le système de stats custom ZombieZ
     */
    private void giveStarterKit(Player player) {
        // Vider l'inventaire au cas où
        player.getInventory().clear();

        // Créer les items avec le système de stats custom
        ItemGenerator generator = ItemGenerator.getInstance();

        // ═══════════════════════════════════════════════
        // ARME DE DÉPART: Épée du Survivant (Uncommon)
        // ═══════════════════════════════════════════════
        ZombieZItem starterSword = createStarterWeapon(generator);
        player.getInventory().setItem(0, starterSword.toItemStack());

        // ═══════════════════════════════════════════════
        // ARMURE DE DÉPART: Set du Survivant (Common/Uncommon)
        // ═══════════════════════════════════════════════
        ZombieZItem starterHelmet = createStarterArmor(generator, ItemType.HELMET);
        ZombieZItem starterChestplate = createStarterArmor(generator, ItemType.CHESTPLATE);
        ZombieZItem starterLeggings = createStarterArmor(generator, ItemType.LEGGINGS);
        ZombieZItem starterBoots = createStarterArmor(generator, ItemType.BOOTS);

        player.getInventory().setHelmet(starterHelmet.toItemStack());
        player.getInventory().setChestplate(starterChestplate.toItemStack());
        player.getInventory().setLeggings(starterLeggings.toItemStack());
        player.getInventory().setBoots(starterBoots.toItemStack());

        // ═══════════════════════════════════════════════
        // CONSOMMABLES
        // ═══════════════════════════════════════════════
        // Nourriture
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 32);
        ItemMeta foodMeta = food.getItemMeta();
        if (foodMeta != null) {
            foodMeta.setDisplayName("§6Rations de Survie");
            foodMeta.setLore(List.of("§7De la viande pour survivre.", "§e§oÉquipement de départ"));
            food.setItemMeta(foodMeta);
        }
        player.getInventory().setItem(1, food);

        // Torches
        ItemStack torches = new ItemStack(Material.TORCH, 16);
        player.getInventory().setItem(2, torches);

        // Message de confirmation
        MessageUtils.sendRaw(player, "§a✓ §7Vous avez reçu votre §6équipement de départ §7avec stats custom!");
    }

    /**
     * Crée l'épée de départ avec stats custom
     */
    private ZombieZItem createStarterWeapon(ItemGenerator generator) {
        // Stats de base pour l'épée de départ
        Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
        baseStats.put(StatType.DAMAGE, 7.0);          // Dégâts de base
        baseStats.put(StatType.ATTACK_SPEED, 1.6);    // Vitesse d'attaque standard

        // Créer un item avec des stats prédéfinies
        return ZombieZItem.builder()
            .uuid(UUID.randomUUID())
            .itemType(ItemType.SWORD)
            .material(Material.IRON_SWORD)
            .rarity(Rarity.UNCOMMON)
            .tier(1)
            .zoneLevel(1)
            .baseName("Épée du Survivant")
            .generatedName("⚔ Épée du Survivant")
            .baseStats(baseStats)
            .affixes(new ArrayList<>())
            .itemScore(50)
            .createdAt(System.currentTimeMillis())
            .identified(true)
            .itemLevel(5)
            .build();
    }

    /**
     * Crée une pièce d'armure de départ avec stats custom
     */
    private ZombieZItem createStarterArmor(ItemGenerator generator, ItemType armorType) {
        // Déterminer le matériau et les stats selon le type d'armure
        Material material;
        Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
        String baseName;
        double armor;

        switch (armorType) {
            case HELMET -> {
                material = Material.LEATHER_HELMET;
                armor = 1.5;
                baseName = "Casque du Survivant";
            }
            case CHESTPLATE -> {
                material = Material.LEATHER_CHESTPLATE;
                armor = 4.0;
                baseName = "Plastron du Survivant";
            }
            case LEGGINGS -> {
                material = Material.LEATHER_LEGGINGS;
                armor = 3.0;
                baseName = "Jambières du Survivant";
            }
            case BOOTS -> {
                material = Material.LEATHER_BOOTS;
                armor = 1.5;
                baseName = "Bottes du Survivant";
            }
            default -> {
                material = Material.LEATHER_CHESTPLATE;
                armor = 2.0;
                baseName = "Armure du Survivant";
            }
        }

        baseStats.put(StatType.ARMOR, armor);
        // Petit bonus de vie sur l'armure de départ
        baseStats.put(StatType.MAX_HEALTH, 2.0);

        return ZombieZItem.builder()
            .uuid(UUID.randomUUID())
            .itemType(armorType)
            .material(material)
            .rarity(Rarity.COMMON)
            .tier(0)
            .zoneLevel(1)
            .baseName(baseName)
            .generatedName("🛡 " + baseName)
            .baseStats(baseStats)
            .affixes(new ArrayList<>())
            .itemScore(25)
            .createdAt(System.currentTimeMillis())
            .identified(true)
            .itemLevel(3)
            .build();
    }

    /**
     * Envoie le message de bienvenue aux nouveaux joueurs
     */
    private void sendWelcomeMessage(Player player) {
        MessageUtils.sendTitle(player, "§6§lZOMBIEZ", "§7Bienvenue dans l'apocalypse", 20, 60, 20);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§8§m                                                  ");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §6§lBIENVENUE SUR ZOMBIEZ!");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §7▸ Tue des zombies pour gagner des §ePoints");
            MessageUtils.sendRaw(player, "  §7▸ Avance vers le §bNord §7pour plus de défis");
            MessageUtils.sendRaw(player, "  §7▸ Collecte des items §5Légendaires §7uniques");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §a/zone §7- Voir ta zone actuelle");
            MessageUtils.sendRaw(player, "  §a/stats §7- Voir tes statistiques");
            MessageUtils.sendRaw(player, "  §a/refuge §7- Trouver le refuge le plus proche");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§8§m                                                  ");
            MessageUtils.sendRaw(player, "");
        }, 40L);
    }

    /**
     * Envoie le message de retour aux joueurs existants
     */
    private void sendReturnMessage(Player player, PlayerData data) {
        String zone = "Zone " + data.getCurrentZone().get();
        String time = MessageUtils.formatTime(data.getPlaytime().get());
        
        MessageUtils.sendTitle(player, "§a§lBon retour!", "§7" + zone + " • " + time + " de jeu", 10, 40, 10);
        
        // Résumé rapide
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.send(player, "§7Niveau §e" + data.getLevel().get() + 
                " §7| §c" + data.getKills().get() + " §7kills | §6" + 
                EconomyManager.formatCompact(data.getPoints().get()) + " §7points");
        }, 20L);
    }

    /**
     * Déconnexion d'un joueur
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Supprimer le message de quit par défaut
        event.quitMessage(null);
        
        // Broadcast
        MessageUtils.broadcast("§c- §7" + player.getName() + " §7a quitté le serveur");

        // Supprimer du cache de zone
        plugin.getZoneManager().removeFromCache(player.getUniqueId());
        
        // Nettoyer le cache de déplacement (FIX: fuite mémoire)
        if (plugin.getPlayerMoveListener() != null) {
            plugin.getPlayerMoveListener().removeFromCache(player.getUniqueId());
        }
        
        // Nettoyer le momentum (garder les records mais nettoyer l'état temporaire)
        if (plugin.getMomentumManager() != null) {
            plugin.getMomentumManager().onPlayerQuit(player);
        }
        
        // Nettoyer les invitations de party en attente
        if (plugin.getPartyManager() != null) {
            plugin.getPartyManager().onPlayerQuit(player);
        }

        // Sauvegarder et décharger les données (async)
        plugin.getPlayerDataManager().unloadPlayer(player.getUniqueId()).thenRun(() -> {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.log(Level.INFO, "§7Joueur " + player.getName() + " sauvegardé et déchargé");
            }
        });
    }
}
