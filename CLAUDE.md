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

