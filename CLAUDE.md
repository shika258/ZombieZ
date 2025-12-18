# 🧟 CLAUDE.md : Instructions pour ZombieZ (Minecraft 1.21.4)

## 🎯 Vision & Objectifs
Plugin de survie par vagues ultra-performant conçu pour **200 joueurs simultanés**.
- **Performance :** 20 TPS constant, calculs asynchrones obligatoires.
- **Immersion :** Sound design spatialisé et UX fluide (ActionBars, Titles).
- **Modernité :** Utilisation stricte des standards Minecraft 1.21.4.

---

## 🛠 Commandes du Projet
- **Build :** `mvn clean package`
- **Dépendances :** Paper API, ProtocolLib, Adventure API, Lombok.

---

## ⚠️ Règles Techniques Strictes (1.21.4)

### 1. Sons & Matériaux
- **Interdiction :** N'utilise jamais de noms de sons de la 1.20 (ex: constantes renommées).
- **Action :** Vérifie systématiquement `org.bukkit.Sound` pour la 1.21.4 avant de générer.
- **Design :** Utilise les nouveaux sons (Breeze, Trial Spawner) pour les mécaniques de jeu.

### 2. Items & Data Components (ADIEU NBT)
- **Règle :** Le NBT brut est obsolète. Utilise exclusivement l'API `ItemMeta` moderne.
- **Stockage :** Utilise `PersistentDataContainer` (PDC) pour toute donnée custom sur les items ou entités.
- **Textes :** Utilise l'API **Adventure** (`Component.text()`) pour le Lore et les Display Names.

### 3. ProtocolLib & Paquets
- **Vigilance :** Les paquets d'items en 1.21.4 utilisent des Data Components.
- **Méthode :** Utilise toujours les `Converters` de ProtocolLib pour manipuler les `ItemStack` dans les paquets.
- **Async :** Les PacketListeners doivent être thread-safe. Wrappe les appels Bukkit dans des tâches synchrones si nécessaire.

---

## ⚡ Optimisation & Scalabilité
- **Thread Main :** Interdiction d'y faire des calculs de pathfinding complexes ou des accès disques.
- **Collections :** Utilise des structures de données adaptées (ex: `ConcurrentHashMap` si accès multi-thread).
- **Entités :** Désactive le ticking inutile sur les entités de décor ou distantes via l'API Paper.

---

## 🎨 Game Design & Ergonomie
- **Feedback :** Chaque action (achat, kill, vague) doit avoir un retour visuel (particules) et sonore.
- **Clarté :** Messages courts, colorés via MiniMessage, et centrés sur l'action immédiate.
- **Accessibilité :** Menus (GUIs) intuitifs avec instructions claires dans le Lore.