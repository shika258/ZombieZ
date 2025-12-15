# 🔥 Système de Pouvoirs & Item Level - ZombieZ

## 📖 Vue d'ensemble

Le système de pouvoirs ajoute une couche de profondeur au loot procédural en introduisant :
- **Item Level (ILVL)** : Puissance réelle de l'objet
- **Pouvoirs** : Effets spéciaux qui se déclenchent en combat
- **Scaling dynamique** : Les pouvoirs deviennent plus puissants avec l'ILVL

### 🎮 Concept Principal

**RARETÉ ≠ PUISSANCE**

- La **rareté** définit le **potentiel** (nombre d'affixes, qualité des stats)
- L'**ILVL** définit la **puissance réelle** (dégâts, scaling des pouvoirs)

**Exemple concret :**
- Épée Légendaire ILVL 10 (early game) : Faible, mais avec du potentiel
- Épée Légendaire ILVL 85 (late game) : Très puissante !

---

## 🧱 Architecture Technique

### Classes Principales

#### 1. **ItemLevelManager** (`items/power/ItemLevelManager.java`)
- Gère le calcul de l'ILVL selon la zone et la rareté
- Plages d'ILVL configurables par rareté
- Facteurs de scaling pour les pouvoirs

#### 2. **PowerManager** (`items/power/PowerManager.java`)
- Registry de tous les pouvoirs disponibles
- Gestion des chances d'apparition par rareté
- Configuration globale du système

#### 3. **Power** (classe abstraite - `items/power/Power.java`)
- Base pour tous les pouvoirs
- Gestion des cooldowns
- Calcul des chances de proc
- Système de lore dynamique

#### 4. **PowerTriggerListener** (`items/power/PowerTriggerListener.java`)
- Écoute les événements de combat
- Déclenche les pouvoirs au bon moment
- Applique les pouvoirs sur les items

---

## ⚡ Pouvoirs Implémentés

### 1. Roulade Panda 🐼
**ID:** `panda_roll`

**Description :** Invoque un panda qui roule sur les ennemis et inflige des dégâts AOE.

**Caractéristiques :**
- **Proc Chance :** 15% par coup
- **Cooldown :** 12 secondes
- **Rareté minimum :** Rare
- **Scaling ILVL :**
  - Dégâts : `5.0 + (ILVL * 0.5)`
  - Rayon AOE : `3.0 + (ILVL * 0.02)` blocs
  - Durée : `5s + (ILVL / 20)s`

**Effets :**
- Dégâts AOE périodiques
- Knockback sur les ennemis
- Effets visuels et sonores

**Exemple scaling :**
- ILVL 10 : 10 dégâts, 3.2 blocs, 5.5s
- ILVL 50 : 30 dégâts, 4.0 blocs, 7.5s
- ILVL 100 : 55 dégâts, 5.0 blocs, 10s

---

### 2. Essaim d'Abeilles 🐝
**ID:** `bee_swarm`

**Description :** Invoque des abeilles agressives qui pourchassent les ennemis.

**Caractéristiques :**
- **Proc Chance :** 12% par coup
- **Cooldown :** 15 secondes
- **Rareté minimum :** Rare
- **Scaling ILVL :**
  - Dégâts/abeille : `2.0 + (ILVL * 0.2)`
  - Nombre d'abeilles : `3 + (ILVL * 0.04)`
  - Durée : `4s + (ILVL / 30)s`

**Effets :**
- Dégâts sur la durée
- Application de Poison I
- Ciblage automatique des ennemis
- Recherche intelligente de cibles

**Exemple scaling :**
- ILVL 10 : 4 dégâts/abeille, 3 abeilles, 4.3s
- ILVL 50 : 12 dégâts/abeille, 5 abeilles, 5.7s
- ILVL 100 : 22 dégâts/abeille, 7 abeilles, 7.3s

---

## 📊 Plages d'Item Level

| Rareté | ILVL Min | ILVL Max | % Chance Pouvoir |
|--------|----------|----------|------------------|
| Commun | 1 | 20 | 0% |
| Peu Commun | 10 | 35 | 5% |
| Rare | 15 | 40 | 15% |
| Épique | 35 | 70 | 35% |
| Légendaire | 50 | 100 | 60% |
| Mythique | 70 | 100 | 85% |
| Exalted | 85 | 100 | 100% |

---

## ⚙️ Configuration

### Fichier : `powers.yml`

```yaml
# Activer/désactiver le système
enabled: true

# Configuration Item Level
item-level:
  base-ilvl-per-zone: 10  # ILVL de base par zone
  max-ilvl: 100           # ILVL maximum

# Chances d'obtenir un pouvoir par rareté
power-chances:
  legendary: 0.60  # 60% de chance

# Configuration d'un pouvoir spécifique
powers:
  panda_roll:
    enabled: true
    proc-chance: 0.15
    cooldown: 12
    minimum-rarity: RARE
    base-damage: 5.0
    damage-per-ilvl: 0.5
```

---

## 🔧 Ajouter un Nouveau Pouvoir

### 1. Créer la classe du pouvoir

```java
package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;

public class MonNouveauPouvoir extends Power {

    public MonNouveauPouvoir() {
        super("mon_pouvoir", "Nom Affiché", "Description");
        this.baseProcChance = 0.1;
        this.cooldownMs = 10000;
        this.minimumRarity = Rarity.EPIC;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) return;
        applyCooldown(player);

        // Votre logique ici
        double damage = calculateDamage(itemLevel);
        // ...
    }

    private double calculateDamage(int itemLevel) {
        return 10.0 + (itemLevel * 0.5);
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts: §c" + calculateDamage(itemLevel));
        return stats;
    }
}
```

### 2. Enregistrer le pouvoir

Dans `PowerManager.registerDefaultPowers()` :

```java
registerPower(new MonNouveauPouvoir());
```

### 3. Ajouter la configuration

Dans `powers.yml` :

```yaml
powers:
  mon_pouvoir:
    enabled: true
    proc-chance: 0.1
    cooldown: 10
    minimum-rarity: EPIC
    # Vos paramètres custom
```

---

## 🎯 Game Design

### Progression

- **Zone 1-3 (ILVL 10-30)** : Early game, pouvoirs faibles mais amusants
- **Zone 4-7 (ILVL 30-60)** : Mid game, pouvoirs utiles
- **Zone 8-10 (ILVL 60-100)** : End game, pouvoirs très puissants

### Équilibrage

**Formule générale de scaling :**
```
valeur_finale = base + (ILVL * scaling_factor)
```

**Facteurs de scaling recommandés :**
- Dégâts : 0.3 - 0.6 par ILVL
- Durée : 0.3 - 1.0 ticks par ILVL
- Rayon : 0.01 - 0.03 blocs par ILVL

### UX Joueur

Le système doit être :
- ✅ **Simple à comprendre** : "Plus l'ILVL est élevé, plus c'est fort"
- ✅ **Visible** : ILVL affiché dans le lore
- ✅ **Gratifiant** : Effets visuels et sonores
- ✅ **Équilibré** : Ni trop fort, ni trop faible

---

## 🧪 Tests

### Test 1 : Génération d'item
```java
ItemGenerator gen = ItemGenerator.getInstance();
ZombieZItem item = gen.generate(5, Rarity.LEGENDARY, ItemType.SWORD, 0.0);

// Vérifier l'ILVL
System.out.println("ILVL: " + item.getItemLevel());

// Vérifier le pouvoir
System.out.println("Pouvoir: " + item.getPowerId());
```

### Test 2 : Trigger de pouvoir
1. Équiper une arme avec pouvoir
2. Frapper un zombie
3. Observer le déclenchement du pouvoir
4. Vérifier le cooldown

### Test 3 : Scaling
Générer le même pouvoir avec différents ILVL :
- ILVL 10 : Effets faibles
- ILVL 50 : Effets moyens
- ILVL 100 : Effets puissants

---

## 📈 Performances

### Optimisations

- **Cooldowns** : Map en mémoire, nettoyage périodique
- **Cache** : ItemManager cache les items générés
- **Async** : Les effets visuels tournent en async quand possible
- **Cleanup** : Les entités invoquées sont automatiquement supprimées

### Monitoring

```java
// Stats du système
String stats = powerManager.getStats();
// "Pouvoirs: 2 | Système: Activé | Joueurs avec cooldowns: 5"
```

---

## 🚀 Évolutions Futures

### Idées de Pouvoirs

1. **Lightning Strike** : Invoque la foudre
2. **Ice Nova** : Gèle les ennemis en AOE
3. **Blood Siphon** : Vole de la vie
4. **Chain Lightning** : Foudre rebondissante
5. **Meteor Shower** : Pluie de météores
6. **Shadow Clone** : Clone qui combat
7. **Phoenix Rebirth** : Résurrection automatique

### Améliorations Possibles

- [ ] Système de combos de pouvoirs
- [ ] Pouvoirs légendaires uniques
- [ ] Synergies entre pouvoirs
- [ ] Évolution de pouvoirs (upgrade)
- [ ] Statistiques de pouvoirs (tracking)

---

## 📝 Notes Importantes

### ILVL vs Rareté

⚠️ **NE PAS CONFONDRE !**
- Rareté = Potentiel, couleur, effets visuels
- ILVL = Puissance réelle

Un légendaire ILVL 10 est **moins puissant** qu'un épique ILVL 70.

### Compatibilité

Le système est entièrement **rétrocompatible** :
- Les anciens items sans ILVL reçoivent ILVL 1 par défaut
- Le système peut être désactivé dans la config
- Aucun impact sur les items existants

---

## 🤝 Contribution

Pour contribuer un nouveau pouvoir :

1. Créer une classe dans `items/power/impl/`
2. Étendre `Power`
3. Implémenter `trigger()` et `getPowerStats()`
4. Ajouter la configuration dans `powers.yml`
5. Tester avec différents ILVL
6. Documenter le scaling

---

**Système développé pour ZombieZ**
Version 1.0 - Décembre 2025
