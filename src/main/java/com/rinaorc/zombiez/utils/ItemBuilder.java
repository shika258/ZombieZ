package com.rinaorc.zombiez.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder fluide pour créer des ItemStacks customisés
 * Optimisé pour le système de loot procédural
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private List<Component> lore;

    /**
     * Crée un nouveau builder avec le matériau spécifié
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
        this.lore = new ArrayList<>();
    }

    /**
     * Crée un builder depuis un ItemStack existant
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
        this.lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
    }

    /**
     * Définit le nom de l'item
     */
    public ItemBuilder name(String name) {
        meta.displayName(Component.text(MessageUtils.colorize(name)));
        return this;
    }

    /**
     * Définit le nom avec un Component
     */
    public ItemBuilder name(Component name) {
        meta.displayName(name);
        return this;
    }

    /**
     * Ajoute une ligne de lore
     */
    public ItemBuilder lore(String line) {
        lore.add(Component.text(MessageUtils.colorize(line)));
        return this;
    }

    /**
     * Ajoute plusieurs lignes de lore
     */
    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            lore.add(Component.text(MessageUtils.colorize(line)));
        }
        return this;
    }

    /**
     * Ajoute une liste de lore
     */
    public ItemBuilder lore(List<String> lines) {
        for (String line : lines) {
            lore.add(Component.text(MessageUtils.colorize(line)));
        }
        return this;
    }

    /**
     * Définit tout le lore
     */
    public ItemBuilder setLore(List<String> lines) {
        lore.clear();
        return lore(lines);
    }

    /**
     * Ajoute une ligne vide au lore
     */
    public ItemBuilder loreLine() {
        lore.add(Component.empty());
        return this;
    }

    /**
     * Définit la quantité
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * Ajoute un enchantement
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * Ajoute un enchantement glowing (sans enchantement réel)
     */
    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }
    
    /**
     * Ajoute un enchantement glowing conditionnellement
     */
    public ItemBuilder glow(boolean condition) {
        if (condition) {
            return glow();
        }
        return this;
    }
    
    /**
     * Alias pour lore - compatibilité
     */
    public ItemBuilder addLore(String line) {
        return lore(line);
    }
    
    /**
     * Alias pour lore avec liste - compatibilité
     */
    public ItemBuilder addLore(List<String> lines) {
        return lore(lines);
    }

    /**
     * Ajoute des flags d'item
     */
    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    /**
     * Cache tous les attributs
     */
    public ItemBuilder hideAttributes() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    /**
     * Cache tout (enchants, attributs, etc.)
     */
    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    /**
     * Cache les tooltips vanilla des attributs par défaut (dégâts d'attaque, vitesse)
     * En définissant un modificateur vide, on supprime les attributs par défaut du matériau
     */
    public ItemBuilder hideDefaultAttributes() {
        // Définir un multimap vide pour supprimer les attributs par défaut du matériau
        meta.setAttributeModifiers(com.google.common.collect.HashMultimap.create());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    /**
     * Rend l'item incassable
     */
    public ItemBuilder unbreakable() {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Définit la durabilité (dégâts)
     */
    public ItemBuilder durability(int damage) {
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(damage);
        }
        return this;
    }

    /**
     * Définit le modèle custom (pour resource packs)
     */
    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    /**
     * Ajoute un attribut modifier
     */
    public ItemBuilder attribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        EquipmentSlotGroup slot = getSlotForMaterial(item.getType());
        AttributeModifier modifier = new AttributeModifier(
            NamespacedKey.minecraft(attribute.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8)),
            amount,
            operation,
            slot
        );
        meta.addAttributeModifier(attribute, modifier);
        return this;
    }

    /**
     * Ajoute des dégâts d'attaque
     */
    public ItemBuilder attackDamage(double damage) {
        return attribute(Attribute.ATTACK_DAMAGE, damage, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute de la vitesse d'attaque
     */
    public ItemBuilder attackSpeed(double speed) {
        return attribute(Attribute.ATTACK_SPEED, speed, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute de l'armure
     */
    public ItemBuilder armor(double armor) {
        return attribute(Attribute.ARMOR, armor, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute de la résistance d'armure
     */
    public ItemBuilder armorToughness(double toughness) {
        return attribute(Attribute.ARMOR_TOUGHNESS, toughness, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute de la vie max
     */
    public ItemBuilder maxHealth(double health) {
        return attribute(Attribute.MAX_HEALTH, health, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute de la vitesse de mouvement
     */
    public ItemBuilder movementSpeed(double speed) {
        return attribute(Attribute.MOVEMENT_SPEED, speed, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Ajoute du knockback resistance
     */
    public ItemBuilder knockbackResistance(double resistance) {
        return attribute(Attribute.KNOCKBACK_RESISTANCE, resistance, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Définit la couleur (pour armure en cuir)
     */
    public ItemBuilder color(Color color) {
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        }
        return this;
    }

    /**
     * Définit la couleur avec RGB
     */
    public ItemBuilder color(int r, int g, int b) {
        return color(Color.fromRGB(r, g, b));
    }

    /**
     * Ajoute une donnée persistante (String)
     */
    public ItemBuilder data(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    /**
     * Ajoute une donnée persistante (Integer)
     */
    public ItemBuilder data(NamespacedKey key, int value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        return this;
    }

    /**
     * Ajoute une donnée persistante (Double)
     */
    public ItemBuilder data(NamespacedKey key, double value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
        return this;
    }

    /**
     * Ajoute une donnée persistante (Long)
     */
    public ItemBuilder data(NamespacedKey key, long value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, value);
        return this;
    }

    /**
     * Ajoute une donnée persistante (Boolean via Byte)
     */
    public ItemBuilder data(NamespacedKey key, boolean value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
        return this;
    }

    /**
     * Construit l'ItemStack final
     */
    public ItemStack build() {
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Détermine le slot d'équipement basé sur le matériau
     */
    private EquipmentSlotGroup getSlotForMaterial(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || name.equals("PLAYER_HEAD") || name.equals("CARVED_PUMPKIN")) {
            return EquipmentSlotGroup.HEAD;
        } else if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) {
            return EquipmentSlotGroup.CHEST;
        } else if (name.endsWith("_LEGGINGS")) {
            return EquipmentSlotGroup.LEGS;
        } else if (name.endsWith("_BOOTS")) {
            return EquipmentSlotGroup.FEET;
        } else if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE") ||
                   name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("TRIDENT") ||
                   name.equals("MACE") || name.equals("BOW") || name.equals("CROSSBOW")) {
            return EquipmentSlotGroup.MAINHAND;
        } else if (name.equals("SHIELD")) {
            return EquipmentSlotGroup.OFFHAND;
        }
        return EquipmentSlotGroup.ANY;
    }

    // ==================== MÉTHODES STATIQUES UTILITAIRES ====================

    /**
     * Crée rapidement un item simple
     */
    public static ItemStack create(Material material, String name) {
        return new ItemBuilder(material).name(name).build();
    }

    /**
     * Crée rapidement un item avec lore
     */
    public static ItemStack create(Material material, String name, String... lore) {
        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    /**
     * Crée un placeholder (item gris pour les GUIs)
     */
    public static ItemStack placeholder() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
    }

    /**
     * Crée un placeholder avec couleur
     */
    public static ItemStack placeholder(Material glassPaneMaterial) {
        return new ItemBuilder(glassPaneMaterial)
            .name(" ")
            .build();
    }

    /**
     * Crée une tête de joueur
     */
    public static ItemBuilder skull(String playerName) {
        return new ItemBuilder(Material.PLAYER_HEAD);
        // Note: Pour définir le skin, utiliser SkullMeta.setOwningPlayer()
    }
    
    /**
     * Définit le propriétaire du skull
     */
    public ItemBuilder skullOwner(String playerName) {
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        }
        return this;
    }

    /**
     * Clone un ItemBuilder depuis un item existant
     */
    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item);
    }
    
    /**
     * Configure une potion
     */
    public ItemBuilder potion(org.bukkit.potion.PotionType potionType) {
        if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
            potionMeta.setBasePotionType(potionType);
        }
        return this;
    }
    
    /**
     * Configure une potion avec effet customisé
     */
    public ItemBuilder potion(org.bukkit.potion.PotionEffect effect) {
        if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
            potionMeta.addCustomEffect(effect, true);
        }
        return this;
    }
    
    /**
     * Définit la couleur de potion
     */
    public ItemBuilder potionColor(Color color) {
        if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
            potionMeta.setColor(color);
        }
        return this;
    }
}
