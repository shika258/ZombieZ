# SYSTEME DE TALENTS - CAHIER DES CHARGES
## ZombieZ - Game Design Document

---

## SOMMAIRE

1. [Vue d'ensemble](#vue-densemble)
2. [Structure du systeme](#structure-du-systeme)
3. [Talents du Guerrier](#guerrier)
4. [Talents du Chasseur](#chasseur)
5. [Talents de l'Occultiste](#occultiste)
6. [Synergies et Builds](#synergies-et-builds)
7. [Specifications techniques](#specifications-techniques)

---

## VUE D'ENSEMBLE

### Philosophie
Le systeme de talents doit permettre au joueur de **personnaliser radicalement** son gameplay. Chaque talent est un **choix impactant** qui modifie la facon de jouer, pas juste des bonus de stats.

### Principes de design
- **Impact visible**: Chaque talent doit avoir un effet visuel et sonore distinct
- **Choix difficile**: Les 5 options a chaque palier doivent toutes etre attrayantes
- **Synergie**: Les talents doivent pouvoir se combiner pour creer des builds uniques
- **Dopamine**: Effets visuels satisfaisants, feedbacks sonores, sensations de puissance
- **Scalabilite**: Les talents scalent avec l'equipement du joueur

### Structure des paliers
| Palier | Niveau | Theme |
|--------|--------|-------|
| 1 | 0 | Fondation - Definit le style de base |
| 2 | 5 | Amplification - Renforce le style choisi |
| 3 | 10 | Specialisation - Premiere mechanique unique |
| 4 | 15 | Evolution - Combo et interactions |
| 5 | 20 | Maitrise - Mechanique avancee |
| 6 | 30 | Transcendance - Transformation du gameplay |
| 7 | 40 | Apex - Pouvoir majeur |
| 8 | 50 | Legendaire - Capacite ultime |

---

## STRUCTURE DU SYSTEME

### Interface
- **2 pages** dans le menu ClassInfoGUI
- Page 1: Paliers 1-4 (Niveaux 0, 5, 10, 15)
- Page 2: Paliers 5-8 (Niveaux 20, 30, 40, 50)
- Talents non debloques en gris avec niveau requis
- Talent selectionne en vert lumineux
- Preview des effets au survol

### Regles
- 1 seul talent selectionnable par palier
- Changement de talent possible (cooldown 1h ou cout en gemmes)
- Les talents sont des **PASSIFS** permanents
- Les effets proc sur les actions du joueur (attaque, kill, degats recus, etc.)

---

## GUERRIER
### Identite: Tank melee brutal, force brute, survie par l'agression

---

### PALIER 1 - NIVEAU 0 (Fondation)

#### 1.1 - FRAPPE SISMIQUE
> *Vos attaques ont 15% de chance de creer une onde de choc*

**Effet**: Lors d'une attaque, chance de creer un cercle de degats autour de vous
- Degats: 80% des degats de base
- Rayon: 3 blocs
- Animation: Cercle de particules de pierre qui s'expand + ecran shake leger
- Son: Impact sourd + craquement de sol

**Synergie visee**: AoE, Horde clear

---

#### 1.2 - SOIF DE SANG
> *Chaque kill vous soigne de 5% de vos PV max*

**Effet**: Heal instantane a chaque elimination
- Heal: 5% PV max
- Animation: Particules rouges qui remontent vers le joueur
- Son: Bruit de succion satisfaisant

**Synergie visee**: Sustain, Survie en horde

---

#### 1.3 - FUREUR CROISSANTE
> *Chaque coup porte augmente vos degats de 2% (max 20%, reset apres 3s sans toucher)*

**Effet**: Stack de fureur qui boost les degats
- Stack: +2% par hit, max 10 stacks
- Duration: Reset apres 3s sans attaquer
- Animation: Aura rouge qui s'intensifie avec les stacks
- Son: Grognement de rage qui monte

**Synergie visee**: DPS soutenu, Combat prolonge

---

#### 1.4 - PEAU DE FER
> *Les degats recus sont reduits de 15% mais vous etes 10% plus lent*

**Effet**: Trade-off survie vs mobilite
- Reduction: -15% degats recus
- Malus: -10% vitesse de deplacement
- Animation: Aspect metallique leger sur l'armure
- Son: Cliquetis metallique en marchant

**Synergie visee**: Tank pur, Position statique

---

#### 1.5 - CHARGE DEVASTATRICE
> *Sprinter pendant 1.5s puis frapper inflige 200% de degats et etourdit*

**Effet**: Bonus de degats apres sprint
- Condition: Sprinter 1.5s minimum
- Bonus: +200% degats sur la prochaine attaque
- Effet: Stun 0.5s sur la cible
- Animation: Trainee de poussiere pendant le sprint, explosion a l'impact
- Son: Charge de taureau + impact devastateur
- Cooldown interne: 5s

**Synergie visee**: Engagement, Burst damage

---

### PALIER 2 - NIVEAU 5 (Amplification)

#### 2.1 - ECHO DE GUERRE
> *Vos AoE ont 30% de chance de se repeter une seconde fois*

**Effet**: Double proc des effets de zone
- Chance: 30%
- Delay: 0.3s entre les deux
- Animation: Onde fantome qui suit la premiere
- Son: Echo du son original

**Synergie visee**: Combo avec Frappe Sismique, AoE build

---

#### 2.2 - FRENETIQUE
> *Sous 40% PV, vitesse d'attaque +40% et vol de vie +10%*

**Effet**: Mode berserk a low HP
- Seuil: 40% PV
- Bonus: +40% attack speed, +10% lifesteal
- Animation: Yeux rouges, aura de rage
- Son: Battements de coeur rapides

**Synergie visee**: Risk/Reward, Combo avec Soif de Sang

---

#### 2.3 - MASSE D'ARMES
> *Les coups critiques projettent les ennemis en arriere*

**Effet**: Knockback sur les crits
- Distance: 3 blocs
- Animation: Impact violent avec onde de choc
- Son: Coup de masse lourd

**Synergie visee**: Controle de foule, Kiting melee

---

#### 2.4 - BASTION
> *Bloquer une attaque (bouclier ou parade) vous donne un bouclier temporaire*

**Effet**: Contre-attaque defensive
- Bouclier: 20% PV max pendant 3s
- Cooldown: 8s
- Animation: Dome dore autour du joueur
- Son: Cloche de protection

**Synergie visee**: Tank, Combo avec Peau de Fer

---

#### 2.5 - DECHAÎNEMENT
> *Tuer 3 ennemis en 5 secondes declenche une explosion autour de vous*

**Effet**: Reward pour les multi-kills
- Degats: 150% degats de base
- Rayon: 4 blocs
- Animation: Explosion de sang et de rage
- Son: Rugissement + explosion

**Synergie visee**: Horde clear, Combo avec Fureur Croissante

---

### PALIER 3 - NIVEAU 10 (Specialisation)

#### 3.1 - TOURBILLON DE LAMES
> *25% de chance sur attaque de faire tournoyer votre arme, frappant tout autour*

**Effet**: Spin attack automatique
- Degats: 120% degats de base a tous les ennemis dans 2.5 blocs
- Animation: Rotation rapide de l'arme avec trainee visuelle
- Son: Sifflement de lame

**Synergie visee**: Core du build AoE

---

#### 3.2 - VAMPIRE DE GUERRE
> *10% des degats infliges sont convertis en PV. Double sous 30% HP*

**Effet**: Lifesteal passif ameliore
- Lifesteal base: 10%
- Lifesteal boost: 20% sous 30% HP
- Animation: Filets de sang vers le joueur
- Son: Drain vital

**Synergie visee**: Core du build Sustain

---

#### 3.3 - COLERE DES ANCETRES
> *Apres avoir recu des degats, votre prochaine attaque dans 2s inflige +100% degats*

**Effet**: Riposte puissante
- Window: 2s apres avoir ete touche
- Bonus: +100% degats
- Animation: Arme qui brille de rage
- Son: Cri de vengeance

**Synergie visee**: Core du build Riposte

---

#### 3.4 - TITAN IMMUABLE
> *Immunite au knockback et aux stuns. -20% degats recus quand immobile*

**Effet**: Tank indeplacable
- Permanent: Pas de knockback ni stun
- Bonus immobile: -20% degats si pas bouge depuis 1s
- Animation: Ancrage au sol visible
- Son: Pas lourd, impact de titan

**Synergie visee**: Core du build Tank statique

---

#### 3.5 - EXECUTEUR
> *Les ennemis sous 25% HP prennent 50% de degats supplementaires de vous*

**Effet**: Finisher brutal
- Seuil: 25% HP ennemi
- Bonus: +50% degats
- Animation: Marque rouge sur les cibles basses
- Son: Battement sourd quand l'ennemi est executable

**Synergie visee**: Core du build Burst/Execute

---

### PALIER 4 - NIVEAU 15 (Evolution)

#### 4.1 - RESONANCE SISMIQUE
> *Vos AoE laissent une zone de fracture pendant 3s qui inflige des degats*

**Effet**: Persistent AoE zone
- Degats: 30% degats de base par seconde
- Duree: 3s
- Animation: Sol craquele avec particules de pierre
- Son: Grondement sourd continu

**Synergie visee**: Zone control, Combo AoE

---

#### 4.2 - FRISSON DU COMBAT
> *Chaque point de vie vole genere 0.5s de vitesse d'attaque bonus (+50%)*

**Effet**: Attack speed sur lifesteal
- Duree stack: 0.5s par HP vole (cap 3s)
- Bonus: +50% attack speed pendant la duree
- Animation: Eclairs rouges sur les bras
- Son: Acceleration cardiaque

**Synergie visee**: Combo Vampire + Frenetique

---

#### 4.3 - VENGEANCE ARDENTE
> *Apres une riposte, les 3 prochaines attaques brulent les ennemis*

**Effet**: DoT sur riposte
- Degats burn: 40% degats de base sur 2s
- Stack: 3 attaques enflammees apres riposte
- Animation: Arme enflammee
- Son: Crackling fire

**Synergie visee**: Combo avec Colere des Ancetres

---

#### 4.4 - FORTERESSE
> *Quand votre bouclier temporaire expire, il explose pour des degats*

**Effet**: Bouclier offensif
- Degats explosion: 200% du bouclier restant
- Rayon: 3 blocs
- Animation: Explosion doree
- Son: Shatter de verre divin

**Synergie visee**: Combo avec Bastion

---

#### 4.5 - MOISSON SANGLANTE
> *Les executions restaurent 15% de vos PV max et reset le cooldown de sprint*

**Effet**: Reward d'execution
- Heal: 15% PV max sur kill d'ennemi <25% HP
- Bonus: Reset du sprint
- Animation: Explosion de sang satisfaisante
- Son: Coup final epique

**Synergie visee**: Combo avec Executeur + Charge Devastatrice

---

### PALIER 5 - NIVEAU 20 (Maitrise)

#### 5.1 - CATACLYSME
> *Toutes les 10 attaques, declenchez automatiquement une mega AoE*

**Effet**: Guaranteed big AoE
- Compteur: 10 attaques
- Degats: 250% degats de base
- Rayon: 5 blocs
- Animation: Enorme explosion de terre et de feu
- Son: Impact cataclysmique

**Synergie visee**: Build AoE ultime

---

#### 5.2 - IMMORTEL
> *Une fois par minute, si vous devez mourir, restez a 1 PV et devenez invincible 2s*

**Effet**: Cheat death
- Cooldown: 60s
- Duree invincibilite: 2s
- Animation: Aura doree, temps ralenti momentanement
- Son: Heartbeat puis surge de puissance

**Synergie visee**: Survie ultime, Combo Low HP builds

---

#### 5.3 - CYCLONE DE RAGE
> *Activer sprint vous fait tournoyer, infligeant des degats continus*

**Effet**: Sprint = Spin to win
- Degats: 60% degats de base par 0.5s aux ennemis proches
- Rayon: 2 blocs
- Animation: Tornade de lames
- Son: Vent violent + metal

**Synergie visee**: Mobilite offensive

---

#### 5.4 - AEGIS ETERNAL
> *Chaque bloc parfait (timing serré) reflete 100% des degats*

**Effet**: Parry perfect
- Window: 0.3s avant l'impact
- Reflect: 100% degats renvoyes
- Animation: Flash dore, projectile qui repart
- Son: Clang parfait + impact renvoye

**Synergie visee**: Skill expression, Tank actif

---

#### 5.5 - SEIGNEUR DE GUERRE
> *Les kills d'execution rendent votre prochaine attaque instakill si elle aussi execute*

**Effet**: Chain execute
- Condition: Kill un ennemi <25% HP
- Effet: La prochaine attaque sur un ennemi <25% HP est instakill
- Cooldown: Reset a chaque chain kill
- Animation: Trainee de mort
- Son: Execution en chaine

**Synergie visee**: Execute build ultime

---

### PALIER 6 - NIVEAU 30 (Transcendance)

#### 6.1 - TREMOR ETERNAL
> *Vous generez des ondes sismiques passives toutes les 2s en combat*

**Effet**: Passive AoE pulse
- Degats: 50% degats de base
- Rayon: 3 blocs
- Interval: 2s
- Animation: Ondes concentriques au sol
- Son: Pulse sismique

**Synergie visee**: AoE passive, Horde clear

---

#### 6.2 - AVATAR DE SANG
> *A max stacks de lifesteal (100 HP voles), explosez pour des degats massifs*

**Effet**: Blood bomb mechanic
- Condition: Voler 100 HP (compteur)
- Degats: 400% degats de base
- Rayon: 4 blocs
- Self-heal: 30% PV max
- Animation: Explosion de sang epique
- Son: Pulse cardiaque puis explosion

**Synergie visee**: Vampire ultime

---

#### 6.3 - REPRESAILLES INFINIES
> *Chaque fois que vous ripostez, les degats bonus de riposte augmentent de 25% (max 200%)*

**Effet**: Riposte scaling
- Stack: +25% degats de riposte par riposte reussie
- Max: +200% (base 100% + 200% = 300% total)
- Reset: Si vous ne ripostez pas pendant 10s
- Animation: Aura de rage croissante
- Son: Montee en puissance

**Synergie visee**: Riposte god

---

#### 6.4 - BASTILLE IMPRENABLE
> *Votre reduction de degats est doublee, et vous regenerez 2% HP/s en combat*

**Effet**: Ultimate tank
- Bonus: x2 damage reduction
- Regen: 2% HP/s en combat
- Animation: Aura de pierre
- Son: Heartbeat stable et puissant

**Synergie visee**: Immortalite passive

---

#### 6.5 - FAUCHEUR
> *Les ennemis que vous avez endommages sous 15% HP meurent automatiquement*

**Effet**: Auto-execute threshold
- Seuil: 15% HP
- Condition: Vous devez avoir fait des degats a l'ennemi
- Animation: Marque de mort qui se complete
- Son: Glas de la mort

**Synergie visee**: Execute passif

---

### PALIER 7 - NIVEAU 40 (Apex)

#### 7.1 - APOCALYPSE TERRESTRE
> *Vos AoE ont 10% de chance de declencher un seisme geant*

**Effet**: Mega AoE proc
- Chance: 10%
- Degats: 500% degats de base
- Rayon: 8 blocs
- Stun: 1s sur tous les ennemis touches
- Animation: Le sol se fissure, eruption de terre
- Son: Tremblement de terre devastateur

**Synergie visee**: AoE legendary

---

#### 7.2 - SEIGNEUR VAMPIRE
> *Le lifesteal peut depasser vos PV max, creant un bouclier de sang*

**Effet**: Overheal shield
- Overheal: Jusqu'a 50% PV max en bouclier
- Decay: -5% par seconde hors combat
- Animation: Aura de sang tourbillonnante
- Son: Pouls demoniaque

**Synergie visee**: Sustain legendary

---

#### 7.3 - NEMESIS
> *Chaque ennemi qui vous touche prend des degats de riposte automatiques*

**Effet**: Passive thorns extreme
- Degats: 75% des degats recus renvoyes
- Animation: Eclairs de riposte
- Son: Impact reflexif

**Synergie visee**: Tank offensif legendary

---

#### 7.4 - COLOSSE
> *Taille +20%, PV max +50%, degats de melee +30%, mais vitesse -25%*

**Effet**: Giant form permanent
- Taille: +20% (visuel)
- PV: +50%
- Degats melee: +30%
- Vitesse: -25%
- Animation: Apparence de geant
- Son: Pas lourds, voix plus grave

**Synergie visee**: Tank absolu

---

#### 7.5 - ANGE DE LA MORT
> *Les ennemis sous 30% HP ont 5% de chance de mourir instantanement a chaque tick*

**Effet**: Death aura
- Rayon: 5 blocs
- Tick: Chaque 0.5s
- Chance: 5% instakill
- Animation: Aura sombre, faucheur spectral
- Son: Murmures de mort

**Synergie visee**: Execute legendary passive

---

### PALIER 8 - NIVEAU 50 (Legendaire)

#### 8.1 - RAGNAROK
> *Toutes les 30s, declenchez automatiquement une apocalypse sismique massive*

**Effet**: Auto mega nuke
- Cooldown: 30s
- Degats: 800% degats de base
- Rayon: 12 blocs
- Effet: Stun 2s + knockback
- Animation: Fin du monde locale
- Son: Ragnarok horn + impact planetaire

**Synergie visee**: Ultimate AoE fantasy

---

#### 8.2 - DIEU DU SANG
> *Vous ne pouvez pas descendre sous 1 PV tant que vous infligez des degats*

**Effet**: Can't die while fighting
- Condition: Avoir fait des degats dans les 2 dernieres secondes
- Animation: Corps ecarlate, yeux rouges sang
- Son: Battements de coeur infinis

**Synergie visee**: Ultimate vampire fantasy

---

#### 8.3 - AVATAR DE VENGEANCE
> *Les degats accumules sur vous (cap 500% de vos PV max) peuvent etre liberes en une attaque*

**Effet**: Damage stored release
- Stockage: Tous les degats recus (cap 500% PV max)
- Release: Attaque speciale (crouch + attack)
- Degats: 100% du stocke
- Rayon: 6 blocs
- Animation: Explosion de rage pure
- Son: Liberation cataclysmique

**Synergie visee**: Ultimate riposte fantasy

---

#### 8.4 - CITADELLE VIVANTE
> *Vous etes immunise aux degats mais ne pouvez pas attaquer pendant 3s, puis explosion*

**Effet**: Charge and release
- Phase 1: 3s d'invincibilite totale (pas d'attaque possible)
- Phase 2: Explosion massive (300% degats base, 5 blocs)
- Cooldown: 20s
- Animation: Cocon de pierre puis explosion
- Son: Charge d'energie puis release

**Synergie visee**: Ultimate tank fantasy

---

#### 8.5 - EXTINCTION
> *Votre premiere attaque de chaque combat est un instakill garanti*

**Effet**: First hit = death
- Condition: Premier ennemi touche apres 10s sans combat
- Effet: Instakill (meme les elites, bosses = 30% HP direct)
- Animation: Frappe divine, eclair
- Son: Tonnerre divin

**Synergie visee**: Ultimate execute fantasy

---

## CHASSEUR
### Identite: DPS a distance, critiques devastateurs, mobilite et precision

---

### PALIER 1 - NIVEAU 0 (Fondation)

#### 1.1 - TIRS MULTIPLES
> *20% de chance de tirer 2 projectiles supplementaires*

**Effet**: Multi-shot proc
- Chance: 20%
- Projectiles bonus: 2 (en cone)
- Degats bonus: 60% chacun
- Animation: Triple trait de fleches
- Son: Triple whoosh

**Synergie visee**: Spam, AoE ranged

---

#### 1.2 - OEIL DE LYNX
> *+25% de chance de critique et les crits font +30% de degats*

**Effet**: Crit boost passif
- Crit chance: +25%
- Crit damage: +30%
- Animation: Oeil qui brille sur crit
- Son: Impact precis

**Synergie visee**: Crit build core

---

#### 1.3 - CHASSEUR AGILE
> *Esquiver (double tap direction) vous rend invisible 1s et boost les degats suivants +50%*

**Effet**: Tactical dodge
- Cooldown dodge: 3s
- Invisibilite: 1s
- Bonus degats: +50% sur prochaine attaque
- Animation: Blur + disparition
- Son: Whoosh furtif

**Synergie visee**: Hit and run

---

#### 1.4 - MARQUE DU CHASSEUR
> *Vos attaques marquent les ennemis. Les ennemis marques prennent +15% de degats*

**Effet**: Damage amp debuff
- Duree marque: 5s
- Bonus degats: +15% de toutes sources
- Animation: Symbole de cible sur l'ennemi
- Son: Lock-on beep

**Synergie visee**: DPS amplifie

---

#### 1.5 - FLECHES PERCANTES
> *Vos projectiles traversent le premier ennemi et touchent celui derriere*

**Effet**: Pierce
- Pierce: 1 ennemi
- Degats second: 80%
- Animation: Fleche qui traverse
- Son: Double impact

**Synergie visee**: Line clear, Double target

---

### PALIER 2 - NIVEAU 5 (Amplification)

#### 2.1 - RAFALE
> *Apres 3 tirs consecutifs sur la meme cible, le 4eme fait +100% degats*

**Effet**: Combo shot
- Condition: 3 hits sur meme cible
- Bonus 4eme: +100% degats
- Animation: 4eme tir brille
- Son: Charged shot impact

**Synergie visee**: Single target DPS

---

#### 2.2 - SNIPER
> *Plus la cible est loin, plus les degats sont eleves (max +50% a 15+ blocs)*

**Effet**: Distance scaling
- Scaling: +3.33% par bloc au-dela de 5
- Max: +50% a 15+ blocs
- Animation: Trail lumineux sur long shots
- Son: Whistle distant

**Synergie visee**: Kiting, Long range

---

#### 2.3 - FANTOME
> *Rester invisible 3s+ confere +100% de degats critique sur la sortie*

**Effet**: Ambush bonus
- Condition: 3s+ invisibilite
- Bonus: +100% crit damage sur premiere attaque
- Animation: Apparition spectrale
- Son: Assassin strike

**Synergie visee**: Burst, Combo avec Chasseur Agile

---

#### 2.4 - VENIN
> *Vos attaques empoisonnent, infligeant 40% degats bonus sur 3s*

**Effet**: Poison DoT
- Degats: 40% degats de base sur 3s
- Stack: Oui (nouvelles attaques refresh + stack)
- Animation: Effet vert poison
- Son: Drip toxique

**Synergie visee**: DoT build

---

#### 2.5 - RICOCHET
> *Les projectiles qui touchent ont 25% de chance de rebondir sur un ennemi proche*

**Effet**: Bouncing shots
- Chance: 25%
- Degats ricochet: 70%
- Range ricochet: 5 blocs
- Animation: Projectile qui change de direction
- Son: Ting metalique

**Synergie visee**: Multi-target

---

### PALIER 3 - NIVEAU 10 (Specialisation)

#### 3.1 - PLUIE DE FLECHES
> *25% de chance que votre tir invoque une pluie de fleches sur la zone*

**Effet**: Arrow rain proc
- Chance: 25%
- Degats: 30% par fleche (8 fleches)
- Zone: 4x4 blocs
- Animation: Ciel qui s'assombrit, pluie mortelle
- Son: Sifflement multiple puis impacts

**Synergie visee**: AoE core

---

#### 3.2 - OEIL DU PREDATEUR
> *Les critiques ont 30% de chance de reset votre cooldown d'esquive*

**Effet**: Crit = mobility
- Chance: 30% sur crit
- Effet: Reset dodge cooldown
- Animation: Flash bleu
- Son: Refresh sound

**Synergie visee**: Hyper mobilite

---

#### 3.3 - TRAQUEUR
> *Les ennemis marques sont visibles a travers les murs et prennent +25% degats (up de +15%)*

**Effet**: Wallhack + damage boost
- Vision: A travers obstacles
- Bonus: +25% degats (au lieu de 15%)
- Animation: Contour rouge visible
- Son: Heartbeat tracking

**Synergie visee**: Single target hunter

---

#### 3.4 - TOXINES MORTELLES
> *Le poison peut crit et les ennemis empoisonnes ont -30% de vitesse*

**Effet**: Poison upgrade
- Poison peut crit: Oui
- Slow: -30% vitesse ennemie
- Animation: Poison plus sombre
- Son: Bubbling toxique

**Synergie visee**: DoT + Control core

---

#### 3.5 - TIREUR D'ELITE
> *Rester immobile 1.5s garantit un critique sur le prochain tir*

**Effet**: Guaranteed crit from stealth
- Condition: 1.5s sans bouger
- Effet: Prochain tir = crit garanti
- Animation: Scope lock-on
- Son: Steady aim charge

**Synergie visee**: Burst core

---

### PALIER 4 - NIVEAU 15 (Evolution)

#### 4.1 - DELUGE
> *La pluie de fleches dure 2s de plus et les fleches percent*

**Effet**: Arrow rain upgrade
- Duree: +2s (total ~3s)
- Pierce: Les fleches de pluie traversent
- Animation: Pluie plus dense
- Son: Storm prolonge

**Synergie visee**: AoE evolution

---

#### 4.2 - PREDATEUR SUPREME
> *Les kills pendant l'invisibilite prolongent l'invisibilite de 2s*

**Effet**: Stealth chain kills
- Extension: +2s par kill
- Cap: 10s max
- Animation: Deviens plus transparent
- Son: Silence croissant

**Synergie visee**: Stealth assassin

---

#### 4.3 - SENTENCE DE MORT
> *Les ennemis marques qui meurent font exploser leur marque, touchant les ennemis proches*

**Effet**: Mark explosion on death
- Degats: 100% des degats du killing blow
- Rayon: 3 blocs
- Animation: Explosion de la marque
- Son: Boom satisfaisant

**Synergie visee**: Chain kill potential

---

#### 4.4 - PANDEMIE
> *Quand un ennemi empoisonne meurt, le poison se propage aux ennemis proches*

**Effet**: Spreading poison
- Range: 4 blocs
- Heritage: Meme stacks de poison
- Animation: Nuage toxique
- Son: Contamination spread

**Synergie visee**: DoT area control

---

#### 4.5 - SURCHAUFFE
> *Chaque tir augmente les degats de 5% mais aussi le recul. Reset apres 2s*

**Effet**: Ramping damage with recoil
- Stack: +5% degats par tir (max +50%)
- Malus: Recul croissant (harder to aim)
- Reset: 2s sans tirer
- Animation: Arme qui chauffe/brille
- Son: Whine montant

**Synergie visee**: Risk/reward DPS

---

### PALIER 5 - NIVEAU 20 (Maitrise)

#### 5.1 - TEMPETE D'ACIER
> *Toutes les 15s, declenchez automatiquement une mega pluie de fleches*

**Effet**: Auto arrow storm
- Cooldown: 15s
- Degats: 50% par fleche (20 fleches)
- Zone: 8x8 blocs
- Animation: Tempete epique
- Son: Apocalyptic rain

**Synergie visee**: AoE mastery

---

#### 5.2 - SPECTRE
> *Vous pouvez attaquer pendant l'invisibilite sans la briser (3 attaques max)*

**Effet**: Attack from stealth
- Attaques: 3 avant de redevenir visible
- Bonus maintenu: Les bonus de stealth restent
- Animation: Attaques fantomatiques
- Son: Silent strikes

**Synergie visee**: Ultimate stealth assassin

---

#### 5.3 - CHASSEUR DE PRIMES
> *Tuer un ennemi marque vous soigne de 10% PV et donne +20% degats pendant 5s*

**Effet**: Bounty reward
- Heal: 10% PV max
- Buff: +20% degats 5s
- Animation: Gold coins effect
- Son: Bounty collected

**Synergie visee**: Hunt and reward

---

#### 5.4 - EPIDEMIE
> *Le poison stack indefiniment et les ennemis a 10+ stacks prennent des degats x2*

**Effet**: Infinite poison scaling
- Stack: Pas de limite
- Threshold: 10 stacks = x2 poison damage
- Animation: Ennemi completement vert
- Son: Bubbling intensifie

**Synergie visee**: Ultimate DoT

---

#### 5.5 - ZONE DE MORT
> *Creez une zone ou votre vitesse de tir est +100% (zone suit votre position)*

**Effet**: Personal kill zone
- Bonus: +100% attack speed
- Rayon: 4 blocs autour de vous
- Animation: Cercle rouge au sol
- Son: Rapid fire ambiance

**Synergie visee**: Stationary DPS god

---

### PALIER 6 - NIVEAU 30 (Transcendance)

#### 6.1 - ARMAGEDDON AERIEN
> *La pluie de fleches peut crit et les crits font spawn des fleches bonus*

**Effet**: Crit arrow rain
- Crit: Les fleches de pluie peuvent crit
- Bonus: Crit = 2 fleches bonus
- Animation: Fleches dorées sur crit
- Son: Epic impacts

**Synergie visee**: AoE + Crit combo

---

#### 6.2 - MAITRE DES OMBRES
> *L'invisibilite est permanente tant que vous ne prenez pas de degats*

**Effet**: Permanent stealth
- Condition: Pas de degats recus
- Attaques: Ne brisent plus l'invisibilite
- Animation: Ombre constante
- Son: Ambient whisper

**Synergie visee**: Ultimate stealth

---

#### 6.3 - EXECUTEUR DE PRIMES
> *Les ennemis marques sous 20% HP meurent instantanement*

**Effet**: Mark execute
- Seuil: 20% HP
- Condition: Etre marque par vous
- Animation: Marque qui se complete et tue
- Son: Contract fulfilled

**Synergie visee**: Ultimate hunter

---

#### 6.4 - PESTE NOIRE
> *Le poison reduit les soins recus par l'ennemi de 75% et vous soigne*

**Effet**: Anti-heal + self heal
- Reduction soins ennemi: -75%
- Self heal: 5% des degats de poison
- Animation: Aura de maladie
- Son: Plague bells

**Synergie visee**: Ultimate DoT control

---

#### 6.5 - GATLING
> *Apres 20 tirs consecutifs, passez en mode gatling (tir auto, +200% attack speed) pendant 5s*

**Effet**: Full auto mode
- Activation: 20 tirs consecutifs
- Duree: 5s
- Bonus: +200% attack speed, tir automatique
- Animation: Transformation de l'arme
- Son: Minigun spin up

**Synergie visee**: Ultimate sustained DPS

---

### PALIER 7 - NIVEAU 40 (Apex)

#### 7.1 - METEOR SHOWER
> *La pluie de fleches est remplacee par une pluie de meteores explosifs*

**Effet**: Arrows become meteors
- Degats: 150% par meteor (10 meteores)
- Rayon explosion: 2 blocs chacun
- Zone: 10x10 blocs
- Animation: Meteores enflammés
- Son: Apocalyptic impacts

**Synergie visee**: Ultimate AoE legendary

---

#### 7.2 - REAPER
> *Vos attaques depuis l'invisibilite instakill les ennemis sous 30% HP*

**Effet**: Stealth execute
- Condition: Attaque depuis stealth
- Seuil: 30% HP = instakill
- Animation: Faux de la mort
- Son: Death whisper

**Synergie visee**: Ultimate assassin legendary

---

#### 7.3 - CHASSEUR LEGENDAIRE
> *Vous pouvez marquer 5 ennemis simultanement et les marques durent indefiniment*

**Effet**: Multi-mark permanent
- Marques: 5 simultanees
- Duree: Infinie (jusqu'a la mort)
- Animation: 5 marques distinctes
- Son: Multi lock-on

**Synergie visee**: Ultimate bounty hunter

---

#### 7.4 - BLIGHT
> *Le poison peut se propager meme aux ennemis a pleine vie proches d'un infecte*

**Effet**: Passive contagion
- Range: 3 blocs
- Tick: Check toutes les 2s
- Animation: Nuage miasmatique
- Son: Disease spread

**Synergie visee**: Ultimate plague

---

#### 7.5 - ARSENAL VIVANT
> *Vous tirez automatiquement sur l'ennemi le plus proche toutes les 0.5s*

**Effet**: Auto-targeting shots
- Rate: 1 tir / 0.5s
- Degats: 80% degats de base
- Range: 10 blocs
- Animation: Tirs automatiques
- Son: Steady shots

**Synergie visee**: Passive DPS legendary

---

### PALIER 8 - NIVEAU 50 (Legendaire)

#### 8.1 - ORBITAL STRIKE
> *Toutes les 45s, un bombardement orbital devastateur frappe la zone*

**Effet**: Nuke from orbit
- Cooldown: 45s
- Degats: 1000% degats de base
- Zone: 15 blocs de rayon
- Animation: Laser du ciel, explosion massive
- Son: Orbital cannon charge then impact

**Synergie visee**: Ultimate AoE fantasy

---

#### 8.2 - VOID WALKER
> *Vous existez dans les ombres - immunite aux degats tant que vous etes en mouvement*

**Effet**: Moving = invincible
- Condition: En deplacement
- Effet: Immunite totale aux degats
- Animation: Corps d'ombre
- Son: Void whispers

**Synergie visee**: Ultimate mobility fantasy

---

#### 8.3 - DEATH NOTE
> *Marquer un ennemi le tue apres 10s, peu importe ses PV*

**Effet**: Delayed instakill
- Delay: 10s
- Effet: Mort garantie (Bosses = 50% HP direct)
- Cooldown: 60s
- Animation: Compte a rebours sur la cible
- Son: Clock ticking then death knell

**Synergie visee**: Ultimate hunter fantasy

---

#### 8.4 - APOCALYPSE TOXIQUE
> *Vous emettez constamment un nuage de poison autour de vous*

**Effet**: Passive poison aura
- Rayon: 5 blocs
- Degats: 20% degats de base/s
- Effets: Tous vos bonus de poison s'appliquent
- Animation: Nuage toxique permanent
- Son: Hissing gas

**Synergie visee**: Ultimate plague fantasy

---

#### 8.5 - BULLET TIME
> *Activer ralentit le temps a 25% pendant 5s (vous bougez normalement)*

**Effet**: Time slow
- Activation: Crouch + Jump
- Duree: 5s
- Slow: Le monde est a 25% vitesse
- Cooldown: 60s
- Animation: Effet Matrix
- Son: Time distortion

**Synergie visee**: Ultimate control fantasy

---

## OCCULTISTE
### Identite: Mage devastateur, AoE magique, manipulation des elements et des ames

---

### PALIER 1 - NIVEAU 0 (Fondation)

#### 1.1 - EMBRASEMENT
> *Vos attaques ont 25% de chance d'enflammer l'ennemi (50% degats sur 3s)*

**Effet**: Fire DoT
- Chance: 25%
- Degats: 50% degats de base sur 3s
- Animation: Ennemi en feu
- Son: Woosh de flammes

**Synergie visee**: Fire mage core

---

#### 1.2 - GIVRE MORDANT
> *Vos attaques ont 25% de chance de geler l'ennemi (slow 50% pendant 2s)*

**Effet**: Frost slow
- Chance: 25%
- Slow: 50% vitesse
- Duree: 2s
- Animation: Cristaux de glace sur l'ennemi
- Son: Craquement de glace

**Synergie visee**: Frost mage core

---

#### 1.3 - ARC ELECTRIQUE
> *Vos attaques ont 25% de chance de chain lightning (3 ennemis proches)*

**Effet**: Chain lightning
- Chance: 25%
- Cibles: 3 ennemis
- Degats: 60% par cible
- Range: 5 blocs entre cibles
- Animation: Eclairs qui sautent
- Son: Zap electrique

**Synergie visee**: Lightning mage core

---

#### 1.4 - SIPHON D'AME
> *Chaque kill restaure 3% de vos PV max et genere une orbe d'ame*

**Effet**: Soul harvest
- Heal: 3% PV max par kill
- Orbes: Flottent autour de vous (max 5)
- Animation: Ames qui remontent du cadavre
- Son: Whisper d'ames

**Synergie visee**: Soul/Dark mage core

---

#### 1.5 - VOID BOLT
> *20% de chance que votre attaque soit remplacee par un projectile void*

**Effet**: Special projectile
- Chance: 20%
- Degats: 150% degats de base
- Effet: Traverse tous les ennemis
- Animation: Projectile violet sombre
- Son: Void whoosh

**Synergie visee**: Void mage core

---

### PALIER 2 - NIVEAU 5 (Amplification)

#### 2.1 - PROPAGATION
> *Les ennemis en feu enflamment les ennemis proches*

**Effet**: Fire spread
- Range: 2 blocs
- Tick: Check chaque seconde
- Animation: Flammes qui sautent
- Son: Fire whoosh

**Synergie visee**: Fire AoE

---

#### 2.2 - COEUR DE GLACE
> *Les ennemis geles prennent +30% de degats et peuvent etre shatter*

**Effet**: Frost damage amp + shatter
- Bonus degats: +30%
- Shatter: Si tue pendant gel = AoE de glace (3 blocs)
- Animation: Explosion de glace
- Son: Glass shatter

**Synergie visee**: Frost burst

---

#### 2.3 - SURCHARGE
> *Les chain lightning peuvent crit et chaque crit ajoute une cible*

**Effet**: Lightning crit bonus
- Crit: Oui (meme % que vous)
- Bonus crit: +1 cible
- Animation: Eclairs plus intenses sur crit
- Son: Thunder crack

**Synergie visee**: Lightning scaling

---

#### 2.4 - RESERVOIR D'AMES
> *Les orbes d'ames peuvent etre consommees pour un burst de degats*

**Effet**: Soul burst
- Activation: Crouch + Attack
- Degats par orbe: 100% degats de base
- Consomme: Toutes les orbes
- Animation: Explosion d'ames
- Son: Soul scream

**Synergie visee**: Soul burst

---

#### 2.5 - INSTABILITE DU VOID
> *Les Void Bolts explosent a l'impact*

**Effet**: Void explosion
- Rayon: 3 blocs
- Degats AoE: 80% degats de base
- Animation: Implosion violette
- Son: Void collapse

**Synergie visee**: Void AoE

---

### PALIER 3 - NIVEAU 10 (Specialisation)

#### 3.1 - TEMPETE DE FEU
> *30% de chance sur attaque de faire pleuvoir des meteores*

**Effet**: Fire meteor proc
- Chance: 30%
- Meteores: 3
- Degats: 80% par meteore
- Zone: 5 blocs
- Animation: Pluie de feu
- Son: Meteor impacts

**Synergie visee**: Fire AoE core

---

#### 3.2 - BLIZZARD
> *Les ennemis geles generent une aura de froid qui gele les autres*

**Effet**: Frost chain reaction
- Rayon aura: 3 blocs
- Duree: Tant que l'ennemi est gele
- Animation: Zone de blizzard
- Son: Howling wind

**Synergie visee**: Frost control core

---

#### 3.3 - TEMPETE ELECTRIQUE
> *Vous generez des eclairs autour de vous en permanence*

**Effet**: Passive lightning
- Tick: Toutes les 1.5s
- Cibles: 2 ennemis proches
- Degats: 40% degats de base
- Range: 6 blocs
- Animation: Eclairs constants
- Son: Constant crackling

**Synergie visee**: Lightning passive core

---

#### 3.4 - PACTE DES AMES
> *Chaque orbe d'ame augmente vos degats de 5%*

**Effet**: Damage per soul
- Bonus: +5% degats par orbe
- Max: +25% (5 orbes)
- Animation: Ames qui brillent plus
- Son: Power building

**Synergie visee**: Soul stacking core

---

#### 3.5 - RIFT DIMENSIONNEL
> *Les Void Bolts laissent une faille qui tire sur les ennemis proches*

**Effet**: Persistent void zone
- Duree: 3s
- Tick: 1 tir/s sur ennemi le plus proche
- Degats: 50% degats de base
- Animation: Portail violet
- Son: Void hum

**Synergie visee**: Void zone control core

---

### PALIER 4 - NIVEAU 15 (Evolution)

#### 4.1 - PHOENIX
> *Les ennemis tues par le feu ont 30% de chance d'exploser*

**Effet**: Fire death explosion
- Chance: 30%
- Degats: 150% degats de base
- Rayon: 4 blocs
- Animation: Phoenix explosion
- Son: Phoenix cry + explosion

**Synergie visee**: Fire chain reaction

---

#### 4.2 - ZERO ABSOLU
> *Les ennemis geles pendant 3s+ sont instakill (petits) ou prennent 500% degats (gros)*

**Effet**: Deep freeze execute
- Condition: 3s de gel continu
- Petits ennemis: Instakill
- Gros/Bosses: 500% degats burst
- Animation: Statue de glace qui explose
- Son: Ice shatter

**Synergie visee**: Frost execute

---

#### 4.3 - CONDUCTEUR
> *Les eclairs vous soignent pour 5% des degats infliges*

**Effet**: Lightning leech
- Heal: 5% des degats de lightning
- Animation: Energie qui revient
- Son: Energy absorb

**Synergie visee**: Lightning sustain

---

#### 4.4 - MOISSON ETERNELLE
> *Les orbes d'ames regenerent 1% de vos PV/s chacune*

**Effet**: Soul heal over time
- Regen: 1% PV/s par orbe
- Max: 5% PV/s (5 orbes)
- Animation: Liens d'ames vers vous
- Son: Soul whisper

**Synergie visee**: Soul sustain

---

#### 4.5 - ANCRE DU VOID
> *Les failles dimensionnelles attirent les ennemis vers elles*

**Effet**: Void pull
- Pull: Slow constant vers le centre
- Strength: 30% de leur vitesse vers le rift
- Animation: Distortion gravitationnelle
- Son: Gravity warp

**Synergie visee**: Void crowd control

---

### PALIER 5 - NIVEAU 20 (Maitrise)

#### 5.1 - AVATAR DE FEU
> *Vous etes constamment entoure de flammes qui brulent les ennemis proches*

**Effet**: Fire aura
- Rayon: 3 blocs
- Degats: 30% degats de base/s
- Bonus: Immunite au feu
- Animation: Aura de flammes
- Son: Roaring fire

**Synergie visee**: Fire melee hybrid

---

#### 5.2 - SEIGNEUR DU GIVRE
> *Vos attaques ont 100% de chance de geler et la duree de gel est doublee*

**Effet**: Guaranteed freeze
- Chance: 100%
- Duree: x2 (4s au lieu de 2s)
- Animation: Vous etes entoure de glace
- Son: Frost lord ambiance

**Synergie visee**: Frost perma-CC

---

#### 5.3 - DIEU DE LA FOUDRE
> *Les chain lightning n'ont plus de limite de cibles*

**Effet**: Unlimited chains
- Cibles: Illimitees (tous les ennemis en range)
- Range chain: 6 blocs
- Animation: Web d'eclairs
- Son: Thunder storm

**Synergie visee**: Lightning AoE mastery

---

#### 5.4 - LEGION D'AMES
> *Vous pouvez stocker 10 orbes d'ames et elles vous protegent (5% DR chacune)*

**Effet**: More souls + defense
- Max orbes: 10
- Damage reduction: 5% par orbe (max 50%)
- Animation: Cercle d'ames protectrices
- Son: Legion whispers

**Synergie visee**: Soul tank

---

#### 5.5 - MAITRE DU VOID
> *Les failles dimensionnelles peuvent etre detonees pour des degats massifs*

**Effet**: Void detonate
- Activation: Re-tirer dans la faille
- Degats: 300% degats de base
- Rayon: 5 blocs
- Animation: Implosion puis explosion
- Son: Reality tear

**Synergie visee**: Void burst mastery

---

### PALIER 6 - NIVEAU 30 (Transcendance)

#### 6.1 - INFERNO
> *Toutes les 10s, une vague de feu emane de vous*

**Effet**: Fire nova
- Cooldown: 10s
- Degats: 200% degats de base
- Rayon: 6 blocs
- Enflamme: Tous les touches
- Animation: Explosion de feu
- Son: Inferno roar

**Synergie visee**: Fire AoE transcendence

---

#### 6.2 - ERE GLACIAIRE
> *Les zones de gel persistent au sol pendant 5s apres la mort d'un ennemi gele*

**Effet**: Frost zones on kill
- Duree: 5s
- Effet: Gele les ennemis qui passent
- Rayon: 2 blocs
- Animation: Patch de glace
- Son: Freezing wind

**Synergie visee**: Frost zone control transcendence

---

#### 6.3 - TEMPETE PERPETUELLE
> *Vous etes entoure d'une tempete electrique permanente*

**Effet**: Lightning storm aura
- Rayon: 5 blocs
- Tick: Toutes les 0.5s
- Cibles: 3 random dans la zone
- Degats: 25% par hit
- Animation: Storm cloud around you
- Son: Perpetual thunder

**Synergie visee**: Lightning passive transcendence

---

#### 6.4 - NECROMANCIEN
> *Les orbes d'ames peuvent etre depensees pour invoquer des squelettes*

**Effet**: Soul summons
- Cout: 1 orbe = 1 squelette
- Squelette: 50% de vos stats, 10s duree
- Max: 5 squelettes
- Animation: Squelette qui emerge
- Son: Bone rattle

**Synergie visee**: Soul summoner transcendence

---

#### 6.5 - DIMENSION CORROMPUE
> *Vous existez partiellement dans le void - 25% de chance d'eviter les degats*

**Effet**: Void dodge
- Chance: 25%
- Effet: Degats completement ignores
- Animation: Body flicker void
- Son: Reality skip

**Synergie visee**: Void defense transcendence

---

### PALIER 7 - NIVEAU 40 (Apex)

#### 7.1 - SOLEIL NOIR
> *Invoquez un soleil de feu qui brule tout pendant 10s*

**Effet**: Fire sun summon
- Cooldown: 30s
- Duree: 10s
- Degats: 100% degats de base/s
- Rayon: 8 blocs
- Animation: Soleil ardent au-dessus de vous
- Son: Solar flare ambiance

**Synergie visee**: Fire legendary AoE

---

#### 7.2 - HIVER ETERNEL
> *Les ennemis dans votre zone sont constamment ralentis de 70%*

**Effet**: Permanent slow aura
- Rayon: 8 blocs
- Slow: 70%
- Bonus: +50% degats a vos attaques contre les slowed
- Animation: Blizzard permanent
- Son: Arctic wind

**Synergie visee**: Frost legendary control

---

#### 7.3 - MJOLNIR
> *Vos chain lightning frappent 3 fois chacun*

**Effet**: Triple strike
- Strikes: 3 par cible
- Degats: 60% x3 = 180% total
- Animation: Triple zap
- Son: Thunder god strike

**Synergie visee**: Lightning legendary damage

---

#### 7.4 - SEIGNEUR DES MORTS
> *Les ennemis tues ont 50% de chance de revenir comme vos serviteurs*

**Effet**: Raise dead
- Chance: 50%
- Serviteur: 30% de leurs stats, vous servent 15s
- Max: 10 serviteurs
- Animation: Resurrection sombre
- Son: Unholy revival

**Synergie visee**: Soul army legendary

---

#### 7.5 - TROU NOIR
> *Les failles peuvent fusionner en un trou noir devastateur*

**Effet**: Merge rifts into black hole
- Condition: 3+ failles proches (5 blocs)
- Auto-merge: Oui
- Degats: 500% degats de base sur la duree
- Pull: Tous les ennemis aspires
- Duree: 5s
- Animation: Black hole
- Son: Gravity collapse

**Synergie visee**: Void legendary

---

### PALIER 8 - NIVEAU 50 (Legendaire)

#### 8.1 - PLUIE DE METEORES
> *Toutes les 60s, une pluie de 20 meteores devastateurs*

**Effet**: Meteor apocalypse
- Cooldown: 60s
- Meteores: 20
- Degats: 200% chacun
- Zone: 20 blocs
- Animation: Apocalypse de feu
- Son: End of the world

**Synergie visee**: Ultimate fire fantasy

---

#### 8.2 - STASE TEMPORELLE
> *Gelez le temps pour tous les ennemis pendant 5s*

**Effet**: Time freeze
- Activation: Crouch + Jump
- Duree: 5s
- Effet: Ennemis completement immobiles
- Cooldown: 90s
- Animation: Monde en nuances de bleu
- Son: Time stop

**Synergie visee**: Ultimate frost fantasy

---

#### 8.3 - JUGEMENT DIVIN
> *Un eclair divin frappe tous les ennemis a l'ecran simultanement*

**Effet**: Screen-wide lightning
- Activation: Toutes les 30s
- Degats: 300% degats de base a TOUS
- Animation: Ciel qui s'ouvre, eclairs divins
- Son: Divine judgment

**Synergie visee**: Ultimate lightning fantasy

---

#### 8.4 - ARMEE IMMORTELLE
> *Vos serviteurs sont immortels et respawn apres 5s s'ils meurent*

**Effet**: Immortal army
- Respawn: 5s apres mort
- Buff serviteurs: +50% stats
- Animation: Ames immortelles
- Son: Legion eternal

**Synergie visee**: Ultimate soul fantasy

---

#### 8.5 - EFFACEMENT
> *Creez une zone de void qui efface tout ce qu'elle touche*

**Effet**: Instakill zone
- Activation: Crouch + Attack (5s charge)
- Zone: 10 blocs
- Effet: Instakill tous les ennemis (Bosses = 70% HP)
- Duree: 3s
- Cooldown: 120s
- Animation: Zone de neant absolu
- Son: Reality erasure

**Synergie visee**: Ultimate void fantasy

---

## SYNERGIES ET BUILDS

### GUERRIER

| Build | Talents cles | Playstyle |
|-------|-------------|-----------|
| **AoE Devastator** | Frappe Sismique → Echo de Guerre → Tourbillon → Resonance → Cataclysme → Tremor → Apocalypse → Ragnarok | Spam AoE, horde clear |
| **Vampire Immortel** | Soif de Sang → Frenetique → Vampire de Guerre → Frisson → Immortel → Avatar de Sang → Seigneur Vampire → Dieu du Sang | Lifesteal, unkillable |
| **Riposte Master** | Fureur Croissante → Bastion → Colere des Ancetres → Vengeance Ardente → Aegis Eternal → Represailles → Nemesis → Avatar de Vengeance | Counter-attack focused |
| **Tank Absolu** | Peau de Fer → Bastion → Titan Immuable → Forteresse → Aegis Eternal → Bastille → Colosse → Citadelle Vivante | Pure defense |
| **Executioner** | Charge Devastatrice → Dechaînement → Executeur → Moisson Sanglante → Seigneur de Guerre → Faucheur → Ange de la Mort → Extinction | Execute focused |

### CHASSEUR

| Build | Talents cles | Playstyle |
|-------|-------------|-----------|
| **Rain of Death** | Tirs Multiples → Ricochet → Pluie de Fleches → Deluge → Tempete d'Acier → Armageddon → Meteor Shower → Orbital Strike | AoE spam |
| **Ghost Assassin** | Chasseur Agile → Fantome → Oeil du Predateur → Predateur Supreme → Spectre → Maitre des Ombres → Reaper → Void Walker | Stealth burst |
| **Bounty Hunter** | Marque du Chasseur → Rafale → Traqueur → Sentence de Mort → Chasseur de Primes → Executeur de Primes → Chasseur Legendaire → Death Note | Mark and execute |
| **Plague Doctor** | Fleches Percantes → Venin → Toxines Mortelles → Pandemie → Epidemie → Peste Noire → Blight → Apocalypse Toxique | DoT spreading |
| **Gun Bunny** | Oeil de Lynx → Sniper → Tireur d'Elite → Surchauffe → Zone de Mort → Gatling → Arsenal Vivant → Bullet Time | DPS machine |

### OCCULTISTE

| Build | Talents cles | Playstyle |
|-------|-------------|-----------|
| **Pyromancer** | Embrasement → Propagation → Tempete de Feu → Phoenix → Avatar de Feu → Inferno → Soleil Noir → Pluie de Meteores | Fire everything |
| **Frost Lich** | Givre Mordant → Coeur de Glace → Blizzard → Zero Absolu → Seigneur du Givre → Ere Glaciaire → Hiver Eternel → Stase Temporelle | CC god |
| **Storm Caller** | Arc Electrique → Surcharge → Tempete Electrique → Conducteur → Dieu de la Foudre → Tempete Perpetuelle → Mjolnir → Jugement Divin | Chain lightning |
| **Necromancer** | Siphon d'Ame → Reservoir d'Ames → Pacte des Ames → Moisson Eternelle → Legion d'Ames → Necromancien → Seigneur des Morts → Armee Immortelle | Summon army |
| **Void Lord** | Void Bolt → Instabilite du Void → Rift Dimensionnel → Ancre du Void → Maitre du Void → Dimension Corrompue → Trou Noir → Effacement | Zone control |

---

## SPECIFICATIONS TECHNIQUES

### Animation Guidelines

1. **Echelle visuelle par palier**
   - Niveaux 0-15: Effets subtils mais visibles
   - Niveaux 20-30: Effets notables, particules moyennes
   - Niveaux 40-50: Effets epiques, ecran entier possible

2. **Performance**
   - Max 50 particules par effet standard
   - Max 200 particules pour effets legendaires
   - Utiliser des sprites plutot que des entites
   - Pooling des particules obligatoire

3. **Feedback**
   - Son obligatoire pour chaque proc
   - Flash visuel sur le joueur pour les procs importants
   - Ecran shake leger pour les gros impacts
   - Text popup pour les instakills et executions

### Scaling

1. **Degats des talents**
   - Base: % des degats de base du joueur
   - Peut crit: Oui sauf indication contraire
   - Applique les bonus de classe: Oui

2. **Cooldowns internes**
   - Proc avec % chance: 0.5s cooldown interne minimum
   - Gros effets: 5-30s cooldown
   - Legendaires: 30-120s cooldown

### Donnees a stocker

```
PlayerClassTalents:
  - playerId: UUID
  - classType: GUERRIER/CHASSEUR/OCCULTISTE
  - selectedTalents:
    - tier1: talentId (ou null)
    - tier2: talentId (ou null)
    - tier3: talentId (ou null)
    - tier4: talentId (ou null)
    - tier5: talentId (ou null)
    - tier6: talentId (ou null)
    - tier7: talentId (ou null)
    - tier8: talentId (ou null)
  - lastTalentChange: timestamp
```

### UI Structure

```
ClassInfoGUI (45 slots):
  Page 1:
    - Slot 4: Header (classe + niveau)
    - Slots 10-16: Tier 1 talents (niveau 0)
    - Slots 19-25: Tier 2 talents (niveau 5)
    - Slots 28-34: Tier 3 talents (niveau 10)
    - Slots 37-43: Tier 4 talents (niveau 15)
    - Slot 44: Page suivante

  Page 2:
    - Slot 4: Header (classe + niveau)
    - Slots 10-16: Tier 5 talents (niveau 20)
    - Slots 19-25: Tier 6 talents (niveau 30)
    - Slots 28-34: Tier 7 talents (niveau 40)
    - Slots 37-43: Tier 8 talents (niveau 50)
    - Slot 36: Page precedente
```

---

## RESUME

- **120 talents** au total (40 par classe)
- **8 paliers** de niveaux (0, 5, 10, 15, 20, 30, 40, 50)
- **5 choix** par palier
- **15+ builds viables** (5 par classe)
- Focus sur le **gameplay impact** et la **synergie**
- **Effets visuels** satisfaisants et **feedback** constant
