package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zones.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Jackpot Zombie - Zombie slot machine
 *
 * Mecanique:
 * - Un zombie avec 3 rouleaux de symboles au-dessus
 * - Chaque COUP stoppe un rouleau
 * - 3 symboles identiques = JACKPOT!
 * - Differentes combinaisons donnent differentes recompenses
 */
public class JackpotZombieEvent extends MicroEvent {

    private Zombie jackpotZombie;
    private UUID jackpotUUID;
    private final List<TextDisplay> slotDisplays = new ArrayList<>();

    // Symboles des rouleaux
    private static final String[] SYMBOLS = {"🍒", "💎", "⭐", "🔥", "💰", "👑"};
    private static final String[] SYMBOL_COLORS = {"§c", "§b", "§e", "§6", "§a", "§d"};

    private final String[] reels = new String[3];
    private final boolean[] reelsStopped = {false, false, false};
    private int reelsStoppedCount = 0;
    private int spinTick = 0;
    private boolean resultCalculated = false;
    private int jackpotLevel = 0; // 0=rien, 1=2 identiques, 2=3 identiques special, 3=MEGA JACKPOT

    private final Random random = new Random();

    // Configuration
    private static final double JACKPOT_HEALTH = 100.0;
    private static final int SPIN_SPEED = 3; // Ticks entre chaque changement de symbole

    public JackpotZombieEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.JACKPOT_ZOMBIE, player, location, zone);

        // Initialiser les rouleaux (ils tournent au depart)
        for (int i = 0; i < 3; i++) {
            reels[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
    }

    @Override
    protected void onStart() {
        // Spawn le zombie jackpot
        jackpotZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        jackpotUUID = jackpotZombie.getUniqueId();
        registerEntity(jackpotZombie);

        // Configuration
        jackpotZombie.setCustomName("§6§l🎰 JACKPOT ZOMBIE 🎰");
        jackpotZombie.setCustomNameVisible(true);
        jackpotZombie.setBaby(false);
        jackpotZombie.setShouldBurnInDay(false);
        jackpotZombie.setAI(false);

        // Stats
        jackpotZombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(JACKPOT_HEALTH);
        jackpotZombie.setHealth(JACKPOT_HEALTH);
        jackpotZombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);

        // Equipement violet/dore (casino style)
        Color casinoColor = Color.fromRGB(148, 0, 211); // Violet
        jackpotZombie.getEquipment().setHelmet(createColoredArmor(Material.LEATHER_HELMET, casinoColor));
        jackpotZombie.getEquipment().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, Color.YELLOW));
        jackpotZombie.getEquipment().setLeggings(createColoredArmor(Material.LEATHER_LEGGINGS, casinoColor));
        jackpotZombie.getEquipment().setBoots(createColoredArmor(Material.LEATHER_BOOTS, Color.YELLOW));
        jackpotZombie.getEquipment().setHelmetDropChance(0f);
        jackpotZombie.getEquipment().setChestplateDropChance(0f);
        jackpotZombie.getEquipment().setLeggingsDropChance(0f);
        jackpotZombie.getEquipment().setBootsDropChance(0f);

        jackpotZombie.setGlowing(true);

        // Tags
        jackpotZombie.addScoreboardTag("micro_event_entity");
        jackpotZombie.addScoreboardTag("jackpot_zombie");
        jackpotZombie.addScoreboardTag("event_" + id);

        // Creer les affichages des rouleaux (armor stands invisibles avec noms)
        createSlotDisplays();

        // Effet de spawn
        location.getWorld().spawnParticle(Particle.FIREWORK, location.clone().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);

        // Message d'instructions
        player.sendMessage("§6§l🎰 JACKPOT ZOMBIE!");
        player.sendMessage("§7Frappez pour §eSTOPPER §7les rouleaux!");
        player.sendMessage("§73 identiques = §6§lJACKPOT!");
    }

    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cree les TextDisplays pour afficher les rouleaux
     */
    private void createSlotDisplays() {
        Location baseLoc = location.clone().add(0, 2.5, 0);

        for (int i = 0; i < 3; i++) {
            double offset = (i - 1) * 0.8; // -0.8, 0, 0.8
            Location displayLoc = baseLoc.clone().add(offset, 0, 0);

            final int reelIndex = i;
            TextDisplay display = location.getWorld().spawn(displayLoc, TextDisplay.class, d -> {
                d.setBillboard(Display.Billboard.CENTER);
                d.setSeeThrough(false);
                d.setShadowed(true);
                d.setDefaultBackground(false);
                d.setBackgroundColor(Color.fromARGB(180, 50, 0, 80)); // Fond violet casino

                d.text(getSlotDisplayComponent(reelIndex));

                // Taille des symboles (plus gros que les holograms classiques)
                float scale = 1.8f;
                d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));
            });

            slotDisplays.add(display);
            registerEntity(display);
        }
    }

    /**
     * Obtient l'affichage d'un rouleau (Component pour TextDisplay)
     */
    private Component getSlotDisplayComponent(int index) {
        if (reelsStopped[index]) {
            int symbolIndex = getSymbolIndex(reels[index]);
            NamedTextColor color = getSymbolNamedColor(symbolIndex);
            return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(reels[index], color, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.GRAY));
        } else {
            // Rouleau qui tourne
            return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(reels[index], NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.GRAY));
        }
    }

    /**
     * Obtient la couleur NamedTextColor pour un symbole
     */
    private NamedTextColor getSymbolNamedColor(int symbolIndex) {
        return switch (symbolIndex) {
            case 0 -> NamedTextColor.RED;           // 🍒
            case 1 -> NamedTextColor.AQUA;          // 💎
            case 2 -> NamedTextColor.YELLOW;        // ⭐
            case 3 -> NamedTextColor.GOLD;          // 🔥
            case 4 -> NamedTextColor.GREEN;         // 💰
            case 5 -> NamedTextColor.LIGHT_PURPLE;  // 👑
            default -> NamedTextColor.WHITE;
        };
    }

    /**
     * Trouve l'index d'un symbole
     */
    private int getSymbolIndex(String symbol) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            if (SYMBOLS[i].equals(symbol)) return i;
        }
        return 0;
    }

    @Override
    protected void tick() {
        if (jackpotZombie == null || jackpotZombie.isDead()) {
            if (resultCalculated) {
                complete();
            } else {
                fail();
            }
            return;
        }

        // Faire tourner les rouleaux non stoppes
        spinTick++;
        if (spinTick >= SPIN_SPEED) {
            spinTick = 0;

            for (int i = 0; i < 3; i++) {
                if (!reelsStopped[i]) {
                    // Changer le symbole
                    reels[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];

                    // Son de rotation
                    if (i == 1) { // Son seulement pour le rouleau central
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f);
                    }
                }
            }

            // Mettre a jour les affichages
            updateSlotDisplays();
        }

        // Particules
        if (elapsedTicks % 5 == 0) {
            Location loc = jackpotZombie.getLocation().add(0, 2.5, 0);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 3,
                0.5, 0.3, 0.5, 0,
                new Particle.DustOptions(Color.YELLOW, 0.8f));
        }

        // ActionBar
        String status = reelsStoppedCount == 0 ? "§eROULEAUX EN COURS..." :
                       (reelsStoppedCount == 1 ? "§a1/3 stoppe" :
                       (reelsStoppedCount == 2 ? "§a2/3 stoppes" : "§a§lREVELATION!"));

        sendActionBar("§6🎰 Jackpot Zombie §7| " + status + " §7| §e" + getRemainingTimeSeconds() + "s");
    }

    /**
     * Met a jour les affichages des rouleaux
     */
    private void updateSlotDisplays() {
        for (int i = 0; i < slotDisplays.size() && i < 3; i++) {
            TextDisplay display = slotDisplays.get(i);
            if (display.isValid()) {
                display.text(getSlotDisplayComponent(i));
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Les TextDisplays sont supprimes via registerEntity
    }

    @Override
    protected void distributeRewards() {
        // Calculer le resultat
        calculateResult();

        // Recompenses basees sur le jackpot
        int points;
        String message;
        Sound sound;
        int particleCount;

        switch (jackpotLevel) {
            case 3 -> { // MEGA JACKPOT (👑👑👑)
                points = 1500;
                message = "§6§l🎰 MEGA JACKPOT!!! 👑👑👑 §6+1500⚡";
                sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particleCount = 60;

                // Annonce serveur
                plugin.getServer().broadcast(
                    net.kyori.adventure.text.Component.text(
                        "§6§l🎰 " + player.getName() + " a touche le §e§lMEGA JACKPOT §6§l(👑👑👑) §6et gagne §e1500⚡§6!"
                    )
                );

                // Drop un item legendaire
                try {
                    ItemStack legendary = plugin.getItemManager().generateItem(zone.getId(), Rarity.LEGENDARY);
                    player.getWorld().dropItemNaturally(player.getLocation(), legendary);
                    player.sendMessage("§6§l+ ITEM LEGENDAIRE!");
                } catch (Exception ignored) {}
            }
            case 2 -> { // JACKPOT (3 identiques)
                points = 800;
                message = "§e§l🎰 JACKPOT! " + reels[0] + reels[1] + reels[2] + " §e+800⚡";
                sound = Sound.ENTITY_PLAYER_LEVELUP;
                particleCount = 40;

                // Drop un item epic
                try {
                    ItemStack epic = plugin.getItemManager().generateItem(zone.getId(), Rarity.EPIC);
                    player.getWorld().dropItemNaturally(player.getLocation(), epic);
                    player.sendMessage("§d+ ITEM EPIQUE!");
                } catch (Exception ignored) {}
            }
            case 1 -> { // 2 identiques
                points = 300;
                message = "§a🎰 Deux identiques! " + reels[0] + reels[1] + reels[2] + " §a+300⚡";
                sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
                particleCount = 30;
            }
            default -> { // Rien
                points = 75;
                message = "§7🎰 Pas de chance... " + reels[0] + reels[1] + reels[2] + " §7+75⚡";
                sound = Sound.BLOCK_NOTE_BLOCK_BASS;
                particleCount = 10;
            }
        }

        // Multiplier par zone
        points = (int) (points * (1.0 + zone.getId() * 0.03));
        int xp = (int) (type.getBaseXpReward() * (1.0 + zone.getId() * 0.05));

        // Distribuer
        plugin.getEconomyManager().addPoints(player, points);
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null) {
            playerData.addXp(xp);
        }

        // Effets
        player.sendTitle(
            jackpotLevel >= 2 ? "§6§l🎰 JACKPOT! 🎰" : "§7🎰",
            reels[0] + " " + reels[1] + " " + reels[2],
            10, 60, 20
        );

        player.sendMessage("");
        player.sendMessage(message);
        player.sendMessage("§7XP: §b+" + xp);
        player.sendMessage("");

        player.playSound(player.getLocation(), sound, 1f, 1f);

        // Particules
        Location loc = player.getLocation().add(0, 1, 0);
        if (jackpotLevel >= 2) {
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, particleCount, 0.5, 1, 0.5, 0.3);
        } else {
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, particleCount, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * Calcule le resultat des rouleaux
     */
    private void calculateResult() {
        resultCalculated = true;

        // Verifier 3 identiques
        if (reels[0].equals(reels[1]) && reels[1].equals(reels[2])) {
            if (reels[0].equals("👑")) {
                jackpotLevel = 3; // MEGA JACKPOT
            } else {
                jackpotLevel = 2; // JACKPOT normal
            }
            return;
        }

        // Verifier 2 identiques
        if (reels[0].equals(reels[1]) || reels[1].equals(reels[2]) || reels[0].equals(reels[2])) {
            jackpotLevel = 1;
            return;
        }

        jackpotLevel = 0;
    }

    @Override
    protected int getBonusPoints() {
        return 0; // Gere dans distributeRewards
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (!entity.getUniqueId().equals(jackpotUUID)) return false;

        // Stopper le prochain rouleau
        if (reelsStoppedCount < 3) {
            int reelToStop = reelsStoppedCount;
            reelsStopped[reelToStop] = true;
            reelsStoppedCount++;

            // Fixer le symbole final (avec une petite chance de manipulation pour le fun)
            // Petite chance d'avoir un symbole identique au precedent si on en a deja un
            if (reelToStop > 0 && random.nextDouble() < 0.25) {
                reels[reelToStop] = reels[reelToStop - 1];
            } else {
                reels[reelToStop] = SYMBOLS[random.nextInt(SYMBOLS.length)];
            }

            updateSlotDisplays();

            // Effet de stop
            int symbolIndex = getSymbolIndex(reels[reelToStop]);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.8f + (reelToStop * 0.3f));

            Location zombieLoc = jackpotZombie.getLocation().add(0, 2.5, 0);
            zombieLoc.getWorld().spawnParticle(Particle.FLASH, zombieLoc, 1);

            // Messages
            player.sendMessage("§6🎰 §7Rouleau " + (reelToStop + 1) + ": " +
                SYMBOL_COLORS[symbolIndex] + "§l" + reels[reelToStop]);

            // Si tous les rouleaux sont stoppes, tuer le zombie pour declencher la fin
            if (reelsStoppedCount >= 3) {
                // Petit delai pour le suspense
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (jackpotZombie != null && !jackpotZombie.isDead()) {
                        // Effet de mort
                        Location loc = jackpotZombie.getLocation();
                        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
                        jackpotZombie.setHealth(0);
                    }
                }, 20L); // 1 seconde de suspense
            }
        }

        // Annuler les degats normaux
        return true;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (entity.getUniqueId().equals(jackpotUUID)) {
            return true;
        }
        return false;
    }
}
