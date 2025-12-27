package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetRarity;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.eggs.EggType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Animation d'ouverture d'oeuf ultra satisfaisante
 * Style slot machine avec suspense et célébration
 */
public class EggOpeningAnimation implements InventoryHolder {

    private static final String TITLE = "§8§l✨ Ouverture en cours...";
    private static final int SIZE = 27;

    // Slots pour l'animation de roulette
    private static final int[] ROULETTE_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int CENTER_SLOT = 13;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final EggType eggType;
    private final int eggCount;
    private final List<PetType> results = new ArrayList<>();
    private boolean animationComplete = false;
    private boolean canClose = false;

    public EggOpeningAnimation(ZombieZPlugin plugin, Player player, EggType eggType, int count) {
        this.plugin = plugin;
        this.player = player;
        this.eggType = eggType;
        this.eggCount = Math.min(count, 10);
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        setupGUI();
        startAnimation();
    }

    private void setupGUI() {
        // Fond sombre mystérieux
        ItemStack darkGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, darkGlass);
        }

        // Indicateurs de sélection (flèches)
        inventory.setItem(4, new ItemBuilder(Material.ARROW)
            .name("§e§l▼ §f§lRÉSULTAT §e§l▼")
            .build());
        inventory.setItem(22, new ItemBuilder(Material.ARROW)
            .name("§e§l▲")
            .build());

        // Cadre doré autour du centre
        ItemStack gold = ItemBuilder.placeholder(Material.YELLOW_STAINED_GLASS_PANE);
        inventory.setItem(3, gold);
        inventory.setItem(5, gold);
        inventory.setItem(21, gold);
        inventory.setItem(23, gold);
    }

    private void startAnimation() {
        // Pré-calculer tous les résultats
        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        for (int i = 0; i < eggCount; i++) {
            PetRarity pityGuarantee = petData.checkPityGuarantee(eggType);
            PetType result = eggType.rollPet(0, pityGuarantee);
            results.add(result);
        }

        // Commencer l'animation
        if (eggCount == 1) {
            startSingleAnimation();
        } else {
            startMultiAnimation();
        }
    }

    private void startSingleAnimation() {
        PetType finalResult = results.get(0);

        new BukkitRunnable() {
            int tick = 0;
            int speed = 1; // Vitesse de défilement (plus bas = plus rapide)
            int slowdownStart = 30; // Ralentir plus tôt pour effet dramatique
            int totalTicks = 65; // Animation plus courte et punchy
            Random random = new Random();
            Material[] frameColors = {
                Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE
            };

            @Override
            public void run() {
                if (tick >= totalTicks) {
                    // Animation terminée - révéler le résultat!
                    revealResult(finalResult);
                    cancel();
                    return;
                }

                // Phase de spin ultra rapide au début
                if (tick <= slowdownStart) {
                    speed = 1;
                    // Effet visuel: cadre qui flashe rapidement
                    if (tick % 2 == 0) {
                        Material frameColor = frameColors[(tick / 2) % frameColors.length];
                        ItemStack frame = ItemBuilder.placeholder(frameColor);
                        for (int slot : new int[]{3, 5, 21, 23}) {
                            inventory.setItem(slot, frame);
                        }
                    }
                } else {
                    // Ralentissement dramatique et rapide
                    int progress = tick - slowdownStart;
                    if (progress > 25) speed = 6;
                    else if (progress > 18) speed = 4;
                    else if (progress > 10) speed = 3;
                    else if (progress > 5) speed = 2;
                    else speed = 1;
                }

                // Mettre à jour la roulette
                if (tick % speed == 0) {
                    updateRoulette(random, tick >= totalTicks - 8 ? finalResult : null);

                    // Sons de défilement - plus variés et satisfaisants
                    float pitch = 0.8f + (tick / (float) totalTicks) * 1.2f;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, pitch);

                    // Son additionnel pour le rythme
                    if (tick > slowdownStart) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, pitch + 0.5f);
                    }
                }

                // Particules plus fréquentes et dynamiques
                if (tick % 3 == 0) {
                    spawnSuspenseParticles();
                    // Particules de côté qui s'intensifient
                    if (tick > slowdownStart) {
                        Location loc = player.getLocation().add(0, 1.5, 0);
                        player.spawnParticle(Particle.ENCHANTED_HIT, loc, 8, 1, 0.5, 1, 0.1);
                    }
                }

                // Intensifier le suspense vers la fin avec des sons crescendo
                if (tick > slowdownStart) {
                    if (tick % 4 == 0) {
                        float bassPitch = 0.5f + ((tick - slowdownStart) / 35f) * 0.5f;
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, bassPitch);
                    }
                    // Dernier moment - son de tension
                    if (tick >= totalTicks - 10 && tick % 2 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.8f);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void updateRoulette(Random random, PetType forcedCenter) {
        // Faire défiler les pets dans la roulette
        PetType[] allPets = PetType.values();

        for (int i = 0; i < ROULETTE_SLOTS.length; i++) {
            int slot = ROULETTE_SLOTS[i];
            PetType pet;

            if (forcedCenter != null && slot == CENTER_SLOT) {
                pet = forcedCenter;
            } else {
                // Probabilités biaisées pour montrer des raretés variées (suspense!)
                if (random.nextDouble() < 0.1 && forcedCenter == null) {
                    // 10% chance de montrer une rareté élevée (tease)
                    PetRarity[] highRarities = {PetRarity.EPIC, PetRarity.LEGENDARY, PetRarity.MYTHIC};
                    PetRarity tease = highRarities[random.nextInt(highRarities.length)];
                    PetType[] teasePool = PetType.getByRarity(tease);
                    pet = teasePool.length > 0 ? teasePool[random.nextInt(teasePool.length)] : allPets[random.nextInt(allPets.length)];
                } else {
                    pet = allPets[random.nextInt(allPets.length)];
                }
            }

            inventory.setItem(slot, createPetIcon(pet, slot == CENTER_SLOT));
        }
    }

    private ItemStack createPetIcon(PetType pet, boolean isCenter) {
        String name = pet.getColoredName();
        if (isCenter) {
            name = "§l" + name;
        }

        List<String> lore = new ArrayList<>();
        lore.add(pet.getRarity().getStars());

        return new ItemBuilder(pet.getIcon())
            .name(name)
            .lore(lore)
            .glow(isCenter)
            .build();
    }

    private void revealResult(PetType pet) {
        animationComplete = true;

        // Effet flash blanc avant la révélation
        ItemStack whiteFlash = ItemBuilder.placeholder(Material.WHITE_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, whiteFlash);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);

        // Révéler après un court flash
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Fond sombre
            ItemStack darkGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
            for (int i = 0; i < SIZE; i++) {
                inventory.setItem(i, darkGlass);
            }

            // Afficher le résultat au centre avec style
            ItemStack resultItem = createRevealItem(pet);
            inventory.setItem(CENTER_SLOT, resultItem);

            // Cadre selon la rareté - plus large et visible
            Material frameMaterial = switch (pet.getRarity()) {
                case COMMON -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                case UNCOMMON -> Material.LIME_STAINED_GLASS_PANE;
                case RARE -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                case EPIC -> Material.MAGENTA_STAINED_GLASS_PANE;
                case LEGENDARY -> Material.ORANGE_STAINED_GLASS_PANE;
                case MYTHIC -> Material.RED_STAINED_GLASS_PANE;
            };
            ItemStack frame = ItemBuilder.placeholder(frameMaterial);
            for (int slot : new int[]{3, 4, 5, 12, 14, 21, 22, 23}) {
                inventory.setItem(slot, frame);
            }

            // Son de révélation punch
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

            // Appliquer le résultat avec le pet pré-calculé (évite un nouveau roll)
            boolean isNew = plugin.getPetManager().openEggWithResult(player, eggType, pet) != null;
            PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());

            // Effets selon la rareté
            playCelebration(pet, isNew);

            // Message
            String newTag = isNew ? " §a§l[NOUVEAU!]" : " §e[+1 copie]";
            player.sendMessage("");
            player.sendMessage("§6§l★ §e§lOEUF OUVERT! §6§l★");
            player.sendMessage("§7Vous avez obtenu: " + pet.getColoredName() + newTag);
            if (!isNew && petData != null && petData.hasPet(pet)) {
                int copies = petData.getPet(pet).getCopies();
                int fragments = pet.getRarity().getFragmentsPerDuplicate();
                player.sendMessage("§7Copies: §f" + copies + " §7| §7+§e" + fragments + " §7fragments");
            }
            player.sendMessage("");

            // Permettre de fermer rapidement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                canClose = true;
                inventory.setItem(26, new ItemBuilder(Material.BARRIER)
                    .name("§c§lFermer")
                    .lore(List.of("§7Cliquez ou appuyez sur Échap"))
                    .build());
            }, 20L);
        }, 3L);
    }

    private ItemStack createRevealItem(PetType pet) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(pet.getRarity().getStars() + " " + pet.getRarity().getColoredName());
        lore.add("");
        lore.add("§7Thème: §f" + pet.getTheme());
        lore.add("§7" + pet.getAppearance());
        lore.add("");
        lore.add("§e§lPassif: §f" + pet.getPassiveDescription());
        lore.add("");
        lore.add("§6§lUltime: §f" + pet.getUltimateName());
        lore.add("§7" + pet.getUltimateDescription());
        lore.add("§7(Auto: toutes les " + pet.getUltimateCooldown() + "s)");

        return new ItemBuilder(pet.getIcon())
            .name("§l" + pet.getColoredName())
            .lore(lore)
            .glow(true)
            .build();
    }

    private void playCelebration(PetType pet, boolean isNew) {
        Location loc = player.getLocation();

        switch (pet.getRarity()) {
            case COMMON -> {
                player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 20, 0.5, 0.3, 0.5, 0.02);
            }
            case UNCOMMON -> {
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            case RARE -> {
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.spawnParticle(Particle.ENCHANT, loc.add(0, 1.5, 0), 50, 0.8, 0.8, 0.8, 0.5);
                player.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0.3, 0.3, 0.05);
            }
            case EPIC -> {
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 2.0f);
                player.spawnParticle(Particle.WITCH, loc.add(0, 1.5, 0), 80, 1, 1, 1, 0.2);
                player.spawnParticle(Particle.END_ROD, loc, 40, 0.5, 0.5, 0.5, 0.1);
                epicCelebrationAnimation(loc);
            }
            case LEGENDARY -> {
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);
                player.spawnParticle(Particle.END_ROD, loc.add(0, 1.5, 0), 100, 1.5, 1.5, 1.5, 0.2);
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.5, 0.5, 0.5, 0.3);
                legendaryCelebrationAnimation(loc);
            }
            case MYTHIC -> {
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.5f);
                player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1.5, 0), 150, 2, 2, 2, 0.3);
                player.spawnParticle(Particle.END_ROD, loc, 100, 1, 1, 1, 0.2);
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 80, 1, 1, 1, 0.5);
                mythicCelebrationAnimation(loc);

                // Broadcast pour les mythiques!
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§c§l★★★ MYTHIQUE OBTENU! ★★★");
                Bukkit.broadcastMessage("§f" + player.getName() + " §7a obtenu §c§l" + pet.getDisplayName() + "§7!");
                Bukkit.broadcastMessage("");
            }
        }

        // Badge "NOUVEAU!" spécial
        if (isNew && pet.getRarity().isAtLeast(PetRarity.RARE)) {
            player.sendTitle(
                pet.getRarity().getColor() + "§l" + pet.getDisplayName(),
                "§a§lNOUVEAU PET DÉBLOQUÉ!",
                10, 60, 20
            );
        }
    }

    private void epicCelebrationAnimation(Location center) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 40) { cancel(); return; }

                double angle = tick * 0.3;
                for (int i = 0; i < 3; i++) {
                    double a = angle + i * (Math.PI * 2 / 3);
                    double x = Math.cos(a) * 1.5;
                    double z = Math.sin(a) * 1.5;
                    center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(x, 0.5, z), 3, 0.1, 0.1, 0.1, 0);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void legendaryCelebrationAnimation(Location center) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 60) { cancel(); return; }

                // Spirale montante
                double angle = tick * 0.2;
                double radius = 1 + (tick / 60.0) * 2;
                double y = tick / 30.0;

                for (int i = 0; i < 2; i++) {
                    double a = angle + i * Math.PI;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 2, 0.05, 0.05, 0.05, 0);
                    center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void mythicCelebrationAnimation(Location center) {
        // Explosion de particules en plusieurs vagues
        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave >= 3) { cancel(); return; }

                double radius = 2 + wave * 1.5;
                for (int i = 0; i < 36; i++) {
                    double angle = i * 10 * Math.PI / 180;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 1, z), 5, 0.1, 0.2, 0.1, 0.02);
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, 1.5, z), 2, 0.1, 0.1, 0.1, 0);
                }
                center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f + wave * 0.3f);
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 15L);

        // Colonnes de feu
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 40) { cancel(); return; }

                for (int i = 0; i < 4; i++) {
                    double angle = (tick * 0.15) + i * (Math.PI / 2);
                    double x = Math.cos(angle) * 2.5;
                    double z = Math.sin(angle) * 2.5;
                    for (double y = 0; y < 3; y += 0.3) {
                        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, y, z), 1, 0.05, 0.05, 0.05, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    private void spawnSuspenseParticles() {
        Location loc = player.getLocation().add(0, 1.5, 0);
        player.spawnParticle(Particle.END_ROD, loc, 5, 0.5, 0.5, 0.5, 0.02);
    }

    private void startMultiAnimation() {
        // Animation rapide de spin avant les révélations
        new BukkitRunnable() {
            int spinTick = 0;
            int spinDuration = 25; // Spin court au début
            Random random = new Random();
            Material[] flashColors = {
                Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE
            };

            @Override
            public void run() {
                if (spinTick >= spinDuration) {
                    // Lancer les révélations rapides
                    startMultiReveal();
                    cancel();
                    return;
                }

                // Spin rapide dans la roulette
                if (spinTick % 2 == 0) {
                    PetType[] allPets = PetType.values();
                    for (int slot : ROULETTE_SLOTS) {
                        PetType pet = allPets[random.nextInt(allPets.length)];
                        inventory.setItem(slot, createPetIcon(pet, false));
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f + spinTick * 0.03f);
                }

                // Flash du cadre
                Material frameColor = flashColors[spinTick % flashColors.length];
                ItemStack frame = ItemBuilder.placeholder(frameColor);
                for (int slot : new int[]{3, 5, 21, 23}) {
                    inventory.setItem(slot, frame);
                }

                spinTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startMultiReveal() {
        // Révéler les résultats un par un très rapidement
        new BukkitRunnable() {
            int index = 0;
            int tick = 0;
            int revealDelay = 8; // Beaucoup plus rapide!

            @Override
            public void run() {
                if (index >= results.size()) {
                    // Tous révélés - montrer le résumé
                    showMultiSummary();
                    cancel();
                    return;
                }

                if (tick % revealDelay == 0) {
                    PetType pet = results.get(index);
                    // Utiliser le pet pré-calculé (évite un nouveau roll)
                    boolean isNew = plugin.getPetManager().openEggWithResult(player, eggType, pet) != null;

                    // Son punch pour chaque révélation
                    float pitch = 1.0f + (float) pet.getRarity().ordinal() * 0.15f;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, pitch);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch);

                    // Son spécial pour les raretés hautes
                    if (pet.getRarity().isAtLeast(PetRarity.EPIC)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
                        // Flash court pour les épiques+
                        ItemStack flash = ItemBuilder.placeholder(switch (pet.getRarity()) {
                            case EPIC -> Material.MAGENTA_STAINED_GLASS_PANE;
                            case LEGENDARY -> Material.ORANGE_STAINED_GLASS_PANE;
                            case MYTHIC -> Material.RED_STAINED_GLASS_PANE;
                            default -> Material.WHITE_STAINED_GLASS_PANE;
                        });
                        for (int s : new int[]{3, 4, 5, 21, 22, 23}) {
                            inventory.setItem(s, flash);
                        }
                    }

                    // Afficher dans la roulette avec animation de remplissage
                    int displaySlot = ROULETTE_SLOTS[Math.min(index, ROULETTE_SLOTS.length - 1)];
                    inventory.setItem(displaySlot, createPetIcon(pet, index == results.size() - 1));

                    // Particules rapides
                    player.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.3, 0.5, 0.1);

                    index++;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void showMultiSummary() {
        animationComplete = true;

        // Compter les résultats
        Map<PetRarity, Integer> rarityCounts = new EnumMap<>(PetRarity.class);
        int newPets = 0;

        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        for (PetType pet : results) {
            rarityCounts.merge(pet.getRarity(), 1, Integer::sum);
        }

        // Meilleur résultat
        PetType best = results.stream()
            .max(Comparator.comparingInt(p -> p.getRarity().ordinal()))
            .orElse(results.get(0));

        // Afficher le résumé
        inventory.setItem(CENTER_SLOT, createRevealItem(best));

        // Message résumé
        player.sendMessage("");
        player.sendMessage("§6§l★ §e§l" + eggCount + " OEUFS OUVERTS! §6§l★");
        player.sendMessage("");
        for (PetRarity rarity : PetRarity.values()) {
            int count = rarityCounts.getOrDefault(rarity, 0);
            if (count > 0) {
                player.sendMessage("  " + rarity.getColoredName() + "§7: §fx" + count);
            }
        }
        player.sendMessage("");

        // Célébration pour le meilleur
        playCelebration(best, false);

        // Son de fin satisfaisant
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        // Permettre de fermer rapidement
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            canClose = true;
            inventory.setItem(26, new ItemBuilder(Material.BARRIER)
                .name("§c§lFermer")
                .lore(List.of("§7Cliquez ou appuyez sur Échap"))
                .build());
        }, 20L);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public boolean isAnimationComplete() {
        return animationComplete;
    }

    public boolean canClose() {
        return canClose;
    }

    /**
     * Listener pour l'animation
     */
    public static class AnimationListener implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof EggOpeningAnimation anim)) return;
            event.setCancelled(true);

            if (event.getRawSlot() == 26 && anim.canClose()) {
                event.getWhoClicked().closeInventory();
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof EggOpeningAnimation) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof EggOpeningAnimation anim)) return;

            // Empêcher de fermer pendant l'animation
            if (!anim.canClose()) {
                Bukkit.getScheduler().runTask(anim.plugin, () -> {
                    if (event.getPlayer() instanceof Player p && p.isOnline()) {
                        anim.open();
                    }
                });
            }
        }
    }
}
