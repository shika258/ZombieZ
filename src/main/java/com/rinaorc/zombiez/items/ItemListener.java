package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableRarity;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les événements liés aux items ZombieZ
 * Gère le pickup, l'équipement, et les effets
 */
public class ItemListener implements Listener {

    private final ZombieZPlugin plugin;

    // Compteurs de coups pour les effets spéciaux élémentaires (par joueur)
    private final Map<UUID, Integer> infernoBurstCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> thunderstrikeCounters = new ConcurrentHashMap<>();

    public ItemListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Réinitialise les compteurs d'effets élémentaires pour un joueur
     * À appeler lors de la déconnexion
     */
    public void clearPlayerCounters(UUID playerId) {
        infernoBurstCounters.remove(playerId);
        thunderstrikeCounters.remove(playerId);
    }

    /**
     * Gère le ramassage d'items ZombieZ
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();

        if (!ZombieZItem.isZombieZItem(item)) {
            return;
        }

        // Obtenir la rareté de l'item
        Rarity rarity = ZombieZItem.getItemRarity(item);
        if (rarity == null) return;

        // ============ MISSIONS DE COLLECTION ============
        var missionManager = plugin.getMissionManager();

        // Tracker tous les items ramassés
        missionManager.updateProgress(player,
            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.ITEMS_LOOTED, 1);

        // Tracker les items rares+ (Rare, Epic, Legendary, Mythic, Exalted)
        if (rarity.isAtLeast(Rarity.RARE)) {
            missionManager.updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.RARE_ITEMS_FOUND, 1);
        }

        // Tracker les items épiques+ (Epic, Legendary, Mythic, Exalted)
        if (rarity.isAtLeast(Rarity.EPIC)) {
            missionManager.updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.EPIC_ITEMS_FOUND, 1);
        }

        // ============ ACHIEVEMENTS DE COLLECTION ============
        var achievementManager = plugin.getAchievementManager();
        var playerData = plugin.getPlayerDataManager().getPlayer(player);

        if (playerData != null) {
            // Incrémenter le compteur total d'items ramassés
            playerData.incrementStat("items_looted");
            int totalItemsLooted = (int) playerData.getStat("items_looted");

            // Achievements de collection
            achievementManager.checkAndUnlock(player, "loot_collector_1", totalItemsLooted);
            achievementManager.checkAndUnlock(player, "loot_collector_2", totalItemsLooted);
            achievementManager.checkAndUnlock(player, "hoarder", totalItemsLooted);

            // Achievements de raretés
            if (rarity.isAtLeast(Rarity.EPIC)) {
                achievementManager.incrementProgress(player, "rare_finder", 1);
            }
            if (rarity.isAtLeast(Rarity.LEGENDARY)) {
                achievementManager.incrementProgress(player, "legendary_luck", 1);
            }
            if (rarity == Rarity.MYTHIC || rarity == Rarity.EXALTED) {
                achievementManager.incrementProgress(player, "mythic_finder", 1);
            }
        }

        // Son différent selon rareté
        if (rarity.isAtLeast(Rarity.LEGENDARY)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else if (rarity.isAtLeast(Rarity.EPIC)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        }
    }

    /**
     * Gère le drop d'items - applique le glow et le nom visible pour TOUS les items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();
        Item droppedItem = event.getItemDrop();
        Player player = event.getPlayer();

        // Vérifier si c'est un ZombieZItem
        if (ZombieZItem.isZombieZItem(itemStack)) {
            Rarity rarity = ZombieZItem.getItemRarity(itemStack);

            if (rarity != null) {
                // Appliquer le glow et le nom visible
                String itemName = getItemDisplayName(itemStack);
                plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, getRarityChatColor(rarity));

                // Avertissement pour items légendaires+
                if (rarity.isAtLeast(Rarity.LEGENDARY)) {
                    MessageUtils.sendRaw(player,
                        "§c⚠ Vous avez droppé un item " + rarity.getColoredName() + "§c!");
                }
            }
            return;
        }

        // Vérifier si c'est un Consommable
        if (Consumable.isConsumable(itemStack)) {
            Consumable consumable = Consumable.fromItemStack(itemStack);
            if (consumable != null) {
                ConsumableRarity rarity = consumable.getRarity();
                String itemName = consumable.getType().getDisplayName();
                ChatColor color = getConsumableRarityChatColor(rarity);
                plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, color);
            }
            return;
        }

        // Vérifier si c'est un FoodItem
        String foodId = getFoodId(itemStack);
        if (foodId != null) {
            var foodRegistry = plugin.getPassiveMobManager().getFoodRegistry();
            if (foodRegistry != null) {
                FoodItem foodItem = foodRegistry.getItem(foodId);
                if (foodItem != null) {
                    ChatColor color = getFoodRarityChatColor(foodItem.getRarity());
                    plugin.getItemManager().applyDroppedItemEffects(droppedItem, foodItem.getDisplayName(), color);
                }
            }
            return;
        }

        // Pour tous les autres items (vanilla ou autres), appliquer un glow blanc et le nom
        String itemName = getItemDisplayName(itemStack);
        if (itemName != null && !itemName.isEmpty()) {
            plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, ChatColor.WHITE);
        }
    }

    /**
     * Obtient le nom d'affichage d'un ItemStack
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) return null;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Utiliser le nom custom si présent
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        }

        // Sinon utiliser le nom du matériau formaté
        return formatMaterialName(item.getType());
    }

    /**
     * Formate le nom d'un matériau pour l'affichage
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Obtient la ChatColor correspondant à une Rarity
     */
    private ChatColor getRarityChatColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
            case MYTHIC -> ChatColor.LIGHT_PURPLE;
            case EXALTED -> ChatColor.RED;
        };
    }

    /**
     * Obtient la ChatColor correspondant à une ConsumableRarity
     */
    private ChatColor getConsumableRarityChatColor(ConsumableRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Obtient la ChatColor correspondant à une FoodRarity
     */
    private ChatColor getFoodRarityChatColor(FoodItem.FoodRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Obtient l'ID de nourriture ZombieZ d'un item
     */
    private String getFoodId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        if (!meta.getPersistentDataContainer().has(FoodItem.FOOD_KEY, PersistentDataType.STRING)) {
            return null;
        }
        return meta.getPersistentDataContainer().get(FoodItem.FOOD_KEY, PersistentDataType.STRING);
    }

    /**
     * Gère le changement d'équipement
     * Vérifie la restriction de zone avant de permettre l'équipement
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Vérifier si c'est un slot d'équipement
        int slot = event.getSlot();
        boolean isArmorSlot = slot >= 36 && slot <= 39;
        boolean isOffhand = slot == 40;
        boolean isShiftClickToArmor = false;

        if (!isArmorSlot && !isOffhand) {
            // Vérifier si c'est un shift-click vers l'armure
            if (!event.isShiftClick()) {
                return;
            }
            isShiftClickToArmor = true;
        }

        // Déterminer quel item va être équipé
        ItemStack itemToEquip = null;

        if (isShiftClickToArmor) {
            // Shift-click: l'item cliqué va être équipé
            itemToEquip = event.getCurrentItem();
        } else if (isArmorSlot || isOffhand) {
            // Clic direct sur slot d'armure: l'item sur le curseur va être équipé
            itemToEquip = event.getCursor();
        }

        // Vérifier la restriction de zone pour les items ZombieZ
        if (itemToEquip != null && ZombieZItem.isZombieZItem(itemToEquip)) {
            int itemZone = ZombieZItem.getItemZoneLevel(itemToEquip);
            int playerMaxZone = plugin.getPlayerDataManager().getPlayer(player).getMaxZoneReached();

            if (itemZone > playerMaxZone) {
                // Bloquer l'équipement
                event.setCancelled(true);

                // Message d'erreur
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                MessageUtils.sendRaw(player, "§c✗ Vous devez atteindre la §eZone " + itemZone + " §cpour équiper cet objet!");
                MessageUtils.sendRaw(player, "§7Votre progression actuelle: §fZone " + playerMaxZone);

                // Forcer une mise à jour de l'inventaire pour éviter la désynchronisation
                // et garantir que l'item reste bien dans l'inventaire
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.updateInventory();
                }, 1L);

                return;
            }
        }

        // Invalider le cache de stats du joueur et recalculer les attributs
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemManager().invalidatePlayerStats(player.getUniqueId());

            // Recalculer et appliquer les attributs du joueur
            applyPlayerAttributes(player);

            // Vérifier si un item ZombieZ a été équipé
            ItemStack equipped = event.getCurrentItem();
            if (equipped != null && ZombieZItem.isZombieZItem(equipped)) {
                onItemEquipped(player, equipped);
            }
        }, 1L);
    }

    /**
     * Gère le changement d'item en main
     * Note: La restriction de zone n'est pas appliquée ici car
     * les items en main ne sont pas "équipés" au sens strict.
     * Seuls les slots d'armure et offhand sont restreints.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Invalider le cache
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemManager().invalidatePlayerStats(player.getUniqueId());

            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (newItem != null && ZombieZItem.isZombieZItem(newItem)) {
                // Afficher un avertissement si l'item nécessite une zone non atteinte
                int itemZone = ZombieZItem.getItemZoneLevel(newItem);
                int playerMaxZone = plugin.getPlayerDataManager().getPlayer(player).getMaxZoneReached();

                if (itemZone > playerMaxZone) {
                    MessageUtils.sendActionBar(player, "§c⚠ Zone " + itemZone + " requise §7(vous: Zone " + playerMaxZone + ")");
                } else {
                    showItemStats(player, newItem);
                }
            }
        }, 1L);
    }

    /**
     * Bloque l'équipement d'armure par clic droit si la zone n'est pas débloquée
     * C'est le cas principal où les items disparaissaient car le clic droit
     * équipe l'armure via le comportement vanilla avant que le système puisse intervenir
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorRightClick(PlayerInteractEvent event) {
        // Ignorer les clics gauches
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ignorer la main secondaire
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !ZombieZItem.isZombieZItem(item)) {
            return;
        }

        // Vérifier si c'est une pièce d'armure qui peut être équipée par clic droit
        if (!isEquippableArmor(item.getType())) {
            return;
        }

        // Vérifier la restriction de zone
        int itemZone = ZombieZItem.getItemZoneLevel(item);
        int playerMaxZone = plugin.getPlayerDataManager().getPlayer(player).getMaxZoneReached();

        if (itemZone > playerMaxZone) {
            // Bloquer l'équipement par clic droit
            event.setCancelled(true);

            // Message d'erreur
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.sendRaw(player, "§c✗ Vous devez atteindre la §eZone " + itemZone + " §cpour équiper cet objet!");
            MessageUtils.sendRaw(player, "§7Votre progression actuelle: §fZone " + playerMaxZone);
        }
    }

    /**
     * Bloque l'échange d'items entre main principale et secondaire
     * si l'item de la main principale ne peut pas aller en offhand à cause de la zone
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offhandItem = event.getOffHandItem(); // L'item qui va dans l'offhand

        if (offhandItem == null || !ZombieZItem.isZombieZItem(offhandItem)) {
            return;
        }

        // Vérifier la restriction de zone pour l'offhand
        int itemZone = ZombieZItem.getItemZoneLevel(offhandItem);
        int playerMaxZone = plugin.getPlayerDataManager().getPlayer(player).getMaxZoneReached();

        if (itemZone > playerMaxZone) {
            // Bloquer l'échange
            event.setCancelled(true);

            // Message d'erreur
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.sendRaw(player, "§c✗ Vous devez atteindre la §eZone " + itemZone + " §cpour équiper cet objet!");
            MessageUtils.sendRaw(player, "§7Votre progression actuelle: §fZone " + playerMaxZone);
        }
    }

    /**
     * Vérifie si un matériau est une pièce d'armure équipable par clic droit
     */
    private boolean isEquippableArmor(Material material) {
        return switch (material) {
            // Casques
            case LEATHER_HELMET, CHAINMAIL_HELMET, IRON_HELMET, GOLDEN_HELMET, DIAMOND_HELMET, NETHERITE_HELMET,
            // Plastrons
            LEATHER_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE,
            // Jambières
            LEATHER_LEGGINGS, CHAINMAIL_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, DIAMOND_LEGGINGS, NETHERITE_LEGGINGS,
            // Bottes
            LEATHER_BOOTS, CHAINMAIL_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, DIAMOND_BOOTS, NETHERITE_BOOTS,
            // Têtes spéciales équipables
            CARVED_PUMPKIN, PLAYER_HEAD, ZOMBIE_HEAD, SKELETON_SKULL, WITHER_SKELETON_SKULL, CREEPER_HEAD, DRAGON_HEAD, PIGLIN_HEAD,
            // Elytres (équipable sur le torse)
            ELYTRA -> true;
            default -> false;
        };
    }

    /**
     * Appelé quand un item ZombieZ est équipé
     */
    private void onItemEquipped(Player player, ItemStack item) {
        ZombieZItem zItem = plugin.getItemManager().getItem(item);
        if (zItem == null) return;

        // Afficher les stats gagnées
        Map<StatType, Double> stats = zItem.getTotalStats();
        
        StringBuilder message = new StringBuilder("§a✓ Équipé: ");
        message.append(zItem.getRarity().getChatColor()).append(zItem.getGeneratedName());
        
        // Stats principales
        if (stats.containsKey(StatType.DAMAGE)) {
            message.append(" §c+").append(String.format("%.1f", stats.get(StatType.DAMAGE))).append(" DMG");
        }
        if (stats.containsKey(StatType.ARMOR)) {
            message.append(" §9+").append(String.format("%.1f", stats.get(StatType.ARMOR))).append(" ARM");
        }
        
        MessageUtils.sendActionBar(player, message.toString());
    }

    /**
     * Affiche les stats d'un item en main dans l'action bar
     */
    private void showItemStats(Player player, ItemStack item) {
        ZombieZItem zItem = plugin.getItemManager().getItem(item);
        if (zItem == null) return;

        String message = zItem.getRarity().getChatColor() + zItem.getGeneratedName() + 
            " §8| §7IS: §f" + zItem.getItemScore();
        
        MessageUtils.sendActionBar(player, message);
    }

    /**
     * Applique les effets spéciaux des items (appelé depuis CombatListener)
     */
    public void processSpecialEffects(Player attacker, org.bukkit.entity.LivingEntity target, double damage) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        
        if (!ZombieZItem.isZombieZItem(weapon)) {
            return;
        }

        ZombieZItem zItem = plugin.getItemManager().getItem(weapon);
        if (zItem == null) return;

        for (String effect : zItem.getSpecialEffects()) {
            processEffect(effect, attacker, target, damage);
        }
    }

    /**
     * Traite un effet spécial spécifique
     */
    private void processEffect(String effectId, Player attacker, org.bukkit.entity.LivingEntity target, double damage) {
        switch (effectId) {
            case "ignite" -> {
                if (Math.random() < 0.15) { // 15% chance
                    target.setFireTicks(60); // 3 secondes
                }
            }
            case "slow" -> {
                if (Math.random() < 0.20) { // 20% chance
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1
                    ));
                }
            }
            case "freeze" -> {
                if (Math.random() < 0.10) { // 10% chance
                    target.setFreezeTicks(60);
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 2
                    ));
                }
            }
            case "poison" -> {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.POISON, 60, 0
                ));
            }
            case "chain_lightning" -> {
                if (Math.random() < 0.15) { // 15% chance
                    // Trouver une cible proche
                    target.getWorld().getNearbyEntities(target.getLocation(), 5, 5, 5).stream()
                        .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                        .filter(e -> e != target && e != attacker)
                        .findFirst()
                        .ifPresent(nearby -> {
                            ((org.bukkit.entity.LivingEntity) nearby).damage(damage * 0.5, attacker);
                            // Effet visuel - arc électrique entre les deux cibles
                            spawnChainLightningArc(target.getLocation(), nearby.getLocation());
                            nearby.getWorld().playSound(nearby.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);
                        });
                }
            }
            case "inferno_burst" -> {
                // Explosion de feu tous les 5 coups
                UUID playerId = attacker.getUniqueId();
                int hitCount = infernoBurstCounters.getOrDefault(playerId, 0) + 1;
                infernoBurstCounters.put(playerId, hitCount);

                if (hitCount >= 5) {
                    infernoBurstCounters.put(playerId, 0);
                    triggerInfernoBurst(attacker, target, damage);
                }
            }
            case "thunderstrike" -> {
                // Éclair puissant tous les 10 coups
                UUID playerId = attacker.getUniqueId();
                int hitCount = thunderstrikeCounters.getOrDefault(playerId, 0) + 1;
                thunderstrikeCounters.put(playerId, hitCount);

                if (hitCount >= 10) {
                    thunderstrikeCounters.put(playerId, 0);
                    triggerThunderstrike(attacker, target, damage);
                }
            }
            case "plague_spread" -> {
                // Propager le poison aux ennemis proches
                target.getWorld().getNearbyEntities(target.getLocation(), 3, 3, 3).stream()
                    .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                    .filter(e -> e != attacker)
                    .forEach(e -> {
                        ((org.bukkit.entity.LivingEntity) e).addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.POISON, 40, 0
                            ));
                    });
            }
        }
    }

    /**
     * Calcule les dégâts modifiés par les stats de l'arme
     */
    public double calculateModifiedDamage(Player attacker, double baseDamage) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(attacker);
        
        double damage = baseDamage;
        
        // Bonus de dégâts en %
        if (stats.containsKey(StatType.DAMAGE_PERCENT)) {
            damage *= (1 + stats.get(StatType.DAMAGE_PERCENT) / 100.0);
        }
        
        // Dégâts élémentaires
        damage += stats.getOrDefault(StatType.FIRE_DAMAGE, 0.0);
        damage += stats.getOrDefault(StatType.ICE_DAMAGE, 0.0);
        damage += stats.getOrDefault(StatType.LIGHTNING_DAMAGE, 0.0);
        
        // Chance critique
        double critChance = stats.getOrDefault(StatType.CRIT_CHANCE, 0.0) / 100.0;
        if (Math.random() < critChance) {
            double critDamage = 1.5 + (stats.getOrDefault(StatType.CRIT_DAMAGE, 0.0) / 100.0);
            damage *= critDamage;
            
            // Effet visuel crit
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        }
        
        return damage;
    }

    /**
     * Calcule les dégâts reçus modifiés par l'armure
     */
    public double calculateDamageReduction(Player defender, double incomingDamage) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(defender);

        double damage = incomingDamage;

        // Réduction de dégâts en %
        if (stats.containsKey(StatType.DAMAGE_REDUCTION)) {
            damage *= (1 - stats.get(StatType.DAMAGE_REDUCTION) / 100.0);
        }

        // Chance d'esquive
        double dodgeChance = stats.getOrDefault(StatType.DODGE_CHANCE, 0.0) / 100.0;
        if (Math.random() < dodgeChance) {
            MessageUtils.sendActionBar(defender, "§a✧ ESQUIVÉ!");
            return 0;
        }

        return Math.max(0, damage);
    }

    // Clés uniques pour les modifiers ZombieZ
    private static final NamespacedKey ZOMBIEZ_HEALTH_KEY = new NamespacedKey("zombiez", "max_health_bonus");
    private static final NamespacedKey ZOMBIEZ_ATTACK_SPEED_KEY = new NamespacedKey("zombiez", "attack_speed_bonus");
    private static final NamespacedKey ZOMBIEZ_MOVEMENT_SPEED_KEY = new NamespacedKey("zombiez", "movement_speed_bonus");

    /**
     * Applique les attributs du joueur basés sur son équipement ZombieZ
     * Notamment le bonus de vie maximale (MAX_HEALTH), vitesse d'attaque (ATTACK_SPEED)
     * et vitesse de déplacement (MOVEMENT_SPEED)
     */
    public void applyPlayerAttributes(Player player) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(player);

        // Appliquer le bonus de vie maximale
        double healthBonus = stats.getOrDefault(StatType.MAX_HEALTH, 0.0);
        applyMaxHealthBonus(player, healthBonus);

        // Appliquer le bonus de vitesse d'attaque
        // ATTACK_SPEED est flat, ATTACK_SPEED_PERCENT est un pourcentage
        double attackSpeedFlat = stats.getOrDefault(StatType.ATTACK_SPEED, 0.0);
        double attackSpeedPercent = stats.getOrDefault(StatType.ATTACK_SPEED_PERCENT, 0.0);
        applyAttackSpeedBonus(player, attackSpeedFlat, attackSpeedPercent);

        // Appliquer le bonus de vitesse de déplacement
        double movementSpeedPercent = stats.getOrDefault(StatType.MOVEMENT_SPEED, 0.0);
        applyMovementSpeedBonus(player, movementSpeedPercent);
    }

    /**
     * Applique le bonus de vie maximale au joueur
     */
    private void applyMaxHealthBonus(Player player, double bonus) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // Supprimer l'ancien modifier ZombieZ s'il existe
        maxHealthAttr.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(ZOMBIEZ_HEALTH_KEY))
            .findFirst()
            .ifPresent(maxHealthAttr::removeModifier);

        // Ajouter le nouveau modifier si le bonus est positif
        if (bonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                ZOMBIEZ_HEALTH_KEY,
                bonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            maxHealthAttr.addModifier(modifier);
        }

        // S'assurer que la vie actuelle ne dépasse pas le nouveau max
        double newMaxHealth = maxHealthAttr.getValue();
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    /**
     * Applique le bonus de vitesse d'attaque au joueur
     * Modifie l'attribut GENERIC_ATTACK_SPEED pour que l'indicateur visuel de Minecraft
     * (la barre de cooldown sous le curseur) se recharge plus ou moins vite
     *
     * @param player Le joueur
     * @param flatBonus Bonus flat de vitesse d'attaque (ex: +0.5)
     * @param percentBonus Bonus en pourcentage (ex: 20 = +20%)
     */
    private void applyAttackSpeedBonus(Player player, double flatBonus, double percentBonus) {
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeedAttr == null) return;

        // Supprimer l'ancien modifier ZombieZ s'il existe
        attackSpeedAttr.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(ZOMBIEZ_ATTACK_SPEED_KEY))
            .findFirst()
            .ifPresent(attackSpeedAttr::removeModifier);

        // Calculer le bonus total
        // La vitesse d'attaque de base est 4.0 (épée) pour Minecraft vanilla
        // Le bonus percent s'applique sur la base
        double baseSpeed = attackSpeedAttr.getBaseValue();
        double percentBonusValue = baseSpeed * (percentBonus / 100.0);
        double totalBonus = flatBonus + percentBonusValue;

        // Ajouter le nouveau modifier si le bonus est non-nul
        if (Math.abs(totalBonus) > 0.001) {
            AttributeModifier modifier = new AttributeModifier(
                ZOMBIEZ_ATTACK_SPEED_KEY,
                totalBonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            attackSpeedAttr.addModifier(modifier);
        }
    }

    /**
     * Applique le bonus de vitesse de déplacement au joueur
     * La stat MOVEMENT_SPEED est un pourcentage (ex: 20 = +20% de vitesse)
     * La vitesse de base d'un joueur Minecraft est 0.1
     *
     * @param player Le joueur
     * @param percentBonus Bonus en pourcentage (ex: 20 = +20%)
     */
    private void applyMovementSpeedBonus(Player player, double percentBonus) {
        AttributeInstance movementSpeedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeedAttr == null) return;

        // Supprimer l'ancien modifier ZombieZ s'il existe
        movementSpeedAttr.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(ZOMBIEZ_MOVEMENT_SPEED_KEY))
            .findFirst()
            .ifPresent(movementSpeedAttr::removeModifier);

        // Calculer le bonus
        // La vitesse de base d'un joueur est 0.1 (sans effets)
        // Un bonus de 50% = 0.1 * 0.5 = +0.05 de vitesse
        double baseSpeed = movementSpeedAttr.getBaseValue(); // Normalement 0.1
        double bonusValue = baseSpeed * (percentBonus / 100.0);

        // Ajouter le nouveau modifier si le bonus est non-nul
        if (Math.abs(bonusValue) > 0.0001) {
            AttributeModifier modifier = new AttributeModifier(
                ZOMBIEZ_MOVEMENT_SPEED_KEY,
                bonusValue,
                AttributeModifier.Operation.ADD_NUMBER
            );
            movementSpeedAttr.addModifier(modifier);
        }
    }

    /**
     * Supprime tous les modifiers ZombieZ d'un joueur (pour cleanup)
     */
    public void removeAllModifiers(Player player) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(ZOMBIEZ_HEALTH_KEY))
                .findFirst()
                .ifPresent(maxHealthAttr::removeModifier);
        }

        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            attackSpeedAttr.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(ZOMBIEZ_ATTACK_SPEED_KEY))
                .findFirst()
                .ifPresent(attackSpeedAttr::removeModifier);
        }

        AttributeInstance movementSpeedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeedAttr != null) {
            movementSpeedAttr.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(ZOMBIEZ_MOVEMENT_SPEED_KEY))
                .findFirst()
                .ifPresent(movementSpeedAttr::removeModifier);
        }
    }

    /**
     * Génère un arc électrique en particules entre deux points
     */
    private void spawnChainLightningArc(Location from, Location to) {
        if (from.getWorld() == null) return;

        Location start = from.clone().add(0, 1, 0);
        Location end = to.clone().add(0, 1, 0);

        // Calculer la direction et la distance
        double distance = start.distance(end);
        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector()).normalize();

        // Dessiner l'arc avec des zigzags
        int segments = (int) (distance * 4);
        for (int i = 0; i <= segments; i++) {
            double progress = (double) i / segments;
            Location point = start.clone().add(direction.clone().multiply(distance * progress));

            // Ajouter du zigzag aléatoire (plus au milieu, moins aux extrémités)
            double zigzagIntensity = Math.sin(progress * Math.PI) * 0.3;
            point.add(
                (Math.random() - 0.5) * zigzagIntensity,
                (Math.random() - 0.5) * zigzagIntensity,
                (Math.random() - 0.5) * zigzagIntensity
            );

            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.05, 0.05, 0.05, 0);
        }

        // Impacts aux deux extrémités
        from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, start, 8, 0.2, 0.2, 0.2, 0.02);
        from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, end, 10, 0.3, 0.3, 0.3, 0.03);
    }

    /**
     * Déclenche l'explosion de feu (Inferno Burst)
     * Effet: AOE de feu autour de la cible, dégâts + mise en feu des ennemis
     */
    private void triggerInfernoBurst(Player attacker, org.bukkit.entity.LivingEntity target, double baseDamage) {
        Location center = target.getLocation();

        // Notification au joueur
        MessageUtils.sendActionBar(attacker, "§6§l🔥 INFERNO BURST! §c+50% dégâts feu");

        // Son d'explosion de feu
        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

        // Particules d'explosion de feu (optimisées - moins nombreuses mais plus impactantes)
        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 25, 1.5, 0.8, 1.5, 0.08);
        center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.5, 0), 8, 1.2, 0.3, 1.2);

        // Dégâts AOE (rayon 3 blocs)
        double aoeDamage = baseDamage * 0.5; // 50% des dégâts de base
        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, 3, 3, 3)) {
            if (entity instanceof org.bukkit.entity.LivingEntity livingEntity && entity != attacker) {
                if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
                    continue;
                }

                double distance = entity.getLocation().distance(center);
                double distanceFactor = 1.0 - (distance / 3.0) * 0.4;
                double finalDamage = aoeDamage * distanceFactor;

                livingEntity.damage(finalDamage, attacker);
                livingEntity.setFireTicks(80); // 4 secondes de feu

                // Petite particule sur chaque cible touchée
                livingEntity.getWorld().spawnParticle(Particle.FLAME, livingEntity.getEyeLocation(), 6, 0.2, 0.3, 0.2, 0.03);
            }
        }
    }

    /**
     * Déclenche l'éclair puissant (Thunderstrike)
     * Effet: Éclair sur la cible + dégâts massifs + étourdissement
     */
    private void triggerThunderstrike(Player attacker, org.bukkit.entity.LivingEntity target, double baseDamage) {
        Location center = target.getLocation();

        // Notification au joueur
        MessageUtils.sendActionBar(attacker, "§e§l⚡ THUNDERSTRIKE! §f+100% dégâts foudre");

        // Faire apparaître un vrai éclair pour l'effet visuel
        center.getWorld().strikeLightningEffect(center);

        // Particules électriques supplémentaires (optimisées)
        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0), 20, 0.8, 1, 0.8, 0.1);

        // Dégâts massifs sur la cible principale
        double thunderDamage = baseDamage * 1.0; // 100% des dégâts de base en bonus
        target.damage(thunderDamage, attacker);

        // Effet d'étourdissement (Slowness extrême + No Jump)
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 4, false, false // 1.5s
        ));
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 1, false, false
        ));

        // Dégâts AOE réduits aux ennemis proches (rayon 2.5 blocs)
        double aoeDamage = baseDamage * 0.3;
        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5)) {
            if (entity instanceof org.bukkit.entity.LivingEntity livingEntity
                && entity != attacker && entity != target) {

                if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
                    continue;
                }

                livingEntity.damage(aoeDamage, attacker);

                // Arc électrique vers la cible secondaire
                spawnChainLightningArc(center, livingEntity.getLocation());
            }
        }
    }
}
