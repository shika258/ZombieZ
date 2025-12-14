package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de titres et cosmétiques
 */
public class CosmeticManager {

    private final ZombieZPlugin plugin;
    
    // Tous les titres disponibles
    @Getter
    private final Map<String, Title> titles;
    
    // Toutes les particules disponibles
    @Getter
    private final Map<String, ParticleEffect> particleEffects;
    
    // Toutes les auras disponibles
    @Getter
    private final Map<String, Aura> auras;
    
    // Cosmétiques actifs par joueur
    private final Map<UUID, ActiveCosmetics> activeCosmetics;
    
    // Tâches d'effet par joueur
    private final Map<UUID, BukkitRunnable> effectTasks;

    public CosmeticManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.titles = new LinkedHashMap<>();
        this.particleEffects = new LinkedHashMap<>();
        this.auras = new LinkedHashMap<>();
        this.activeCosmetics = new ConcurrentHashMap<>();
        this.effectTasks = new ConcurrentHashMap<>();
        
        registerAllCosmetics();
    }

    /**
     * Enregistre tous les cosmétiques
     */
    private void registerAllCosmetics() {
        // ============ TITRES ============
        
        // Titres de progression
        registerTitle(Title.builder()
            .id("newbie")
            .name("Nouveau")
            .displayFormat("§7[Nouveau]")
            .source(TitleSource.LEVEL)
            .requirement(1)
            .build());
        
        registerTitle(Title.builder()
            .id("survivor")
            .name("Survivant")
            .displayFormat("§a[Survivant]")
            .source(TitleSource.LEVEL)
            .requirement(10)
            .build());
        
        registerTitle(Title.builder()
            .id("warrior")
            .name("Guerrier")
            .displayFormat("§e[Guerrier]")
            .source(TitleSource.LEVEL)
            .requirement(25)
            .build());
        
        registerTitle(Title.builder()
            .id("veteran")
            .name("Vétéran")
            .displayFormat("§6[Vétéran]")
            .source(TitleSource.LEVEL)
            .requirement(50)
            .build());
        
        registerTitle(Title.builder()
            .id("legend")
            .name("Légende")
            .displayFormat("§6[§e★§6Légende§e★§6]")
            .source(TitleSource.LEVEL)
            .requirement(100)
            .build());
        
        // Titres de combat
        registerTitle(Title.builder()
            .id("slayer")
            .name("Tueur")
            .displayFormat("§c[Tueur]")
            .source(TitleSource.KILLS)
            .requirement(1000)
            .build());
        
        registerTitle(Title.builder()
            .id("executioner")
            .name("Bourreau")
            .displayFormat("§4[Bourreau]")
            .source(TitleSource.KILLS)
            .requirement(10000)
            .build());
        
        registerTitle(Title.builder()
            .id("death_incarnate")
            .name("Mort Incarnée")
            .displayFormat("§4§l[☠ Mort Incarnée ☠]")
            .source(TitleSource.KILLS)
            .requirement(100000)
            .build());
        
        // Titres de zone
        registerTitle(Title.builder()
            .id("explorer")
            .name("Explorateur")
            .displayFormat("§2[Explorateur]")
            .source(TitleSource.ZONES)
            .requirement(5)
            .build());
        
        registerTitle(Title.builder()
            .id("pioneer")
            .name("Pionnier")
            .displayFormat("§a[Pionnier]")
            .source(TitleSource.ZONES)
            .requirement(11)
            .build());
        
        // Titres de boss
        registerTitle(Title.builder()
            .id("boss_hunter")
            .name("Chasseur de Boss")
            .displayFormat("§d[Chasseur de Boss]")
            .source(TitleSource.BOSSES)
            .requirement(10)
            .build());
        
        registerTitle(Title.builder()
            .id("patient_zero_slayer")
            .name("Vainqueur de Patient Zéro")
            .displayFormat("§5§l[☣ Vainqueur ☣]")
            .source(TitleSource.SPECIAL)
            .requirement(1)
            .build());
        
        // Titres de prestige
        registerTitle(Title.builder()
            .id("prestige_1")
            .name("Prestige I")
            .displayFormat("§b[P1]")
            .source(TitleSource.PRESTIGE)
            .requirement(1)
            .build());
        
        registerTitle(Title.builder()
            .id("prestige_5")
            .name("Prestige V")
            .displayFormat("§9[P5]")
            .source(TitleSource.PRESTIGE)
            .requirement(5)
            .build());
        
        registerTitle(Title.builder()
            .id("prestige_10")
            .name("Prestige X")
            .displayFormat("§5§l[PX]")
            .source(TitleSource.PRESTIGE)
            .requirement(10)
            .build());
        
        // Titres spéciaux
        registerTitle(Title.builder()
            .id("vip_bronze")
            .name("VIP Bronze")
            .displayFormat("§6[VIP]")
            .source(TitleSource.SPECIAL)
            .build());
        
        registerTitle(Title.builder()
            .id("vip_gold")
            .name("VIP Or")
            .displayFormat("§e§l[VIP+]")
            .source(TitleSource.SPECIAL)
            .build());
        
        registerTitle(Title.builder()
            .id("vip_diamond")
            .name("VIP Diamant")
            .displayFormat("§b§l[VIP++]")
            .source(TitleSource.SPECIAL)
            .build());
        
        // ============ PARTICULES ============
        
        registerParticle(ParticleEffect.builder()
            .id("trail_blood")
            .name("Trainée de Sang")
            .particle(Particle.DUST)
            .dustColor(Color.RED)
            .count(3)
            .speed(0)
            .source(EffectSource.BATTLE_PASS)
            .icon(Material.REDSTONE)
            .build());
        
        registerParticle(ParticleEffect.builder()
            .id("trail_fire")
            .name("Trainée de Feu")
            .particle(Particle.FLAME)
            .count(2)
            .speed(0.01)
            .source(EffectSource.ACHIEVEMENT)
            .icon(Material.BLAZE_POWDER)
            .build());
        
        registerParticle(ParticleEffect.builder()
            .id("trail_soul")
            .name("Trainée d'Âmes")
            .particle(Particle.SOUL_FIRE_FLAME)
            .count(2)
            .speed(0.01)
            .source(EffectSource.SHOP)
            .icon(Material.SOUL_LANTERN)
            .build());
        
        registerParticle(ParticleEffect.builder()
            .id("trail_hearts")
            .name("Trainée de Cœurs")
            .particle(Particle.HEART)
            .count(1)
            .speed(0)
            .source(EffectSource.SHOP)
            .icon(Material.RED_DYE)
            .build());
        
        registerParticle(ParticleEffect.builder()
            .id("trail_enchant")
            .name("Trainée Enchantée")
            .particle(Particle.ENCHANT)
            .count(5)
            .speed(0.5)
            .source(EffectSource.SHOP)
            .icon(Material.ENCHANTING_TABLE)
            .build());
        
        registerParticle(ParticleEffect.builder()
            .id("trail_end_rod")
            .name("Trainée Éthérée")
            .particle(Particle.END_ROD)
            .count(2)
            .speed(0.01)
            .source(EffectSource.BATTLE_PASS)
            .icon(Material.END_ROD)
            .build());
        
        // ============ AURAS ============
        
        registerAura(Aura.builder()
            .id("aura_death")
            .name("Aura de Mort")
            .particle(Particle.SOUL)
            .radius(1.5)
            .particleCount(10)
            .source(EffectSource.BATTLE_PASS)
            .icon(Material.WITHER_ROSE)
            .build());
        
        registerAura(Aura.builder()
            .id("aura_flame")
            .name("Aura de Flammes")
            .particle(Particle.FLAME)
            .radius(1.2)
            .particleCount(15)
            .source(EffectSource.SHOP)
            .icon(Material.FIRE_CHARGE)
            .build());
        
        registerAura(Aura.builder()
            .id("aura_electric")
            .name("Aura Électrique")
            .particle(Particle.ELECTRIC_SPARK)
            .radius(1.0)
            .particleCount(8)
            .source(EffectSource.ACHIEVEMENT)
            .icon(Material.LIGHTNING_ROD)
            .build());
        
        registerAura(Aura.builder()
            .id("aura_nature")
            .name("Aura de Nature")
            .particle(Particle.HAPPY_VILLAGER)
            .radius(1.5)
            .particleCount(12)
            .source(EffectSource.SHOP)
            .icon(Material.OAK_SAPLING)
            .build());
        
        registerAura(Aura.builder()
            .id("aura_void")
            .name("Aura du Vide")
            .particle(Particle.PORTAL)
            .radius(2.0)
            .particleCount(20)
            .source(EffectSource.BATTLE_PASS)
            .icon(Material.ENDER_EYE)
            .build());
        
        registerAura(Aura.builder()
            .id("aura_legendary")
            .name("Aura Légendaire")
            .particle(Particle.TOTEM_OF_UNDYING)
            .radius(1.5)
            .particleCount(25)
            .source(EffectSource.EXCLUSIVE)
            .icon(Material.NETHER_STAR)
            .build());
    }

    private void registerTitle(Title title) {
        titles.put(title.getId(), title);
    }

    private void registerParticle(ParticleEffect effect) {
        particleEffects.put(effect.getId(), effect);
    }

    private void registerAura(Aura aura) {
        auras.put(aura.getId(), aura);
    }

    /**
     * Obtient les cosmétiques actifs d'un joueur
     */
    public ActiveCosmetics getActiveCosmetics(UUID playerId) {
        return activeCosmetics.computeIfAbsent(playerId, id -> new ActiveCosmetics());
    }

    /**
     * Équipe un titre
     */
    public boolean equipTitle(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) {
            player.sendMessage("§cTitre introuvable!");
            return false;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return false;
        
        // Vérifier si débloqué
        if (!hasUnlockedTitle(data, title)) {
            player.sendMessage("§cVous n'avez pas débloqué ce titre!");
            return false;
        }
        
        ActiveCosmetics cosmetics = getActiveCosmetics(player.getUniqueId());
        cosmetics.setActiveTitle(titleId);
        
        player.sendMessage("§aTitre équipé: " + title.getDisplayFormat());
        
        // Mettre à jour le display name
        updateDisplayName(player);
        
        return true;
    }

    /**
     * Vérifie si un joueur a débloqué un titre
     */
    public boolean hasUnlockedTitle(PlayerData data, Title title) {
        if (data.getTitles().contains(title.getId())) return true;
        
        // Vérifier les titres automatiques
        return switch (title.getSource()) {
            case LEVEL -> data.getLevel().get() >= title.getRequirement();
            case KILLS -> data.getZombieKills().get() >= title.getRequirement();
            case ZONES -> data.getMaxZoneReached() >= title.getRequirement();
            case BOSSES -> data.getBossKills().get() >= title.getRequirement();
            case PRESTIGE -> data.getPrestige().get() >= title.getRequirement();
            case SPECIAL -> data.getTitles().contains(title.getId());
        };
    }

    /**
     * Équipe une particule
     */
    public boolean equipParticle(Player player, String particleId) {
        ParticleEffect effect = particleEffects.get(particleId);
        if (effect == null) {
            player.sendMessage("§cEffet introuvable!");
            return false;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null || !data.getCosmetics().contains(particleId)) {
            player.sendMessage("§cVous n'avez pas débloqué cet effet!");
            return false;
        }
        
        ActiveCosmetics cosmetics = getActiveCosmetics(player.getUniqueId());
        cosmetics.setActiveParticle(particleId);
        
        player.sendMessage("§aParticules équipées: §e" + effect.getName());
        
        // Démarrer les effets
        startParticleEffects(player);
        
        return true;
    }

    /**
     * Équipe une aura
     */
    public boolean equipAura(Player player, String auraId) {
        Aura aura = auras.get(auraId);
        if (aura == null) {
            player.sendMessage("§cAura introuvable!");
            return false;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null || !data.getCosmetics().contains(auraId)) {
            player.sendMessage("§cVous n'avez pas débloqué cette aura!");
            return false;
        }
        
        ActiveCosmetics cosmetics = getActiveCosmetics(player.getUniqueId());
        cosmetics.setActiveAura(auraId);
        
        player.sendMessage("§aAura équipée: §e" + aura.getName());
        
        // Démarrer les effets
        startParticleEffects(player);
        
        return true;
    }

    /**
     * Retire un cosmétique
     */
    public void unequip(Player player, CosmeticType type) {
        ActiveCosmetics cosmetics = getActiveCosmetics(player.getUniqueId());
        
        switch (type) {
            case TITLE -> {
                cosmetics.setActiveTitle(null);
                updateDisplayName(player);
            }
            case PARTICLE -> {
                cosmetics.setActiveParticle(null);
                stopParticleEffects(player);
            }
            case AURA -> {
                cosmetics.setActiveAura(null);
                stopParticleEffects(player);
            }
        }
        
        player.sendMessage("§7Cosmétique retiré.");
    }

    /**
     * Met à jour le display name du joueur
     */
    public void updateDisplayName(Player player) {
        ActiveCosmetics cosmetics = getActiveCosmetics(player.getUniqueId());
        String titleId = cosmetics.getActiveTitle();
        
        if (titleId == null) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            return;
        }
        
        Title title = titles.get(titleId);
        if (title == null) return;
        
        String displayName = title.getDisplayFormat() + " §f" + player.getName();
        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
    }

    /**
     * Démarre les effets de particules
     */
    public void startParticleEffects(Player player) {
        // Arrêter l'ancienne tâche
        stopParticleEffects(player);
        
        UUID playerId = player.getUniqueId();
        ActiveCosmetics cosmetics = getActiveCosmetics(playerId);
        
        String particleId = cosmetics.getActiveParticle();
        String auraId = cosmetics.getActiveAura();
        
        if (particleId == null && auraId == null) return;
        
        ParticleEffect particleEffect = particleId != null ? particleEffects.get(particleId) : null;
        Aura aura = auraId != null ? auras.get(auraId) : null;
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Player p = plugin.getServer().getPlayer(playerId);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }
                
                Location loc = p.getLocation();
                
                // Trainée de particules
                if (particleEffect != null) {
                    Location trailLoc = loc.clone().add(0, 0.1, 0);
                    if (particleEffect.getParticle() == Particle.DUST && particleEffect.getDustColor() != null) {
                        Particle.DustOptions dust = new Particle.DustOptions(particleEffect.getDustColor(), 1);
                        loc.getWorld().spawnParticle(Particle.DUST, trailLoc, 
                            particleEffect.getCount(), 0.1, 0.1, 0.1, dust);
                    } else {
                        loc.getWorld().spawnParticle(particleEffect.getParticle(), trailLoc,
                            particleEffect.getCount(), 0.1, 0.1, 0.1, particleEffect.getSpeed());
                    }
                }
                
                // Aura
                if (aura != null) {
                    double radius = aura.getRadius();
                    for (int i = 0; i < aura.getParticleCount(); i++) {
                        double angle = (2 * Math.PI / aura.getParticleCount()) * i;
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);
                        Location auraLoc = loc.clone().add(x, 0.5, z);
                        loc.getWorld().spawnParticle(aura.getParticle(), auraLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        };
        
        task.runTaskTimer(plugin, 0L, 5L); // Toutes les 0.25 secondes
        effectTasks.put(playerId, task);
    }

    /**
     * Arrête les effets de particules
     */
    public void stopParticleEffects(Player player) {
        BukkitRunnable task = effectTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Obtient tous les titres débloqués par un joueur
     */
    public List<Title> getUnlockedTitles(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return new ArrayList<>();
        
        List<Title> unlocked = new ArrayList<>();
        for (Title title : titles.values()) {
            if (hasUnlockedTitle(data, title)) {
                unlocked.add(title);
            }
        }
        return unlocked;
    }

    // ==================== INNER CLASSES ====================

    /**
     * Types de cosmétiques
     */
    public enum CosmeticType {
        TITLE, PARTICLE, AURA
    }

    /**
     * Sources des titres
     */
    public enum TitleSource {
        LEVEL, KILLS, ZONES, BOSSES, PRESTIGE, SPECIAL
    }

    /**
     * Sources des effets
     */
    public enum EffectSource {
        SHOP, ACHIEVEMENT, BATTLE_PASS, EXCLUSIVE
    }

    /**
     * Représente un titre
     */
    @Getter
    @Builder
    public static class Title {
        private final String id;
        private final String name;
        private final String displayFormat;
        private final TitleSource source;
        @Builder.Default
        private final int requirement = 0;
    }

    /**
     * Représente un effet de particules
     */
    @Getter
    @Builder
    public static class ParticleEffect {
        private final String id;
        private final String name;
        private final Particle particle;
        private final Color dustColor;
        private final int count;
        private final double speed;
        private final EffectSource source;
        private final Material icon;
    }

    /**
     * Représente une aura
     */
    @Getter
    @Builder
    public static class Aura {
        private final String id;
        private final String name;
        private final Particle particle;
        private final double radius;
        private final int particleCount;
        private final EffectSource source;
        private final Material icon;
    }

    /**
     * Cosmétiques actifs d'un joueur
     */
    @Getter
    public static class ActiveCosmetics {
        private String activeTitle;
        private String activeParticle;
        private String activeAura;
        
        public void setActiveTitle(String title) {
            this.activeTitle = title;
        }
        
        public void setActiveParticle(String particle) {
            this.activeParticle = particle;
        }
        
        public void setActiveAura(String aura) {
            this.activeAura = aura;
        }
    }
}
