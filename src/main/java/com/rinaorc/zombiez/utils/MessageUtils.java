package com.rinaorc.zombiez.utils;

import com.rinaorc.zombiez.ZombieZPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaires pour les messages et notifications
 * Support des codes couleur legacy et MiniMessage
 */
public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static Map<String, String> messages = new HashMap<>();
    private static String prefix = "§6§lZombieZ §8» §7";

    /**
     * Charge les messages depuis la configuration
     */
    public static void reload() {
        ZombieZPlugin plugin = ZombieZPlugin.getInstance();
        if (plugin == null) return;

        FileConfiguration config = plugin.getConfigManager().getMessagesConfig();
        if (config == null) return;

        messages.clear();
        prefix = config.getString("prefix", prefix);

        // Charger tous les messages
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    /**
     * Obtient un message depuis la configuration
     */
    public static String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Obtient un message formaté avec des placeholders
     */
    public static String getMessage(String key, Object... replacements) {
        String message = getMessage(key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(
                    String.valueOf(replacements[i]), 
                    String.valueOf(replacements[i + 1])
                );
            }
        }
        
        return message;
    }

    /**
     * Colorise un message (codes legacy §)
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Convertit un message en Component (Adventure API)
     */
    public static Component toComponent(String message) {
        return Component.text(colorize(message));
    }

    /**
     * Convertit un message MiniMessage en Component
     */
    public static Component parseMiniMessage(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Envoie un message à un joueur avec le préfixe
     */
    public static void send(Player player, String message) {
        player.sendMessage(colorize(prefix + message));
    }

    /**
     * Envoie un message à un joueur sans préfixe
     */
    public static void sendRaw(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    /**
     * Envoie un message formaté depuis la config
     */
    public static void sendMessage(Player player, String key, Object... replacements) {
        send(player, getMessage(key, replacements));
    }

    /**
     * Envoie une action bar à un joueur
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(toComponent(message));
    }

    /**
     * Envoie un titre à un joueur
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );
        
        Title titleObj = Title.title(
            toComponent(title),
            toComponent(subtitle),
            times
        );
        
        player.showTitle(titleObj);
    }

    /**
     * Envoie un titre simple
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 40, 10);
    }

    /**
     * Efface le titre d'un joueur
     */
    public static void clearTitle(Player player) {
        player.clearTitle();
    }

    /**
     * Broadcast un message à tous les joueurs
     */
    public static void broadcast(String message) {
        Bukkit.broadcast(toComponent(prefix + message));
    }

    /**
     * Broadcast un message sans préfixe
     */
    public static void broadcastRaw(String message) {
        Bukkit.broadcast(toComponent(message));
    }

    /**
     * Envoie un son à un joueur
     */
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Envoie un son de succès
     */
    public static void playSoundSuccess(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    /**
     * Envoie un son d'erreur
     */
    public static void playSoundError(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    /**
     * Envoie un son de notification
     */
    public static void playSoundNotify(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    /**
     * Envoie un son de level up
     */
    public static void playSoundLevelUp(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    /**
     * Formate un temps en secondes vers une chaîne lisible
     */
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    /**
     * Formate une durée en millisecondes
     */
    public static String formatDuration(long millis) {
        return formatTime(millis / 1000);
    }

    /**
     * Centre un message pour le chat (largeur 320 pixels)
     */
    public static String centerMessage(String message) {
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                int charWidth = getCharWidth(c, isBold);
                messagePxSize += charWidth;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = 4;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        
        return sb + message;
    }

    /**
     * Obtient la largeur d'un caractère
     */
    private static int getCharWidth(char c, boolean bold) {
        int width = switch (c) {
            case ' ' -> 4;
            case 'i', '!', ':', ';', '|', '.' -> 2;
            case 'l', '\'' -> 3;
            case 't', 'I', '[', ']', '"' -> 4;
            case 'f', 'k', '<', '>', '(', ')', '{', '}' -> 5;
            default -> 6;
        };
        return bold ? width + 1 : width;
    }

    /**
     * Crée une barre de progression
     */
    public static String progressBar(double progress, int length, String filledColor, String emptyColor) {
        int filled = (int) (progress / 100 * length);
        int empty = length - filled;
        
        return filledColor + "█".repeat(Math.max(0, filled)) + 
               emptyColor + "░".repeat(Math.max(0, empty));
    }

    /**
     * Crée une barre de progression avec pourcentage
     */
    public static String progressBarWithPercent(double progress, int length) {
        String bar = progressBar(progress, length, "§a", "§7");
        return bar + " §f" + String.format("%.1f", progress) + "%";
    }
    
    /**
     * Formate un nombre avec des suffixes K, M, B
     */
    public static String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1_000_000) return String.format("%.1fK", number / 1000.0);
        if (number < 1_000_000_000) return String.format("%.1fM", number / 1_000_000.0);
        return String.format("%.1fB", number / 1_000_000_000.0);
    }
    
    /**
     * Formate un nombre avec des suffixes (version int)
     */
    public static String formatNumber(int number) {
        return formatNumber((long) number);
    }
    
    /**
     * Formate un nombre avec séparateur de milliers
     */
    public static String formatWithCommas(long number) {
        return String.format("%,d", number);
    }
}
