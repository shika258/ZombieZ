# 🧟 CLAUDE.md : Instructions pour ZombieZ (Minecraft 1.21.4)

## 🎯 Vision & Objectifs
Plugin de survie par vagues ultra-performant conçu pour **200 joueurs simultanés**.
* **Performance :** 20 TPS constant, calculs asynchrones obligatoires.
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
4.  **Vérification de Flux :** Après modification, scanne le projet pour t'assurer qu'aucune "chaîne de dépendance" n'est brisée.

---

## ⚠️ Règles Techniques Strictes (1.21.4)

### 1. Sons & Matériaux
* **Interdiction :** N'utilise JAMAIS de noms de sons obsolètes (ex: 1.20).
* **Action :** Vérifie systématiquement `org.bukkit.Sound` pour la 1.21.4.
* **Design :** Priorise les sons récents (Breeze, Trial Spawner) pour les mécaniques.

### 2. Items & Data Components (ADIEU NBT)
* **Standard :** Le NBT brut est obsolète. Utilise exclusivement l'API `ItemMeta`.
* **Stockage :** Utilise `PersistentDataContainer` (PDC). Si une clé PDC est modifiée, elle doit être renommée dans tout le code.
* **Textes :** Utilise l'API `Adventure` (`Component.text()`) pour tout ce qui est visible par le joueur.

### 3. ProtocolLib & Paquets
* **Vigilance :** Les paquets d'items en 1.21.4 utilisent des Data Components.
* **Méthode :** Utilise les `Converters` de ProtocolLib pour manipuler les `ItemStack`.
* **Async :** Les PacketListeners doivent être thread-safe.

---

## ⚡ Optimisation & Scalabilité
* **Thread Main :** Interdiction totale d'y faire du pathfinding complexe ou des accès disques.
* **Collections :** Utilise des structures thread-safe (ex: `ConcurrentHashMap`) si l'accès est multi-thread.
* **Entités :** Désactive le ticking des entités de décor via l'API Paper.

## 🎨 Game Design & Ergonomie
* **Feedback :** Chaque action (achat, kill, vague) = retour visuel (particules) + sonore.
* **Clarté :** Messages courts, formatés via MiniMessage, centrés sur l'action.
* **Accessibilité :** GUIs intuitifs avec instructions claires dans le Lore.
