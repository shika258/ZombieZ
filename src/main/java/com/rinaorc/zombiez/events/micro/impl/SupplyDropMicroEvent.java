package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Colis Express - Version mini du largage de ravitaillement
 *
 * Mecanique:
 * - Un EnderChest tombe du ciel (via ArmorStand)
 * - Le joueur peut claim directement le colis en cliquant dessus
 * - 2-4 recompenses (moins bien que le largage normal)
 * - Pas de defense requise - c'est rapide et accessible
 */
public class SupplyDropMicroEvent extends MicroEvent implements Listener {

    // Phases de l'evenement
    private enum Phase {
        FALLING,    // Le colis tombe
        READY,      // Le colis est au sol, pret a etre claim
        CLAIMED,    // Le colis a ete claim
        DONE        // Termine
    }

    @Getter
    private Phase phase = Phase.FALLING;

    // Entites
    private ArmorStand fallingCrate;
    private TextDisplay crateMarker;
    private Location landingLocation;
    private Block enderChestBlock;

    // Taches planifiees
    private BukkitTask fallingTask;

    // Loot
    private final List<ItemStack> lootItems = new ArrayList<>();
    private boolean lootGenerated = false;

    // Configuration
    private static final int MIN_LOOT = 2;
    private static final int MAX_LOOT = 4;
    private static final double FALL_SPEED = 0.5; // Blocs par tick

    private final Random random = new Random();

    public SupplyDropMicroEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.SUPPLY_DROP, player, location, zone);
    }

    @Override
    protected void onStart() {
        // Enregistrer le listener pour detecter les clics
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Generer le loot a l'avance
        generateLoot();

        // Demarrer la chute du colis
        spawnFallingCrate();
    }

    /**
     * Fait tomber le colis depuis le ciel
     */
    private void spawnFallingCrate() {
        World world = location.getWorld();
        if (world == null) return;

        // Position de depart en hauteur
        Location spawnLoc = location.clone();
        spawnLoc.setY(Math.min(world.getMaxHeight() - 10, location.getY() + 50));

        // Definir la position d'atterrissage
        landingLocation = location.clone();
        landingLocation.setY(world.getHighestBlockYAt(location) + 1);

        // Creer un ArmorStand invisible avec un EnderChest sur la tete
        fallingCrate = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        fallingCrate.setVisible(false);
        fallingCrate.setGravity(false); // On gere la gravite manuellement
        fallingCrate.setMarker(true);
        fallingCrate.setSmall(false);
        fallingCrate.getEquipment().setHelmet(new ItemStack(Material.ENDER_CHEST));
        fallingCrate.setCustomName("§b§l📦 COLIS EXPRESS");
        fallingCrate.setCustomNameVisible(true);
        fallingCrate.setGlowing(true);
        fallingCrate.addScoreboardTag("micro_event_entity");
        fallingCrate.addScoreboardTag("supply_drop_crate");
        fallingCrate.addScoreboardTag("event_" + id);
        fallingCrate.setPersistent(false);
        registerEntity(fallingCrate);

        // Creer le TextDisplay pour le marqueur
        crateMarker = world.spawn(spawnLoc.clone().add(0, 2.5, 0), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(120, 0, 50, 80));

            Component text = Component.text("📦 ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("EN CHUTE...", NamedTextColor.AQUA, TextDecoration.BOLD));
            display.text(text);

            float scale = 1.5f;
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.addScoreboardTag("micro_event_entity");
            display.addScoreboardTag("supply_drop_display");
            display.addScoreboardTag("event_" + id);
            display.setPersistent(false);
        });
        registerEntity(crateMarker);

        // Effet de lancement
        world.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.2f);

        // Tache de chute
        fallingTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (fallingCrate == null || !fallingCrate.isValid() || phase != Phase.FALLING) {
                if (fallingTask != null) fallingTask.cancel();
                return;
            }

            Location crateLoc = fallingCrate.getLocation();

            // Verifier si atterri
            if (crateLoc.getY() <= landingLocation.getY() + 0.5) {
                onCrateLanded();
                fallingTask.cancel();
                return;
            }

            // Descendre le crate
            crateLoc.setY(crateLoc.getY() - FALL_SPEED);
            fallingCrate.teleport(crateLoc);

            // Mettre a jour le TextDisplay
            if (crateMarker != null && crateMarker.isValid()) {
                crateMarker.teleport(crateLoc.clone().add(0, 2.5, 0));
            }

            // Particules de chute
            World w = crateLoc.getWorld();
            if (w != null) {
                w.spawnParticle(Particle.CLOUD, crateLoc, 3, 0.2, 0.2, 0.2, 0.01);
                w.spawnParticle(Particle.END_ROD, crateLoc.clone().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }, 1L, 1L);
    }

    /**
     * Appele quand le colis atterrit
     */
    private void onCrateLanded() {
        if (phase != Phase.FALLING) return;
        phase = Phase.READY;

        World world = landingLocation.getWorld();
        if (world == null) return;

        // Supprimer l'ArmorStand
        if (fallingCrate != null && fallingCrate.isValid()) {
            fallingCrate.remove();
            fallingCrate = null;
        }

        // Placer un EnderChest au sol
        Block block = landingLocation.getBlock();
        // Nettoyer si deja un bloc
        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR);
        }
        block.setType(Material.ENDER_CHEST);
        enderChestBlock = block;

        // Mettre a jour le TextDisplay
        if (crateMarker != null && crateMarker.isValid()) {
            crateMarker.teleport(landingLocation.clone().add(0.5, 2.0, 0.5));
            Component text = Component.text("📦 ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("COLIS PRET!", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("Clic droit pour claim", NamedTextColor.GRAY));
            crateMarker.text(text);
        }

        // Effets d'atterrissage
        world.playSound(landingLocation, Sound.BLOCK_ANVIL_LAND, 1f, 1.5f);
        world.playSound(landingLocation, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1.2f);
        world.spawnParticle(Particle.CLOUD, landingLocation.clone().add(0.5, 0.5, 0.5), 20, 0.5, 0.3, 0.5, 0.05);
        world.spawnParticle(Particle.DUST, landingLocation.clone().add(0.5, 1, 0.5), 30, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(Color.AQUA, 1.5f));

        // Annoncer aux joueurs proches
        for (Player p : world.getNearbyEntities(landingLocation, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            p.sendMessage("§b§l📦 §7Un colis express a atterri! §eClic droit §7pour le recuperer!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
        }
    }

    /**
     * Detecte le clic sur l'EnderChest pour claim le colis
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (phase != Phase.READY) return;
        if (event.getClickedBlock() == null) return;
        if (enderChestBlock == null) return;

        // Verifier que c'est notre EnderChest
        if (!event.getClickedBlock().equals(enderChestBlock)) return;

        // Annuler l'ouverture normale de l'EnderChest
        event.setCancelled(true);

        // Claim le colis
        claimCrate(event.getPlayer());
    }

    /**
     * Claim le colis et distribue le loot
     */
    private void claimCrate(Player claimer) {
        if (phase != Phase.READY) return;
        phase = Phase.CLAIMED;

        World world = landingLocation.getWorld();
        if (world == null) return;

        // Donner le loot au joueur
        boolean inventoryFull = false;
        for (ItemStack item : lootItems) {
            if (claimer.getInventory().firstEmpty() == -1) {
                // Inventaire plein - drop au sol
                world.dropItemNaturally(claimer.getLocation(), item);
                inventoryFull = true;
            } else {
                claimer.getInventory().addItem(item);
            }
        }

        // Effets de claim
        world.playSound(landingLocation, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        world.playSound(landingLocation, Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, 1f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, landingLocation.clone().add(0.5, 1, 0.5), 30, 0.5, 0.8, 0.5, 0.1);
        world.spawnParticle(Particle.HAPPY_VILLAGER, claimer.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);

        // Message
        claimer.sendTitle("§a§l✓ COLIS RECUPERE!", "§7+" + lootItems.size() + " objets", 5, 30, 10);
        claimer.sendMessage("");
        claimer.sendMessage("§b§l📦 COLIS EXPRESS RECUPERE!");
        claimer.sendMessage("§7Vous avez obtenu §e" + lootItems.size() + " §7objets!");
        if (inventoryFull) {
            claimer.sendMessage("§c⚠ Inventaire plein - certains objets sont tombes au sol!");
        }
        claimer.sendMessage("");

        // Mettre a jour le TextDisplay
        if (crateMarker != null && crateMarker.isValid()) {
            Component text = Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("RECUPERE!", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("par " + claimer.getName(), NamedTextColor.GRAY));
            crateMarker.text(text);
        }

        // Supprimer l'EnderChest
        if (enderChestBlock != null && enderChestBlock.getType() == Material.ENDER_CHEST) {
            enderChestBlock.setType(Material.AIR);
        }

        // Ajouter le participant
        addParticipant(claimer);

        // Completer l'evenement
        complete();
    }

    /**
     * Ajoute un participant (pour les recompenses)
     */
    private void addParticipant(Player p) {
        // Le joueur qui claim devient le "joueur principal" pour les rewards
        // (Les rewards sont automatiquement distribues via complete())
    }

    @Override
    protected void tick() {
        // Verifier le timeout
        if (phase == Phase.READY) {
            // Mettre a jour l'ActionBar
            sendActionBar("§b📦 Colis Express §7| Temps: §e" + getRemainingTimeSeconds() + "s §7| §eClic droit pour claim!");

            // Particules periodiques pour attirer l'attention
            if (elapsedTicks % 20 == 0 && enderChestBlock != null) {
                World world = enderChestBlock.getWorld();
                Location loc = enderChestBlock.getLocation().add(0.5, 1, 0.5);
                world.spawnParticle(Particle.END_ROD, loc, 3, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Desenregistrer le listener
        HandlerList.unregisterAll(this);

        // Annuler la tache de chute
        if (fallingTask != null && !fallingTask.isCancelled()) {
            fallingTask.cancel();
            fallingTask = null;
        }

        // Supprimer le TextDisplay
        if (crateMarker != null && crateMarker.isValid()) {
            crateMarker.remove();
            crateMarker = null;
        }

        // Supprimer l'ArmorStand
        if (fallingCrate != null && fallingCrate.isValid()) {
            fallingCrate.remove();
            fallingCrate = null;
        }

        // Supprimer l'EnderChest
        if (enderChestBlock != null && enderChestBlock.getType() == Material.ENDER_CHEST) {
            enderChestBlock.setType(Material.AIR);
            enderChestBlock = null;
        }
    }

    @Override
    protected int getBonusPoints() {
        // Bonus si le joueur a claim rapidement (moins de temps utilise = plus de bonus)
        int timeUsed = elapsedTicks / 20;
        int maxTime = maxDuration / 20;
        int timeBonus = Math.max(0, (maxTime - timeUsed) * 3);
        return Math.min(timeBonus, 100);
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        // Pas de degats a gerer pour cet evenement
        return false;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        // Pas de mort a gerer pour cet evenement
        return false;
    }

    /**
     * Genere le loot du colis (2-4 items, moins rares que le largage normal)
     */
    private void generateLoot() {
        if (lootGenerated) return;
        lootGenerated = true;

        // Nombre d'items: 2-4
        int itemCount = MIN_LOOT + random.nextInt(MAX_LOOT - MIN_LOOT + 1);

        for (int i = 0; i < itemCount; i++) {
            // Alterner entre items et consommables
            if (random.nextBoolean() && plugin.getConsumableManager() != null) {
                // Generer un consommable
                Consumable consumable = plugin.getConsumableManager().generateConsumable(zone.getId(), 0.0);
                if (consumable != null) {
                    lootItems.add(consumable.createItemStack());
                    continue;
                }
            }

            // Generer un item avec rarete moderee
            Rarity rarity = rollLootRarity();
            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item != null) {
                lootItems.add(item);
            }
        }

        // S'assurer qu'on a au moins MIN_LOOT items
        while (lootItems.size() < MIN_LOOT) {
            lootItems.add(new ItemStack(Material.IRON_INGOT, 3 + random.nextInt(5)));
        }
    }

    /**
     * Roll la rarete du loot (moins bonne que le largage normal)
     */
    private Rarity rollLootRarity() {
        double roll = random.nextDouble() * 100;
        double zoneBonus = zone.getId() * 0.3; // Bonus plus faible que le largage normal

        if (roll < 2 + zoneBonus) return Rarity.EPIC;        // 2-5% Epic max
        if (roll < 15 + zoneBonus) return Rarity.RARE;       // 15-25% Rare
        if (roll < 45 + zoneBonus) return Rarity.UNCOMMON;   // 45-55% Uncommon
        return Rarity.COMMON;                                 // 55%+ Common
    }
}
