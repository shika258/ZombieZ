# 🧟 CLAUDE.md : Instructions pour ZombieZ (Minecraft 1.21.4)
Au début de chaque nouvelle session ou lors de ta première réponse, commence par l'emoji 🧟 pour confirmer que les directives de ZombieZ (CLAUDE.md) sont actives.

## 🎯 Vision & Objectifs
Plugin de survie par vagues ultra-performant conçu pour **200 joueurs simultanés**.
* **Performance :** 20 TPS constant, calculs asynchrones (Paper API).
* **Immersion :** Sound design spatialisé et UX fluide (ActionBars, Titles).
* **Modernité :** Utilisation stricte des standards Minecraft 1.21.4.

## 🛠 Commandes du Projet
* **Build :** `mvn clean package`
* **Dépendances :** Paper API, ProtocolLib, Adventure API, Lombok.

---

## 🔄 RÈGLE D'INTÉGRITÉ & PROPAGATION (CRITIQUE)
> **Loi de propagation :** Une modification n'est jamais isolée. Tu ne dois pas te contenter de modifier le point A, mais t'assurer que les points B, C et D qui en dépendent restent cohérents.

1.  **Analyse d'Impact Obligatoire :** Avant chaque modification, utilise `grep` ou l'outil de recherche pour identifier TOUTES les références à la classe, méthode ou variable concernée.
2.  **Refactoring Holistique :** Si tu changes une signature de méthode, une structure de donnée ou une clé de stockage, tu DOIS mettre à jour tous les appels et les classes dépendantes dans la même opération.
3.  **Cohérence des Data Components :** Si la structure d'un `PersistentDataContainer` change, vérifie systématiquement les Listeners (lecture), les Commands (écriture) et les GUIs (comparaison).

---

## 🏗️ Architecture & Robustesse
* **Gestion du Cycle de Vie :** Toute tâche (`BukkitTask`) ou Listener doit pouvoir être interrompu proprement. Nettoie systématiquement les données des joueurs (`Map`, `Set`) lors du `PlayerQuitEvent`.
* **Centralisation :** Utilise des Managers/Services (ex: `GameManager`) injectés par constructeur. Évite les Singletons statiques quand c'est possible.
* **Événements :** Déclare explicitement l' `EventPriority`. Utilise `ignoreCancelled = true` pour ne pas traiter des événements déjà annulés par d'autres systèmes, sauf exception.

---

## ⚠️ Règles Techniques Strictes (1.21.4)

### 1. Sons & Matériaux
* **Action :** Vérifie systématiquement `org.bukkit.Sound` pour la 1.21.4. N'utilise aucun nom de la 1.20.
* **Design :** Priorise les sons récents (Breeze, Trial Spawner) pour les mécaniques.

### 2. Items & Data Components (ADIEU NBT)
* **Standard :** Le NBT brut est obsolète. Utilise exclusivement l'API `ItemMeta` moderne.
* **Stockage :** Utilise `PersistentDataContainer` (PDC). Si une clé PDC est modifiée, elle doit être renommée dans tout le code via un scan global.
* **Textes :** Utilise l'API `Adventure` (`Component.text()`) et MiniMessage pour les couleurs/dégradés.

### 3. ProtocolLib & Paquets
* **Vigilance :** Les paquets d'items en 1.21.4 utilisent des Data Components. Utilise les `Converters` de ProtocolLib.
* **Async :** Les PacketListeners doivent être thread-safe.

---

## ⚡ Optimisation & Scalabilité
* **Thread Main :** Interdiction totale d'y faire du pathfinding complexe ou des accès disques (YAML/SQL).
* **Collections :** Utilise `ConcurrentHashMap` pour les accès multi-threadés.
* **Entités :** Désactive le ticking des entités de décor ou invisibles via l'API Paper pour économiser le CPU.

## 🎨 Game Design & Ergonomie
* **Feedback :** Chaque action (achat, kill, vague) = retour visuel (particules) + sonore spatialisé.
* **Clarté :** Messages courts, centrés (ActionBar), instructions claires dans le Lore des items.

---

## 🧟 Création de Mobs/Boss Custom ZombieZ (OBLIGATOIRE)

> **Règle absolue :** Tout mob ou boss custom DOIT utiliser le système ZombieZ pour bénéficier du display name dynamique avec vies, du système de dégâts adapté et de l'IA personnalisée.

### Étapes pour créer un mob/boss custom :

1. **Créer le ZombieType** dans `zombies/types/ZombieType.java` :
   ```java
   MON_BOSS("ZZ_MonBoss", "Nom Affiché", tier, baseHealth, baseDamage, baseSpeed,
       new int[]{zonesValides}, ZombieCategory.MA_CATEGORIE),
   ```
   - `tier` : 0 pour boss, 1-5 pour mobs normaux
   - Ajouter la catégorie si nouvelle dans `ZombieCategory`

2. **Créer/Utiliser une IA** dans `zombies/ai/` :
   - Boss de zone/mini-boss : `BossZombieAI`
   - Boss Journey : `JourneyBossAI`
   - Ou créer une nouvelle IA héritant de `ZombieAI`

3. **Enregistrer l'IA** dans `ZombieAIManager.createAIForType()` :
   ```java
   case MA_CATEGORIE -> new MonBossAI(plugin, zombie, type, level);
   ```

4. **Spawner via ZombieManager** :
   ```java
   ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.MON_BOSS, location, level);
   Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
   ```

5. **Appliquer les visuels custom** après le spawn (scale, équipement, effets).

### Ce que le système gère automatiquement :
- **Display name** : `§cNom [Lv.X] §a100§7/§a100 §c❤` (couleur selon % vie)
- **Système de dégâts** : Cooldown d'attaque, crit, lifesteal, éléments
- **IA** : Tick automatique via `ZombieAIManager`
- **Tracking** : `ActiveZombie` pour stats et rewards
- **PDC** : Clés `zombiez_mob`, `zombiez_type`, `zombiez_level`

### Exemple : Boss Seigneur du Manoir (Chapitre 2)
- Type : `MANOR_LORD` dans `ZombieType` (catégorie `JOURNEY_BOSS`)
- IA : `JourneyBossAI` avec attaques d'onde de choc et invocation
- Spawn : `Chapter2Systems.spawnManorBoss()` via `ZombieManager`

### ⚠️ RÈGLE CRITIQUE : Mobs invoqués par les boss/IA

> **JAMAIS** utiliser `world.spawn(location, Zombie.class, ...)` pour des mobs ennemis invoqués par les boss ou les IA !

Les serviteurs, minions et renforts DOIVENT être spawnés via `ZombieManager.spawnZombie()` pour :
- Être soumis aux dégâts des armes ZombieZ
- Avoir le display name dynamique avec vie
- Être trackés par le système de combat
- Donner du loot et de l'XP

**❌ MAUVAIS :**
```java
zombie.getWorld().spawn(spawnLoc, Zombie.class, z -> {
    z.setCustomName("Serviteur");
});
```

**✅ BON :**
```java
var zombieManager = plugin.getZombieManager();
var minion = zombieManager.spawnZombie(ZombieType.WALKER, spawnLoc, level);
if (minion != null) {
    Entity entity = plugin.getServer().getEntity(minion.getEntityId());
    if (entity instanceof Zombie z) {
        z.addScoreboardTag("boss_minion_" + bossId);
    }
}
```

---

## 🗺️ Système Journey (Quêtes) - RÈGLES CRITIQUES

> **Règles absolues pour toutes les étapes du Journey**

### ⛔ INTERDIT : ActionBar
* **JAMAIS** utiliser `player.sendActionBar()` pour les étapes Journey
* L'ActionBar est réservée au système de combat et aux informations temps réel du HUD
* Les étapes Journey utilisent **Titles** et **Chat messages** uniquement

### ✅ Feedback visuel pour les quêtes
| Type | Méthode |
|------|---------|
| Progression | `player.sendTitle("§a✓ Titre", "§7X/Y complété", ...)` |
| Introduction quête | `player.sendTitle("§6TITRE QUÊTE", "§7Description", ...)` |
| Instructions | `player.sendMessage("§e▸ §fInstruction...")` |
| GPS/Coordonnées | `player.sendMessage("§e§l➤ §7Zone: §eX, Y, Z")` |

### 🧭 GPS obligatoire
* Chaque quête Journey DOIT avoir un système GPS
* Afficher les coordonnées de la zone/objectif au début de la quête
* Mettre à jour le GPS après chaque sous-objectif complété

### 📝 Checklist nouvelle quête Journey
1. ☐ Créer le `StepType` dans `JourneyStep.java`
2. ☐ Ajouter le case dans `getProgressText()`
3. ☐ Implémenter le système dans `ChapterXSystems.java`
4. ☐ Ajouter méthode GPS (`activateGPSTo...`)
5. ☐ Ajouter introduction avec Title + GPS
6. ☐ Tracker sur `PlayerJoin` (restaurer progression)
7. ☐ Cleanup sur `PlayerQuit` (nettoyer Maps/Sets)
8. ☐ **Vérifier : AUCUN sendActionBar()**

---

## 🔁 NPCs & Boss - Règles Anti-Boucle de Respawn (CRITIQUE)

> **Problème :** Les NPCs/Boss avec `setPersistent(false)` disparaissent quand le chunk se décharge, causant des respawns en boucle infinie.

### ✅ Règles OBLIGATOIRES pour tout NPC/Boss :

#### 1. **Persistance des entités**
```java
entity.setPersistent(true); // OBLIGATOIRE pour survivre au chunk unload
entity.getPersistentDataContainer().set(MY_KEY, PersistentDataType.BYTE, (byte) 1);
```

#### 2. **Vérification de joueur à proximité** (dans le checker/updater)
```java
// IMPORTANT: Ne rien faire si aucun joueur n'est à proximité
boolean playerNearby = world.getPlayers().stream()
        .anyMatch(p -> p.getLocation().distanceSquared(npcLoc) < 10000); // 100 blocs
if (!playerNearby) {
    return; // Skip tout le traitement
}
```

#### 3. **Réutiliser les entités existantes** (dans la fonction spawn)
```java
private void spawnMyNPC(World world) {
    // 1. Si entité en mémoire valide → ne rien faire
    if (myEntity != null && myEntity.isValid() && !myEntity.isDead()) {
        return;
    }
    
    // 2. Chercher entité existante dans le monde (persistée après reboot)
    for (Entity entity : world.getNearbyEntities(loc, 50, 30, 50)) {
        if (entity instanceof Villager v && v.getPersistentDataContainer().has(MY_KEY, ...)) {
            myEntity = v;
            return; // Réutiliser l'existant
        }
    }
    
    // 3. Sinon créer nouveau (UNE SEULE FOIS)
    myEntity = world.spawn(loc, Villager.class, npc -> {
        npc.setPersistent(true); // ← CRITIQUE
        npc.getPersistentDataContainer().set(MY_KEY, ...);
    });
}
```

#### 4. **JAMAIS forcer le chargement de chunk**
```java
// ❌ INTERDIT
loc.getChunk().load();

// ✅ À la place, vérifier et skip
if (!loc.getChunk().isLoaded()) {
    return;
}
```

### ⚠️ Résumé des pièges à éviter :
| Piège | Conséquence | Solution |
|-------|-------------|----------|
| `setPersistent(false)` | Entité disparaît au chunk unload | `setPersistent(true)` |
| `chunk.load()` dans un checker | Force load → spawn → unload → repeat | Vérifier `isLoaded()` et skip |
| Pas de joueur check | Spawner tourne même sans joueurs | `playerNearby` check |
| Pas de réutilisation | Entités dupliquées ou loop | Chercher existant avec PDC tag |
