# 🐾 ZombieZ - Système de Pets
## Game Design Document & Cahier des Charges

---

## 📋 Table des Matières

1. [Vision Générale](#vision-générale)
2. [Système de Raretés](#système-de-raretés)
3. [Obtention des Pets](#obtention-des-pets)
4. [Système d'Amélioration (Gacha)](#système-damélioration-gacha)
5. [Liste des Pets](#liste-des-pets)
6. [Capacités & Mécaniques](#capacités--mécaniques)
7. [Interface Utilisateur](#interface-utilisateur)
8. [Économie & Équilibrage](#économie--équilibrage)
9. [Spécifications Techniques](#spécifications-techniques)

---

## 🎯 Vision Générale

### Concept
Les Pets sont des compagnons permanents qui suivent le joueur et l'assistent dans sa survie contre les zombies. Chaque Pet possède des capacités uniques et peut être amélioré via un système de duplication inspiré de Clash Royale/Brawl Stars.

### Objectifs
- **Engagement Long-terme**: Donner aux joueurs un objectif de collection et progression
- **Personnalisation**: Permettre des builds uniques Pet + Classe
- **Monétisation Éthique**: Système gacha équilibré (pas pay-to-win)
- **Fun Factor**: Des pets visuellement cool avec des capacités satisfaisantes

### Règles Fondamentales
- Un seul Pet actif à la fois
- Les Pets ne meurent pas (mais peuvent être "KO" temporairement)
- Les Pets gagnent en puissance avec les niveaux
- Chaque Pet a une identité visuelle et gameplay unique

---

## ⭐ Système de Raretés

### Tableau des Raretés

| Rareté | Couleur | Drop Rate (Œuf Standard) | Copies pour Max | Niveaux Max |
|--------|---------|--------------------------|-----------------|-------------|
| **Commun** | §7 Gris | 45% | 50 | 9 |
| **Peu Commun** | §a Vert | 30% | 100 | 9 |
| **Rare** | §b Cyan | 15% | 200 | 9 |
| **Épique** | §d Violet | 7% | 400 | 9 |
| **Légendaire** | §6 Or | 2.5% | 800 | 9 |
| **Mythique** | §c Rouge/Animé | 0.5% | 1500 | 9 |

### Progression des Niveaux (Copies Requises)

```
Niveau 1: 1 copie (débloqué)
Niveau 2: 2 copies
Niveau 3: 4 copies
Niveau 4: 10 copies
Niveau 5: 20 copies
Niveau 6: 50 copies
Niveau 7: 100 copies
Niveau 8: 200 copies
Niveau 9: Variable selon rareté (reste des copies)
```

### Bonus par Niveau
- Chaque niveau augmente les stats de base de **+10%**
- Niveau 5: Débloque la **capacité passive secondaire**
- Niveau 9: Débloque le **skin évolution** + effet visuel spécial

---

## 🥚 Obtention des Pets

### Types d'Œufs

#### 1. Œuf Standard (§f Blanc)
- **Source**: Drop des zombies (0.5% par kill)
- **Contenu**: Pool complet (toutes raretés)
- **Garantie**: Aucune

#### 2. Œuf de Zone (§e Jaune)
- **Source**: Boss de zone, coffres rares
- **Contenu**: Pets thématiques de la zone
- **Garantie**: Rare minimum

#### 3. Œuf Élite (§d Violet)
- **Source**: Events dynamiques, Micro-events (rare)
- **Contenu**: Pool complet avec boost
- **Garantie**: Épique minimum (10% Légendaire)

#### 4. Œuf Légendaire (§6 Or)
- **Source**: Achat boutique (Points), Events spéciaux
- **Contenu**: Pool complet
- **Garantie**: Légendaire minimum (5% Mythique)

#### 5. Œuf Mythique (§c Rouge)
- **Source**: Accomplissements ultimes, Events saisonniers
- **Contenu**: Pool Mythique uniquement
- **Garantie**: Mythique garanti

### Système de Pity (Protection Anti-Malchance)

```
Œuf Standard:
- Après 50 œufs sans Rare+ → Rare garanti
- Après 100 œufs sans Épique+ → Épique garanti
- Après 200 œufs sans Légendaire → Légendaire garanti

Œuf Élite:
- Après 20 œufs sans Légendaire → Légendaire garanti

Le compteur se réinitialise après avoir obtenu la rareté concernée.
```

### Sources de Drop

| Source | Type d'Œuf | Chance |
|--------|-----------|--------|
| Zombie Normal | Standard | 0.3% |
| Zombie Élite | Standard | 2% |
| Mini-Boss | Zone | 15% |
| Boss de Zone | Zone | 100% |
| Micro-Event (Complet) | Standard/Élite | 5%/1% |
| Event Dynamique (Réussi) | Élite | 10% |
| Coffre de Zone | Standard | 20% |
| Coffre Rare | Zone | 50% |
| Shop (Points) | Variable | Achat |

---

## 🔄 Système d'Amélioration (Gacha)

### Mécanique de Fusion

Quand un joueur obtient un Pet qu'il possède déjà:
1. Le Pet reçoit **+1 copie** vers le prochain niveau
2. Le joueur reçoit des **Fragments de Pet** bonus
3. Animation de "fusion" satisfaisante

### Fragments de Pet

Monnaie secondaire pour les Pets:
- **Source**: Duplicatas, démantèlement, événements
- **Usage**: Acheter des copies spécifiques en boutique

#### Conversion Duplicatas → Fragments

| Rareté | Fragments par Duplicata |
|--------|------------------------|
| Commun | 5 |
| Peu Commun | 10 |
| Rare | 25 |
| Épique | 100 |
| Légendaire | 500 |
| Mythique | 2000 |

#### Coût en Fragments (Boutique Rotative)

| Rareté | Coût pour 1 Copie |
|--------|------------------|
| Commun | 50 |
| Peu Commun | 100 |
| Rare | 300 |
| Épique | 1000 |
| Légendaire | 5000 |
| Mythique | Non achetable |

### Système de Star Power (Niveau 9+)

Une fois niveau 9 atteint, les copies supplémentaires débloquent:
- **Star Power 1** (SP1): Amélioration majeure de la capacité active
- **Star Power 2** (SP2): Nouvelle capacité passive unique
- **Star Power 3** (SP3): Transformation visuelle ultime

Copies requises après niveau 9:
- SP1: +50% des copies totales requises
- SP2: +100% des copies totales requises
- SP3: +200% des copies totales requises

---

## 🐾 Liste des Pets

### COMMUNS (§7)

#### 1. Chauve-Souris Fantôme
- **Apparence**: Petite chauve-souris spectrale grise
- **Thème**: Détection / Vision
- **Capacité Passive**: Détecte les zombies dans un rayon de 15 blocs (particules)
- **Capacité Active**: "Écho-Scan" - Révèle tous les ennemis dans 30 blocs pendant 5s (CD: 30s)
- **Niveau 5 Bonus**: Détection des coffres cachés
- **Star Power**: L'écho-scan marque les ennemis, +10% dégâts sur eux

#### 2. Rat des Catacombes
- **Apparence**: Rat gris avec yeux brillants
- **Thème**: Loot / Économie
- **Capacité Passive**: +5% de chance de loot supplémentaire
- **Capacité Active**: "Fouille" - Cherche des ressources au sol, trouve 1-3 items (CD: 60s)
- **Niveau 5 Bonus**: +10% Points des zombies
- **Star Power**: Peut trouver des œufs de pet (très rare)

#### 3. Luciole Errante
- **Apparence**: Luciole lumineuse
- **Thème**: Lumière / Support
- **Capacité Passive**: Éclaire un rayon de 5 blocs autour du joueur
- **Capacité Active**: "Flash Aveuglant" - Aveugle les zombies proches 3s (CD: 25s)
- **Niveau 5 Bonus**: Rayon lumineux +5 blocs
- **Star Power**: Le flash inflige des dégâts aux morts-vivants

#### 4. Scarabée Blindé
- **Apparence**: Scarabée avec carapace métallique
- **Thème**: Défense
- **Capacité Passive**: +5% de réduction de dégâts
- **Capacité Active**: "Carapace" - Bouclier absorbant 20 dégâts (CD: 45s)
- **Niveau 5 Bonus**: Réduction +3%
- **Star Power**: Carapace reflète 30% des dégâts

#### 5. Corbeau Messager
- **Apparence**: Petit corbeau noir
- **Thème**: Mobilité / Communication
- **Capacité Passive**: +5% vitesse de déplacement
- **Capacité Active**: "Vol Éclaireur" - Le corbeau part en éclaireur et révèle une zone (CD: 40s)
- **Niveau 5 Bonus**: Vitesse +3%
- **Star Power**: Peut transporter un petit objet vers un autre joueur

---

### PEU COMMUNS (§a)

#### 6. Loup Spectral
- **Apparence**: Loup fantomatique vert translucide
- **Thème**: Combat / Meute
- **Capacité Passive**: Attaque les zombies proches (5 dégâts/2s)
- **Capacité Active**: "Hurlement" - Boost de 20% dégâts pendant 8s (CD: 35s)
- **Niveau 5 Bonus**: Attaque +3 dégâts
- **Star Power**: Invoque 2 loups spectraux temporaires (10s)

#### 7. Champignon Ambulant
- **Apparence**: Petit champignon avec pattes
- **Thème**: Soin / Régénération
- **Capacité Passive**: Régénère 0.5❤/5s au joueur
- **Capacité Active**: "Spore Curative" - Soigne 6❤ instantanément (CD: 40s)
- **Niveau 5 Bonus**: Régénération +0.25❤/5s
- **Star Power**: Spores laissent une zone de soin (3s)

#### 8. Golem de Poche
- **Apparence**: Mini golem de pierre
- **Thème**: Tank / Protection
- **Capacité Passive**: Intercepte 10% des dégâts subis par le joueur
- **Capacité Active**: "Mur de Pierre" - Crée un mur temporaire 3x2 (5s) (CD: 30s)
- **Niveau 5 Bonus**: Interception +5%
- **Star Power**: Le mur repousse les zombies

#### 9. Feu Follet
- **Apparence**: Flamme verte flottante
- **Thème**: Dégâts / Brûlure
- **Capacité Passive**: Les attaques du joueur ont 10% de chance d'enflammer
- **Capacité Active**: "Embrasement" - Enflamme tous les zombies dans 5 blocs (CD: 25s)
- **Niveau 5 Bonus**: Chance +5%
- **Star Power**: Les ennemis en feu prennent +25% dégâts

#### 10. Araignée Tisseuse
- **Apparence**: Araignée cyan luminescente
- **Thème**: Contrôle / Ralentissement
- **Capacité Passive**: Les zombies touchés sont ralentis 1s
- **Capacité Active**: "Toile Géante" - Piège les zombies dans une zone 5x5 (4s) (CD: 30s)
- **Niveau 5 Bonus**: Ralentissement +0.5s
- **Star Power**: La toile inflige des dégâts de poison

---

### RARES (§b)

#### 11. Phénix Mineur
- **Apparence**: Petit oiseau de feu
- **Thème**: Résurrection / Feu
- **Capacité Passive**: À la mort, renaissance avec 30% HP (CD: 5min)
- **Capacité Active**: "Nova de Feu" - Explosion de feu (15 dégâts, 5 blocs) (CD: 35s)
- **Niveau 5 Bonus**: Renaissance avec 40% HP
- **Star Power**: La renaissance crée une explosion de feu

#### 12. Serpent de Givre
- **Apparence**: Serpent de glace
- **Thème**: Glace / Contrôle
- **Capacité Passive**: +15% dégâts de glace (synergies avec items/classes)
- **Capacité Active**: "Souffle Glacial" - Gèle les ennemis devant (3s) (CD: 30s)
- **Niveau 5 Bonus**: Dégâts de glace +10%
- **Star Power**: Les ennemis gelés explosent en mourant

#### 13. Hibou Arcanique
- **Apparence**: Hibou avec runes brillantes
- **Thème**: Magie / Cooldowns
- **Capacité Passive**: -10% cooldown des capacités de classe
- **Capacité Active**: "Reset Arcanique" - Reset le cooldown d'une capacité (CD: 90s)
- **Niveau 5 Bonus**: Cooldown -5% supplémentaire
- **Star Power**: Réduit aussi les cooldowns des items actifs

#### 14. Essaim de Scarabées
- **Apparence**: Nuage de scarabées dorés
- **Thème**: DPS / Essaim
- **Capacité Passive**: Inflige 3 dégâts/s aux zombies proches (2 blocs)
- **Capacité Active**: "Nuée" - L'essaim attaque une cible (50 dégâts sur 5s) (CD: 25s)
- **Niveau 5 Bonus**: Dégâts passifs +2/s
- **Star Power**: La nuée se propage aux ennemis proches

#### 15. Spectre Gardien
- **Apparence**: Fantôme en armure
- **Thème**: Protection / Contre-attaque
- **Capacité Passive**: Pare automatiquement 1 attaque/30s
- **Capacité Active**: "Riposte Spectrale" - Prochaine attaque subie = contre-attaque x2 (CD: 20s)
- **Niveau 5 Bonus**: Parade toutes les 25s
- **Star Power**: La parade stun l'attaquant

---

### ÉPIQUES (§d)

#### 16. Dragon Pygmée
- **Apparence**: Mini dragon (taille d'un chat)
- **Thème**: Puissance / Multi-éléments
- **Capacité Passive**: +15% dégâts globaux
- **Capacité Active**: "Souffle Draconique" - Souffle de feu en cône (40 dégâts) (CD: 25s)
- **Niveau 5 Bonus**: Dégâts +10%
- **Star Power**: Alterne entre feu/glace/foudre (éléments aléatoires)

#### 17. Familier Nécromantique
- **Apparence**: Crâne flottant avec aura violette
- **Thème**: Nécromancie / Minions
- **Capacité Passive**: Les zombies tués ont 10% de chance de devenir alliés (15s)
- **Capacité Active**: "Résurrection" - Ressuscite le dernier zombie tué comme allié (30s) (CD: 45s)
- **Niveau 5 Bonus**: Chance +5%, durée +5s
- **Star Power**: Les alliés morts-vivants explosent en mourant

#### 18. Golem de Cristal
- **Apparence**: Golem fait de cristaux violets
- **Thème**: Tank Ultime / Sacrifice
- **Capacité Passive**: +25% HP max au joueur
- **Capacité Active**: "Sacrifice Cristallin" - Absorbe 100% des dégâts pendant 5s, puis explose (CD: 60s)
- **Niveau 5 Bonus**: HP +10%
- **Star Power**: L'explosion soigne le joueur

#### 19. Félin de l'Ombre
- **Apparence**: Chat noir avec yeux dorés, semi-transparent
- **Thème**: Assassinat / Critique
- **Capacité Passive**: +20% dégâts critiques
- **Capacité Active**: "Embuscade" - Prochaine attaque = critique garanti x3 (CD: 20s)
- **Niveau 5 Bonus**: Critique +10%
- **Star Power**: L'embuscade rend invisible 3s avant l'attaque

#### 20. Élémentaire Instable
- **Apparence**: Sphère d'énergie multicolore changeante
- **Thème**: Chaos / Aléatoire
- **Capacité Passive**: Effet aléatoire toutes les 30s (buff ou dégâts zone)
- **Capacité Active**: "Implosion Chaotique" - Effet puissant aléatoire (CD: 30s)
- **Niveau 5 Bonus**: Les effets positifs durent +50%
- **Star Power**: Peut déclencher plusieurs effets à la fois

---

### LÉGENDAIRES (§6)

#### 21. Gardien Angélique
- **Apparence**: Mini ange en armure dorée
- **Thème**: Protection Divine / Immunité
- **Capacité Passive**: Immunité aux effets négatifs (poison, wither, etc.)
- **Capacité Active**: "Bénédiction Divine" - Invincibilité 3s + full heal (CD: 120s)
- **Niveau 5 Bonus**: Immunité partagée aux alliés proches
- **Star Power**: La bénédiction affecte aussi les alliés dans 10 blocs

#### 22. Wyrm du Néant
- **Apparence**: Serpent cosmique avec étoiles dans le corps
- **Thème**: Espace / Téléportation
- **Capacité Passive**: Téléportation courte (5 blocs) en prenant des dégâts (CD: 10s)
- **Capacité Active**: "Portail du Néant" - Crée un portail vers un point visible (CD: 30s)
- **Niveau 5 Bonus**: Téléportation +3 blocs, CD -3s
- **Star Power**: Peut emmener les alliés dans le portail

#### 23. Titan Miniature
- **Apparence**: Géant humanoïde miniature (1 bloc de haut)
- **Thème**: Force Brute / Écrasement
- **Capacité Passive**: +30% dégâts de mêlée
- **Capacité Active**: "Coup Titanesque" - Frappe le sol (80 dégâts zone, knockback) (CD: 25s)
- **Niveau 5 Bonus**: Dégâts mêlée +15%
- **Star Power**: Le coup laisse une fissure qui inflige des dégâts continus

#### 24. Esprit de la Forêt
- **Apparence**: Dryade miniature avec feuilles et fleurs
- **Thème**: Nature / Régénération Ultime
- **Capacité Passive**: Régénération de 1❤/3s, +50% efficacité des soins reçus
- **Capacité Active**: "Sanctuaire Naturel" - Zone de soin massive (5❤/s pendant 10s) (CD: 60s)
- **Niveau 5 Bonus**: Régénération +0.5❤/3s
- **Star Power**: Le sanctuaire fait aussi repousser les morts-vivants

#### 25. Phénix Ancestral
- **Apparence**: Grand phénix doré majestueux
- **Thème**: Renaissance / Puissance de Feu
- **Capacité Passive**: Renaissance automatique une fois par vie (full HP)
- **Capacité Active**: "Apocalypse de Feu" - Pluie de feu (zone 10x10, 100 dégâts total) (CD: 45s)
- **Niveau 5 Bonus**: Renaissance donne 5s d'invincibilité
- **Star Power**: La renaissance déclenche automatiquement l'apocalypse

---

### MYTHIQUES (§c)

#### 26. Avatar de la Mort
- **Apparence**: Faucheuse miniature avec faux scintillante
- **Thème**: Exécution / Mortalité
- **Capacité Passive**: Les ennemis sous 15% HP sont exécutés instantanément
- **Capacité Active**: "Sentence Mortelle" - Marque une cible, elle meurt dans 5s (boss: -50% HP) (CD: 90s)
- **Niveau 5 Bonus**: Seuil d'exécution 20%
- **Star Power**: L'exécution soigne le joueur de 20% HP max

#### 27. Entité du Vide
- **Apparence**: Silhouette noire avec yeux blancs, distorsion visuelle
- **Thème**: Annihilation / Néant
- **Capacité Passive**: 5% des dégâts infligés ignorent toute résistance
- **Capacité Active**: "Dévoration" - Crée un trou noir aspirant les ennemis (5s) (CD: 60s)
- **Niveau 5 Bonus**: Dégâts purs +3%
- **Star Power**: Le trou noir désintègre les ennemis à faible HP

#### 28. Chroniqueur Temporel
- **Apparence**: Horloge vivante avec engrenages dorés
- **Thème**: Temps / Manipulation
- **Capacité Passive**: +25% vitesse d'attaque et de déplacement
- **Capacité Active**: "Arrêt du Temps" - Freeze tous les ennemis 4s (CD: 75s)
- **Niveau 5 Bonus**: Vitesse +10%
- **Star Power**: Pendant l'arrêt, le joueur inflige x2 dégâts

#### 29. Hydre Primordiale
- **Apparence**: Mini hydre à 3 têtes
- **Thème**: Multi-attaque / Régénération
- **Capacité Passive**: Chaque attaque frappe 3 fois (3 têtes)
- **Capacité Active**: "Souffle Tricolore" - 3 souffles simultanés (feu/glace/poison) (CD: 35s)
- **Niveau 5 Bonus**: Si une tête est "tuée" (gros dégâts), elle repousse en 2 (+dégâts)
- **Star Power**: Peut faire repousser jusqu'à 5 têtes temporairement

#### 30. Colossus Oublié
- **Apparence**: Fragment d'un ancien colosse de pierre avec runes brillantes
- **Thème**: Puissance Ancienne / Destruction
- **Capacité Passive**: +50% dégâts, mais -20% vitesse
- **Capacité Active**: "Éveil du Colosse" - Transformation géante (10s) - dégâts x3, immunité (CD: 120s)
- **Niveau 5 Bonus**: Malus de vitesse réduit à -10%
- **Star Power**: L'éveil génère des ondes de choc continues

---

## ⚡ Capacités & Mécaniques

### Types de Capacités

#### Capacité Passive
- Toujours active tant que le pet est équipé
- S'améliore avec le niveau du pet
- Niveau 5 débloque une passive secondaire

#### Capacité Active
- Déclenchée manuellement (touche configurable, défaut: R)
- A un cooldown
- S'améliore avec le niveau du pet

#### Star Powers (Niveau 9+)
- Modifications majeures des capacités
- Souvent game-changing
- Récompense l'investissement long-terme

### Synergie Pet + Classe

| Classe | Pets Recommandés | Synergie |
|--------|-----------------|----------|
| Berserker | Dragon Pygmée, Titan Miniature | Dégâts bruts |
| Occultiste | Familier Nécromantique, Avatar de la Mort | Minions + exécution |
| Tireur d'Élite | Félin de l'Ombre, Hibou Arcanique | Critiques + cooldowns |
| Ingénieur | Golem de Cristal, Scarabée Blindé | Tank + protection |
| Nécromancien | Familier Nécromantique, Spectre Gardien | Armée de morts |
| Pyromancien | Phénix (tous), Feu Follet | Synergies feu |
| Cryomancien | Serpent de Givre | Synergies glace |

### Mécaniques de Combat des Pets

```java
// Le pet suit le joueur à 2-3 blocs de distance
// Le pet ne peut pas mourir mais peut être "KO"
// Si le pet prend trop de dégâts (seuil basé sur HP joueur), il est KO 30s
// Le pet attaque automatiquement les cibles que le joueur attaque
// Le pet ne génère pas d'aggro (les zombies ciblent le joueur)
```

### États du Pet

1. **Actif**: Suit le joueur, capacités disponibles
2. **Combat**: Attaque activement une cible
3. **KO**: Temporairement indisponible (30s par défaut)
4. **Inactif**: Rangé dans l'inventaire virtuel

---

## 🖥️ Interface Utilisateur

### Menu Principal des Pets (/pet ou /pets)

```
╔══════════════════════════════════════════╗
║         🐾 MES COMPAGNONS 🐾              ║
╠══════════════════════════════════════════╣
║                                          ║
║  [Pet Actif: Dragon Pygmée ★★★]         ║
║  Niveau 7/9 | 156/200 copies             ║
║  ████████░░ 78%                          ║
║                                          ║
║  [📦 Collection] [🥚 Œufs] [⚙️ Options]  ║
║                                          ║
║  Fragments: 2,450 💎                     ║
║  Œufs disponibles: 3                     ║
║                                          ║
╚══════════════════════════════════════════╝
```

### Menu Collection

Grille 9x6 avec tous les pets:
- Pets possédés: Affichés en couleur avec niveau
- Pets non possédés: Silhouette grise avec "?"
- Clic = détails du pet

### Menu Détails d'un Pet

```
╔══════════════════════════════════════════╗
║      §d★ Dragon Pygmée ★ (Épique)        ║
╠══════════════════════════════════════════╣
║  [MODÈLE 3D ROTATIF DU PET]              ║
║                                          ║
║  Niveau: 7/9                             ║
║  Copies: 156/200 pour niveau 8           ║
║  ████████████████░░░░ 78%                ║
║                                          ║
║  ═══ CAPACITÉS ═══                       ║
║                                          ║
║  §7[Passif] +15% dégâts globaux          ║
║  §a[Passif Niv.5] +10% dégâts supplém.   ║
║                                          ║
║  §b[Actif] Souffle Draconique            ║
║  Souffle de feu en cône (40 dégâts)      ║
║  Cooldown: 25s                           ║
║                                          ║
║  §8[Star Power] 🔒 Niveau 9 requis       ║
║                                          ║
║  [ÉQUIPER]  [AMÉLIORER]  [RETOUR]        ║
╚══════════════════════════════════════════╝
```

### Menu Ouverture d'Œuf

Animation cinématique:
1. L'œuf apparaît au centre
2. L'œuf tremble et brille
3. Craquellement progressif
4. EXPLOSION de particules selon rareté
5. Révélation du pet avec fanfare
6. Affichage: Nouveau pet OU duplicata (+fragments)

### HUD En Jeu

```
Coin inférieur gauche:
┌─────────────────────┐
│ [Icône Pet] Niv.7   │
│ [Barre Cooldown]    │
│ Appuyez R pour actif│
└─────────────────────┘
```

### Notifications

- Nouveau pet: Toast notification + son spécial
- Level up: Animation + message chat
- Star Power débloqué: Annonce serveur + effets
- Pet KO: Avertissement + timer

---

## 💰 Économie & Équilibrage

### Prix Boutique (Points)

| Item | Prix (Points) |
|------|---------------|
| Œuf Standard | 500 |
| Œuf Standard x10 | 4,500 (10% réduction) |
| Œuf de Zone | 2,000 |
| Œuf Élite | 5,000 |
| Œuf Légendaire | 15,000 |
| Fragments x100 | 1,000 |

### Estimation de Progression

Pour maxer un pet (niveau 9, pas de Star Power):

| Rareté | Copies Requises | Œufs Moyens | Points Estimés |
|--------|----------------|-------------|----------------|
| Commun | 50 | ~111 | ~55,500 |
| Peu Commun | 100 | ~333 | ~166,500 |
| Rare | 200 | ~1,333 | ~666,500 |
| Épique | 400 | ~5,714 | ~2,857,000 |
| Légendaire | 800 | ~32,000 | ~16,000,000 |
| Mythique | 1500 | ~300,000 | Non réaliste (events) |

### Taux de Drop Ajustés par Zone

| Zone | Modificateur Drop Œuf | Type Œuf Bonus |
|------|----------------------|----------------|
| 1-10 | x1.0 | Standard uniquement |
| 11-20 | x1.2 | Zone débloqué |
| 21-30 | x1.4 | Élite rare (1%) |
| 31-40 | x1.6 | Élite (2%) |
| 41-50 | x1.8 | Élite (3%) |
| 51+ | x2.0 | Légendaire rare (0.5%) |

### Événements Spéciaux

#### Double Drop Weekend
- x2 chances de drop d'œufs
- x2 fragments des duplicatas

#### Pet Spotlight
- Un pet spécifique a +300% chance dans les œufs
- Dure 3 jours

#### Événement Mythique
- Œuf Mythique disponible en récompense
- Défis communautaires

---

## 🔧 Spécifications Techniques

### Architecture des Classes

```
com.rinaorc.zombiez.pets/
├── Pet.java                    // Entité Pet abstraite
├── PetType.java               // Enum de tous les pets
├── PetRarity.java             // Enum des raretés
├── PetData.java               // Données sauvegardées d'un pet
├── PetInstance.java           // Instance active d'un pet en jeu
├── PetManager.java            // Gestionnaire principal
├── PetAbility.java            // Interface des capacités
├── PetFollowAI.java           // IA de suivi du joueur
├── eggs/
│   ├── PetEgg.java            // Classe abstraite œuf
│   ├── StandardEgg.java
│   ├── ZoneEgg.java
│   ├── EliteEgg.java
│   ├── LegendaryEgg.java
│   └── MythicEgg.java
├── abilities/
│   ├── passive/               // Toutes les passives
│   └── active/                // Toutes les actives
├── gui/
│   ├── PetMainMenu.java
│   ├── PetCollectionMenu.java
│   ├── PetDetailsMenu.java
│   ├── PetEggOpeningMenu.java
│   └── PetShopMenu.java
├── commands/
│   └── PetCommand.java
└── listeners/
    ├── PetCombatListener.java
    └── PetInteractionListener.java
```

### Structure de Données (PlayerData)

```java
public class PlayerPetData {
    // Pets possédés: Map<PetType, PetData>
    private Map<PetType, PetData> ownedPets;

    // Pet actuellement équipé
    private PetType equippedPet;

    // Fragments
    private int petFragments;

    // Œufs en attente
    private List<PetEgg> pendingEggs;

    // Compteur Pity
    private Map<EggType, Integer> pityCounters;

    // Statistiques
    private int totalEggsOpened;
    private int legendariesObtained;
    private int mythicsObtained;
}

public class PetData {
    private PetType type;
    private int level;           // 1-9
    private int copies;          // Copies accumulées
    private int starPower;       // 0-3
    private boolean isFavorite;
    private long totalDamageDealt;
    private int timesUsed;
}
```

### Sauvegarde (YAML)

```yaml
players:
  uuid-exemple:
    pets:
      equipped: DRAGON_PYGMEE
      fragments: 2450
      pity:
        standard: 45
        elite: 12
      collection:
        CHAUVE_SOURIS:
          level: 5
          copies: 28
          star_power: 0
        DRAGON_PYGMEE:
          level: 7
          copies: 156
          star_power: 0
        AVATAR_MORT:
          level: 3
          copies: 8
          star_power: 0
      eggs:
        - type: STANDARD
          quantity: 2
        - type: ELITE
          quantity: 1
      stats:
        eggs_opened: 234
        legendaries: 3
        mythics: 1
```

### Commandes

```
/pet                    - Ouvre le menu principal
/pet equip <nom>        - Équipe un pet
/pet unequip            - Déséquipe le pet actuel
/pet list               - Liste tous les pets possédés
/pet info <nom>         - Infos sur un pet
/pet egg                - Ouvre un œuf (si disponible)
/pet fragments          - Affiche les fragments
/pet ability            - Active la capacité du pet (ou touche R)

/petadmin give <joueur> <pet> [niveau] [copies]
/petadmin giveegg <joueur> <type> [quantité]
/petadmin givefragments <joueur> <quantité>
/petadmin setlevel <joueur> <pet> <niveau>
/petadmin reset <joueur>
/petadmin spawnpet <pet>    - Spawn visuel pour tests
```

### Entité Pet (Visuel)

Options d'implémentation:
1. **ArmorStand invisible + tête custom** (simple)
2. **Mob existant avec AI custom** (ex: Bee, Parrot)
3. **Display Entity** (1.19.4+, recommandé)
4. **Citizens NPC** (si plugin installé)

Recommandation: **Display Entity** avec modèle custom via resource pack

### Performance

```java
// Le pet tick toutes les 5 ticks (pas chaque tick)
// La passive check toutes les 20 ticks
// L'attaque auto toutes les 10 ticks si cible valide
// Pathfinding simplifié: téléport si trop loin (>10 blocs)
// Limite: 1 pet par joueur, pas de pet vs pet
```

### Permissions

```yaml
# Joueur standard
zombiez.pet.use           # Utiliser son pet
zombiez.pet.collection    # Voir sa collection
zombiez.pet.egg           # Ouvrir des œufs

# VIP
zombiez.pet.vip.extrastorage    # +5 emplacements d'œufs
zombiez.pet.vip.fastopen        # Skip animation œuf

# Admin
zombiez.pet.admin         # Toutes commandes admin
```

---

## 📅 Roadmap d'Implémentation

### Phase 1: Core System (Semaine 1-2)
- [ ] Structure de données Pet
- [ ] PetManager basique
- [ ] Sauvegarde/chargement
- [ ] Commande /pet basique
- [ ] 5 premiers pets (1 par rareté jusqu'à Rare)

### Phase 2: Visuel & UI (Semaine 3-4)
- [ ] Entité pet suivant le joueur
- [ ] Menu collection
- [ ] Menu détails
- [ ] Système d'œufs basique
- [ ] Animation ouverture œuf

### Phase 3: Capacités (Semaine 5-6)
- [ ] Framework capacités passives
- [ ] Framework capacités actives
- [ ] Intégration combat
- [ ] 10 pets supplémentaires

### Phase 4: Gacha & Économie (Semaine 7-8)
- [ ] Système de copies/niveaux
- [ ] Fragments
- [ ] Boutique
- [ ] Pity system
- [ ] 10 pets supplémentaires

### Phase 5: Polish & Endgame (Semaine 9-10)
- [ ] Star Powers
- [ ] Tous les pets restants
- [ ] Événements pets
- [ ] Synergies classes
- [ ] Équilibrage final

---

## 📝 Notes de Design

### Philosophie d'Équilibrage
- Les pets Communs doivent rester utiles même endgame (niches)
- Les pets Mythiques sont puissants mais pas obligatoires
- La progression doit être satisfaisante à chaque étape
- Le hasard doit être tempéré par le système de Pity et Fragments

### Éviter le Pay-to-Win
- Tous les œufs obtenables en jeu
- Les Mythiques ne sont pas 10x plus forts, juste uniques
- Le skill du joueur reste primordial
- Pas de pet exclusif payant

### Feedback Loop
- Ouvrir un œuf = toujours satisfaisant (nouveau pet OU progression)
- Level up = récompense tangible (+stats visibles)
- Star Power = moment "wow" après investissement

---

*Document créé pour ZombieZ Plugin*
*Version 1.0 - Game Design Pets*
