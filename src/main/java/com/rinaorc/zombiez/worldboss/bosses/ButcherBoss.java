package com.rinaorc.zombiez.worldboss.bosses;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.WorldBoss;
import com.rinaorc.zombiez.worldboss.WorldBossType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Le Boucher (Tank - Taille x3)
 *
 * Capacité: Attire tous les joueurs proches vers lui toutes les 15s (Hook)
 * Stratégie: Plus il frappe, plus il gagne de la résistance. Il faut le kiter.
 *
 * Mécaniques:
 * - Hook: Attire tous les joueurs dans un rayon de 15 blocs vers lui
 * - Résistance croissante: Chaque attaque réussie augmente sa résistance
 * - Ralentissement: Les joueurs attirés sont ralentis
 */
public class ButcherBoss extends WorldBoss {

    // Compteur d'attaques pour la résistance croissante
    private int attackCount = 0;
    private int resistanceLevel = 0;
    private static final int ATTACKS_PER_RESISTANCE = 5;
    private static final int MAX_RESISTANCE_LEVEL = 4;

    // Hook
    private static final double HOOK_RADIUS = 15.0;
    private static final double HOOK_STRENGTH = 1.5;

    public ButcherBoss(ZombieZPlugin plugin, int zoneId) {
        super(plugin, WorldBossType.THE_BUTCHER, zoneId);
    }

    @Override
    protected void useAbility() {
        if (entity == null || !entity.isValid()) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Avertissement sonore
        world.playSound(bossLoc, Sound.ENTITY_RAVAGER_ROAR, 2f, 0.6f);
        world.playSound(bossLoc, Sound.BLOCK_CHAIN_BREAK, 2f, 0.5f);

        // Particules de préparation
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            Location particleLoc = bossLoc.clone().add(
                Math.cos(rad) * HOOK_RADIUS,
                0.5,
                Math.sin(rad) * HOOK_RADIUS
            );
            world.spawnParticle(Particle.DUST, particleLoc, 3, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.RED, 2f));
        }

        // Attirer tous les joueurs proches
        for (Player player : getNearbyPlayers(HOOK_RADIUS)) {
            hookPlayer(player, bossLoc);
        }

        // Annonce
        for (Player player : getNearbyPlayers(30)) {
            player.sendMessage("§4" + type.getDisplayName() + " §7utilise §cHook§7!");
        }
    }

    /**
     * Attire un joueur vers le boss
     */
    private void hookPlayer(Player player, Location bossLoc) {
        // Calculer la direction vers le boss
        Vector direction = bossLoc.toVector().subtract(player.getLocation().toVector());
        double distance = direction.length();

        if (distance > 0) {
            direction.normalize();

            // Appliquer la vélocité
            Vector velocity = direction.multiply(HOOK_STRENGTH);
            velocity.setY(0.3); // Légère élévation
            player.setVelocity(velocity);

            // Effets sur le joueur
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

            // Particules de chaîne
            World world = player.getWorld();
            Vector step = direction.multiply(0.5);
            Location current = player.getLocation().add(0, 1, 0);

            for (int i = 0; i < distance * 2; i++) {
                world.spawnParticle(Particle.DUST, current, 2, 0.1, 0.1, 0.1,
                    new Particle.DustOptions(Color.GRAY, 1.5f));
                current.add(step);
            }

            // Son de chaîne
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.5f, 1f);

            // Message
            player.sendMessage("§c§l⚠ §7Vous êtes attiré vers " + type.getDisplayName() + "!");
        }
    }

    @Override
    protected void onDamageReceived(Player attacker, double damage) {
        // Le Boucher gagne de la résistance à chaque attaque qu'il inflige (pas reçue)
        // Cette méthode est appelée quand il REÇOIT des dégâts, pas quand il frappe
    }

    /**
     * Appelé quand le boss frappe un joueur
     */
    public void onAttackPlayer(Player victim) {
        attackCount++;

        // Vérifier si on doit augmenter la résistance
        if (attackCount >= ATTACKS_PER_RESISTANCE && resistanceLevel < MAX_RESISTANCE_LEVEL) {
            attackCount = 0;
            resistanceLevel++;
            applyResistance();

            // Notification
            for (Player player : getNearbyPlayers(30)) {
                player.sendMessage("§4" + type.getDisplayName() + " §7devient plus résistant! §c(Niveau " + resistanceLevel + ")");
            }
        }
    }

    /**
     * Applique le niveau de résistance actuel
     */
    private void applyResistance() {
        if (entity == null) return;

        // Retirer l'ancien effet
        entity.removePotionEffect(PotionEffectType.RESISTANCE);

        // Appliquer le nouveau niveau
        if (resistanceLevel > 0) {
            entity.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                Integer.MAX_VALUE,
                resistanceLevel - 1,
                false, true
            ));
        }

        // Effets visuels
        World world = entity.getWorld();
        Location loc = entity.getLocation();
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
        world.spawnParticle(Particle.BLOCK, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5,
            Material.IRON_BLOCK.createBlockData());
    }

    @Override
    protected void tick() {
        // Particules de sang autour du Boucher
        if (entity != null && entity.isValid()) {
            Location loc = entity.getLocation().add(0, 1, 0);
            World world = loc.getWorld();
            if (world != null) {
                // Gouttes de sang occasionnelles
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.DUST, loc.clone().add(
                        (Math.random() - 0.5) * 2,
                        Math.random() * 2,
                        (Math.random() - 0.5) * 2
                    ), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f));
                }
            }
        }
    }

    @Override
    protected void updateBossBar() {
        super.updateBossBar();

        // Ajouter l'info de résistance
        if (bossBar != null && resistanceLevel > 0) {
            bossBar.setTitle(type.getTitleName() + " §7[Résistance: §c" + resistanceLevel + "§7] - §c" + getFormattedHealth());
        }
    }
}
