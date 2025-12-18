package com.rinaorc.zombiez.pets.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.abilities.impl.CritDamagePassive;
import com.rinaorc.zombiez.pets.abilities.impl.DamageMultiplierPassive;
import com.rinaorc.zombiez.pets.abilities.impl.DamageReductionPassive;
import com.rinaorc.zombiez.pets.abilities.impl.InterceptPassive;
import com.rinaorc.zombiez.pets.abilities.impl.MeleeDamagePassive;
import com.rinaorc.zombiez.pets.abilities.impl.MultiAttackPassive;
import com.rinaorc.zombiez.pets.abilities.impl.ParryPassive;
import com.rinaorc.zombiez.pets.abilities.impl.PowerSlowPassive;
import com.rinaorc.zombiez.pets.eggs.EggType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour l'intégration des pets avec le système de combat
 * Gère les bonus passifs, les triggers d'événements, et les drops d'oeufs
 */
public class PetCombatListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // Tracking des cibles des joueurs pour le système de pet
    private static final Map<UUID, UUID> playerTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> targetTimestamps = new ConcurrentHashMap<>();
    private static final long TARGET_EXPIRY_MS = 5000; // La cible expire après 5 secondes sans attaque

    public PetCombatListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Récupère la cible actuelle du joueur (le dernier mob attaqué)
     * @return L'entité ciblée ou null si aucune cible valide
     */
    public static LivingEntity getPlayerTarget(Player player) {
        UUID targetId = playerTargets.get(player.getUniqueId());
        if (targetId == null) return null;

        // Vérifier si la cible n'a pas expiré
        Long timestamp = targetTimestamps.get(player.getUniqueId());
        if (timestamp == null || System.currentTimeMillis() - timestamp > TARGET_EXPIRY_MS) {
            playerTargets.remove(player.getUniqueId());
            targetTimestamps.remove(player.getUniqueId());
            return null;
        }

        // Chercher l'entité dans le monde
        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity.getUniqueId().equals(targetId) && entity instanceof LivingEntity living) {
                if (living.isValid() && !living.isDead()) {
                    return living;
                }
            }
        }

        // Cible non trouvée ou morte
        playerTargets.remove(player.getUniqueId());
        targetTimestamps.remove(player.getUniqueId());
        return null;
    }

    /**
     * Enregistre une cible pour un joueur
     */
    private static void setPlayerTarget(Player player, LivingEntity target) {
        playerTargets.put(player.getUniqueId(), target.getUniqueId());
        targetTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Nettoie les données d'un joueur déconnecté
     */
    public static void cleanupPlayer(UUID playerId) {
        playerTargets.remove(playerId);
        targetTimestamps.remove(playerId);
    }

    /**
     * Gère les dégâts infligés par le joueur
     * Applique les bonus du pet
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerDamager(event.getDamager());
        if (player == null)
            return;

        if (!(event.getEntity() instanceof LivingEntity target))
            return;
        if (target instanceof Player)
            return; // Pas de PvP

        // Enregistrer la cible pour le système de pet
        if (target instanceof Monster) {
            setPlayerTarget(player, target);
        }

        PlayerPetData playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getEquippedPet() == null)
            return;

        PetType petType = playerData.getEquippedPet();
        PetData petData = playerData.getPet(petType);
        if (petData == null)
            return;

        double originalDamage = event.getDamage();
        double modifiedDamage = originalDamage;

        // Appliquer les bonus de dégâts selon le pet
        PetAbility passive = plugin.getPetManager().getAbilityRegistry().getPassive(petType);

        // Bonus de dégâts globaux (Dragon Pygmée, etc.)
        if (passive instanceof DamageMultiplierPassive dmp) {
            modifiedDamage *= (1 + dmp.getMultiplier() * petData.getStatMultiplier());
        }

        // Bonus de dégâts mêlée (Titan Miniature)
        if (passive instanceof MeleeDamagePassive mdp) {
            if (event.getDamager() instanceof Player) { // Attaque directe
                modifiedDamage *= (1 + mdp.getMeleeBonus() * petData.getStatMultiplier());
            }
        }

        // Bonus de dégâts critiques (Félin de l'Ombre)
        if (passive instanceof CritDamagePassive cdp) {
            if (isCriticalHit(event)) {
                modifiedDamage *= (1 + cdp.getCritBonus() * petData.getStatMultiplier());
            }
        }

        // Bonus de puissance mais malus de vitesse (Colossus)
        if (passive instanceof PowerSlowPassive psp) {
            modifiedDamage *= (1 + psp.getDamageBonus() * petData.getStatMultiplier());
        }

        // Multi-attaque (Hydre)
        if (passive instanceof MultiAttackPassive map) {
            int attackCount = map.getAttackCount();
            // Les attaques supplémentaires font des dégâts réduits
            // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
            for (int i = 1; i < attackCount; i++) {
                target.setMetadata("zombiez_secondary_damage", new FixedMetadataValue(plugin, true));
                target.damage(originalDamage * 0.3, player);
            }
        }

        // Appliquer les dégâts modifiés
        event.setDamage(modifiedDamage);

        // Enregistrer les dégâts pour les stats
        petData.addDamage((long) modifiedDamage);

        // Trigger les effets onDamageDealt
        if (passive != null) {
            passive.onDamageDealt(player, petData, target, modifiedDamage);
        }
    }

    /**
     * Gère les dégâts reçus par le joueur
     * Applique les réductions du pet
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        PlayerPetData playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getEquippedPet() == null)
            return;

        PetType petType = playerData.getEquippedPet();
        PetData petData = playerData.getPet(petType);
        if (petData == null)
            return;

        double originalDamage = event.getDamage();
        double modifiedDamage = originalDamage;

        PetAbility passive = plugin.getPetManager().getAbilityRegistry().getPassive(petType);

        // Réduction de dégâts (Scarabée Blindé)
        if (passive instanceof DamageReductionPassive drp) {
            modifiedDamage *= (1 - drp.getReductionPercent() * petData.getStatMultiplier());
        }

        // Interception (Golem de Poche)
        if (passive instanceof InterceptPassive ip) {
            modifiedDamage *= (1 - ip.getInterceptPercent() * petData.getStatMultiplier());
        }

        // Parade automatique (Spectre Gardien)
        if (passive instanceof ParryPassive pp) {
            if (pp.canParry(player.getUniqueId())) {
                pp.triggerParry(player.getUniqueId());
                event.setCancelled(true);
                player.sendMessage("§a[Pet] §7Parade automatique!");
                return;
            }
        }

        event.setDamage(modifiedDamage);

        // Trigger les effets onDamageReceived
        if (passive != null) {
            passive.onDamageReceived(player, petData, modifiedDamage);
        }
    }

    /**
     * Gère la mort d'un mob
     * Drop d'oeufs et trigger onKill
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null)
            return;

        Player player = event.getEntity().getKiller();
        LivingEntity killed = event.getEntity();

        // Vérifier si c'est un zombie ZombieZ
        if (!killed.getScoreboardTags().contains("zombiez_mob"))
            return;

        PlayerPetData playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());

        // Chance de drop d'oeuf
        dropPetEgg(player, killed);

        // Si le joueur a un pet équipé, trigger onKill
        if (playerData != null && playerData.getEquippedPet() != null) {
            PetType petType = playerData.getEquippedPet();
            PetData petData = playerData.getPet(petType);

            if (petData != null) {
                petData.addKill();

                PetAbility passive = plugin.getPetManager().getAbilityRegistry().getPassive(petType);
                if (passive != null) {
                    passive.onKill(player, petData, killed);
                }
            }
        }
    }

    /**
     * Gère l'activation de capacité via touche R (clic droit avec main vide)
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        if (!event.getAction().name().contains("RIGHT"))
            return;

        Player player = event.getPlayer();

        // Vérifier si main vide et sneaking
        if (player.getInventory().getItemInMainHand().getType().isAir() && player.isSneaking()) {
            plugin.getPetManager().activateAbility(player);
        }
    }

    /**
     * Détermine si une attaque est un coup critique
     */
    private boolean isCriticalHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            // Un coup critique en vanilla = joueur en chute et pas au sol
            return player.getFallDistance() > 0 && !player.isOnGround() && !player.isClimbing();
        }
        return false;
    }

    /**
     * Obtient le joueur à l'origine des dégâts
     */
    private Player getPlayerDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    /**
     * Gère le drop d'oeufs de pet
     */
    private void dropPetEgg(Player player, LivingEntity killed) {
        // Taux de base
        double baseChance = 0.003; // 0.3%

        // Modifier selon le type de mob
        if (killed.getScoreboardTags().contains("elite")) {
            baseChance = 0.02; // 2% pour élite
        } else if (killed.getScoreboardTags().contains("boss")) {
            baseChance = 0.15; // 15% pour mini-boss
        }

        // Bonus de loot du pet Rat
        PlayerPetData playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (playerData != null && playerData.getEquippedPet() == PetType.RAT_CATACOMBES) {
            PetData petData = playerData.getPet(PetType.RAT_CATACOMBES);
            if (petData != null) {
                baseChance *= (1 + 0.05 * petData.getStatMultiplier());
            }
        }

        // Bonus VIP
        double vipBonus = plugin.getPlayerDataManager().getPlayer(player).getLootLuckBonus();
        baseChance *= (1 + vipBonus);

        // Roll!
        if (random.nextDouble() < baseChance) {
            EggType eggType = killed.getScoreboardTags().contains("boss") ? EggType.ZONE : EggType.STANDARD;

            plugin.getPetManager().giveEgg(player, eggType, 1);
        }
    }
}
