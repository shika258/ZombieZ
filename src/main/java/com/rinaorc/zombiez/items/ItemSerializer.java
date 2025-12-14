package com.rinaorc.zombiez.items;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.rinaorc.zombiez.items.affixes.Affix;
import com.rinaorc.zombiez.items.affixes.AffixRegistry;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Material;

import java.util.*;

/**
 * Sérialise et désérialise les ZombieZItem pour le stockage
 * Format: JSON compact pour la base de données
 */
public class ItemSerializer {

    private static final Gson GSON = new GsonBuilder().create();
    private static final AffixRegistry affixRegistry = AffixRegistry.getInstance();

    /**
     * Sérialise un ZombieZItem en JSON
     */
    public static String serialize(ZombieZItem item) {
        JsonObject json = new JsonObject();
        
        // Identifiants
        json.addProperty("uuid", item.getUuid().toString());
        json.addProperty("type", item.getItemType().name());
        json.addProperty("material", item.getMaterial().name());
        json.addProperty("rarity", item.getRarity().name());
        json.addProperty("tier", item.getTier());
        json.addProperty("zone", item.getZoneLevel());
        
        // Noms
        json.addProperty("baseName", item.getBaseName());
        json.addProperty("genName", item.getGeneratedName());
        
        // Stats de base
        JsonObject baseStats = new JsonObject();
        for (var entry : item.getBaseStats().entrySet()) {
            baseStats.addProperty(entry.getKey().name(), entry.getValue());
        }
        json.add("baseStats", baseStats);
        
        // Affixes
        JsonArray affixesArray = new JsonArray();
        for (ZombieZItem.RolledAffix ra : item.getAffixes()) {
            JsonObject affixJson = new JsonObject();
            affixJson.addProperty("id", ra.getAffix().getId());
            affixJson.addProperty("tier", ra.getAffix().getTier().ordinal());
            
            JsonObject rolledStats = new JsonObject();
            for (var entry : ra.getRolledStats().entrySet()) {
                rolledStats.addProperty(entry.getKey().name(), entry.getValue());
            }
            affixJson.add("stats", rolledStats);
            
            affixesArray.add(affixJson);
        }
        json.add("affixes", affixesArray);
        
        // Métadonnées
        json.addProperty("score", item.getItemScore());
        json.addProperty("created", item.getCreatedAt());
        json.addProperty("identified", item.isIdentified());
        
        if (item.getSetId() != null) {
            json.addProperty("setId", item.getSetId());
        }
        
        return GSON.toJson(json);
    }

    /**
     * Désérialise un JSON en ZombieZItem
     */
    public static ZombieZItem deserialize(String jsonStr) {
        try {
            JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
            
            // Identifiants
            UUID uuid = UUID.fromString(json.get("uuid").getAsString());
            ItemType itemType = ItemType.valueOf(json.get("type").getAsString());
            Material material = Material.valueOf(json.get("material").getAsString());
            Rarity rarity = Rarity.valueOf(json.get("rarity").getAsString());
            int tier = json.get("tier").getAsInt();
            int zone = json.get("zone").getAsInt();
            
            // Noms
            String baseName = json.get("baseName").getAsString();
            String genName = json.get("genName").getAsString();
            
            // Stats de base
            Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
            JsonObject baseStatsJson = json.getAsJsonObject("baseStats");
            for (String key : baseStatsJson.keySet()) {
                baseStats.put(StatType.valueOf(key), baseStatsJson.get(key).getAsDouble());
            }
            
            // Affixes
            List<ZombieZItem.RolledAffix> affixes = new ArrayList<>();
            JsonArray affixesArray = json.getAsJsonArray("affixes");
            for (int i = 0; i < affixesArray.size(); i++) {
                JsonObject affixJson = affixesArray.get(i).getAsJsonObject();
                String affixId = affixJson.get("id").getAsString();
                
                Affix affix = affixRegistry.getAffix(affixId);
                if (affix == null) continue; // Skip si l'affix n'existe plus
                
                Map<StatType, Double> rolledStats = new EnumMap<>(StatType.class);
                JsonObject statsJson = affixJson.getAsJsonObject("stats");
                for (String key : statsJson.keySet()) {
                    rolledStats.put(StatType.valueOf(key), statsJson.get(key).getAsDouble());
                }
                
                affixes.add(ZombieZItem.RolledAffix.builder()
                    .affix(affix)
                    .rolledStats(rolledStats)
                    .build());
            }
            
            // Métadonnées
            int score = json.get("score").getAsInt();
            long created = json.get("created").getAsLong();
            boolean identified = json.has("identified") && json.get("identified").getAsBoolean();
            String setId = json.has("setId") ? json.get("setId").getAsString() : null;
            
            // Construire l'item
            ZombieZItem item = ZombieZItem.builder()
                .uuid(uuid)
                .itemType(itemType)
                .material(material)
                .rarity(rarity)
                .tier(tier)
                .zoneLevel(zone)
                .baseName(baseName)
                .generatedName(genName)
                .baseStats(baseStats)
                .affixes(affixes)
                .itemScore(score)
                .createdAt(created)
                .identified(identified)
                .build();
            
            item.setSetId(setId);
            
            return item;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sérialise une liste d'items (pour l'inventaire complet)
     */
    public static String serializeList(List<ZombieZItem> items) {
        JsonArray array = new JsonArray();
        for (ZombieZItem item : items) {
            array.add(GSON.fromJson(serialize(item), JsonObject.class));
        }
        return GSON.toJson(array);
    }

    /**
     * Désérialise une liste d'items
     */
    public static List<ZombieZItem> deserializeList(String jsonStr) {
        List<ZombieZItem> items = new ArrayList<>();
        try {
            JsonArray array = GSON.fromJson(jsonStr, JsonArray.class);
            for (int i = 0; i < array.size(); i++) {
                ZombieZItem item = deserialize(array.get(i).toString());
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Crée un hash court de l'item pour vérification d'intégrité
     */
    public static String createHash(ZombieZItem item) {
        String data = item.getUuid().toString() + 
                     item.getRarity().name() + 
                     item.getItemScore() + 
                     item.getCreatedAt();
        return Integer.toHexString(data.hashCode());
    }

    /**
     * Vérifie l'intégrité d'un item
     */
    public static boolean verifyHash(ZombieZItem item, String expectedHash) {
        return createHash(item).equals(expectedHash);
    }
}
