# Journal de développement

## 2026-08-19 — Migration MVC : `ServicePortefeuille.getDonnees()` et la visibilité de paquet

### Ce qui a été écrit

`ServicePortefeuille` : ajout de `getDonnees()`, une méthode à visibilité de paquet (aucun
modificateur devant `Portefeuille getDonnees()`) qui renvoie le `Portefeuille` détenu par le
service. En contrepartie, `getObjectif(int)` a été retirée : ce n'était qu'un accesseur de plus,
et le service ne doit garder que trois responsabilités (détenir le portefeuille, calculer le
solde et le total épargné, sauvegarder).

`ServiceEpargne` : `contribuerObjectif()` passe maintenant par `servicePortefeuille.getDonnees().getObjectif(idObjectif)`
plutôt que par un accesseur dédié.

### Choix de conception

**Pourquoi une méthode à visibilité de paquet plutôt qu'un getter public.**

Un getter public (`getPortefeuille()` ou `getDonnees()` publique) aurait permis à n'importe
quelle classe du projet — y compris un contrôleur ou une vue — de récupérer une référence brute
sur `Portefeuille` et d'appeler ses méthodes directement, en court-circuitant complètement les
règles portées par les services (validation, calcul du solde, sauvegarde automatique). C'est
exactement ce que le `CLAUDE.md` interdit explicitement pour `ServicePortefeuille`.

La visibilité de paquet (aucun modificateur en Java) restreint l'accès aux classes du **même
paquet**. `getDonnees()` est donc appelable depuis n'importe quel service de `modele.service`
(`ServiceEpargne`, et bientôt `ServiceTransaction`, `ServiceCategorie`, `ServiceStatistique`),
mais pas depuis `controleur` ni `vue`, qui sont des paquets différents.

**Ce que ça empêche exactement.** Un contrôleur qui écrirait
`servicePortefeuille.getDonnees().ajouterTransaction(...)` pour aller plus vite, en sautant les
vérifications de `ServiceTransaction`, ne compilerait tout simplement pas. Vérifié concrètement :
le même appel copié dans `Menu` (paquet `presentation`) donne à la compilation
`error: getDonnees() is not public in ServicePortefeuille; cannot be accessed from outside package`.
Ce n'est donc pas une règle de discipline ("on essaie de ne pas faire ça") mais une contrainte que
le compilateur fait respecter tout seul — plus fiable qu'un commentaire ou qu'une relecture.

C'est un compromis entre `private` (inutilisable même par les autres services, ce qui aurait
obligé à dupliquer l'accès aux données dans chacun) et `public` (utilisable par tout le monde,
ce qui aurait cassé la séparation des couches). La visibilité de paquet correspond exactement au
périmètre voulu : "les services entre eux, et personne d'autre".

### Points à savoir défendre

- **Pourquoi ne pas avoir laissé `getObjectif(int)` sur `ServicePortefeuille` ?** Parce que
  chaque méthode d'accès ajoutée au fil des besoins des autres services aurait fini par
  reconstituer, méthode par méthode, l'intégralité de l'API de `Portefeuille` — `getDonnees()`
  seule couvre tous les besoins futurs sans cette dérive.
- **Pourquoi ce n'est pas la même chose qu'un simple commentaire "ne pas utiliser en dehors des
  services" ?** Parce qu'un commentaire ne bloque personne, alors que la visibilité de paquet
  fait échouer la compilation — la garantie est structurelle, pas déclarative.

### Reste à faire

`ServiceTransaction` : va utiliser `getDonnees()` pour accéder aux transactions, refermant au
passage le trou de sauvegarde temporaire sur `ajouterDepense`/`ajouterRevenu`/`modifierTransaction`/`supprimerTransaction`.
Puis `ServiceCategorie`, la suite de `ServiceEpargne` (`creerObjectif`, `retirerObjectif`,
`supprimerObjectif`), et `ServiceStatistique`.

## 2026-08-19 — Rattrapage — début de la migration MVC

Cette entrée est écrite après coup, pour couvrir le travail fait avant la création de ce
journal ; elle ne reflète pas l'ordre exact des décisions au moment où elles ont été prises.

### Ce qui a été écrit

- **Déplacement des entités et énumérations**, sans changement de logique : `Transaction`,
  `Epargne`, `MouvementEpargne`, `Portefeuille` vers `modele.entite` ; `TypeTransaction`,
  `Categorie`, `SensMouvement` vers `modele.enumeration`. Les imports de `Menu`, `Main` et
  `GestionnaireFichier` mis à jour en conséquence.
- **`ServicePortefeuille`** créé dans `modele.service` : d'abord une simple délégation vers
  `Portefeuille.getSoldeDisponible()`/`getTotalEpargne()` (le temps que l'entité garde encore
  ces méthodes), puis le vrai calcul rapatrié dans le service une fois que plus rien dans
  `Portefeuille` n'en avait besoin en interne.
- **`ServiceEpargne`** créé avec une seule méthode, `contribuerObjectif()`, déplacée depuis
  `Portefeuille`.
- **Trois corrections de conception**, appliquées ensemble : `getTransactions()`/`getObjectifs()`
  confirmées non modifiables ; `sauvegarder()` et le `GestionnaireFichier` déplacés de
  `Portefeuille` vers `ServicePortefeuille`, seul point d'écriture ; `ServiceEpargne` recâblé
  pour ne dépendre que de `ServicePortefeuille`, plus jamais de `Portefeuille` directement.
- `Menu`/`Main` mis à jour à chaque étape pour utiliser les nouveaux services plutôt que
  `Portefeuille` directement, sur les points déjà migrés.

### Choix de conception

**Migrer plutôt que repartir de zéro.** Le code existant fonctionnait déjà et avait été testé de
bout en bout ; reconstruire par-dessus en préservant le comportement a semblé moins risqué que
tout réécrire, quitte à traverser des états transitoires imparfaits.

**Avancer par petits pas compilables, jamais par gros blocs.** Chaque déplacement de méthode a
été fait seul, recompilé, retesté avant de passer au suivant — plutôt que de déplacer
`Portefeuille` entier d'un coup en espérant que tout retombe juste.

**Accepter des états transitoires assumés plutôt que des contournements silencieux.** Deux fois,
une méthode dépendait encore d'une autre pas encore migrée : `contribuerObjectif()` avait besoin
de `getSoldeDisponible()`, qui n'était pas encore dans un service. Plutôt que de laisser la
première dans l'entité "en attendant", la règle retenue a été : on déplace les deux ensemble,
même si le service receveur ne contient encore qu'une seule méthode.

### Points à savoir défendre

- **Pourquoi `ServiceEpargne` n'a-t-il eu qu'une seule méthode pendant un temps ?** Parce qu'une
  classe de service peut naître avec une seule responsabilité et s'étoffer ensuite — c'est
  préférable à retarder l'extraction en attendant d'avoir "tout" à y mettre d'un coup.
- **Pourquoi le journal n'a-t-il pas été tenu depuis le début, alors que le `CLAUDE.md` le
  demande sans qu'on ait à le rappeler ?** Oubli pur et simple pendant la bascule vers cette
  nouvelle architecture ; corrigé à partir de maintenant, une entrée par étape, au moment de
  l'étape.

### Pièges rencontrés

En retirant `sauvegarder()` de `Portefeuille`, les méthodes pas encore migrées
(`ajouterDepense`, `activerCategorie`, `creerObjectif`, etc.) ont perdu la sauvegarde
automatique jusqu'à ce que leurs services respectifs existent. Vérifié concrètement : une
session qui n'utilise que ces méthodes non migrées ne produit aucun fichier `portefeuille.json`
au redémarrage. Le trou se referme progressivement au fil des prochaines étapes, pas d'un coup.

### Reste à faire

Voir l'entrée précédente : `ServiceTransaction`, `ServiceCategorie`, la suite de
`ServiceEpargne`, `ServiceStatistique`, puis les vues et les contrôleurs.

## 2026-08-19 — `ServiceTransaction` et `ServiceCategorie`

### Ce qui a été écrit

- **`ServiceCategorie`** créé dans `modele.service` avec une seule méthode, `estActive(Categorie)`.
  Nécessaire à `ServiceTransaction` pour appliquer la règle "catégorie active" ; comme pour
  `ServiceEpargne`, la règle "on ne laisse pas dans l'entité en attendant" s'applique : plutôt
  que de garder cette vérification dans `Portefeuille` en attendant un futur `ServiceCategorie`
  complet, le service est né maintenant, avec une seule responsabilité.
- **`ServiceTransaction`** créé avec `ajouterDepense`, `ajouterRevenu`, `modifierTransaction`,
  `supprimerTransaction`, `getHistorique`, `filtrerParDate`, `filtrerParCategorie`,
  `filtrerParType`, déplacées depuis `Portefeuille`. Applique la règle "une transaction ne peut
  porter qu'une catégorie active" via `ServiceCategorie`, avant d'enregistrer ou de modifier.
- **`Portefeuille`** perd toute la section "Gestion des transactions" sauf trois méthodes
  structurelles : `getTransactions()` (déjà là), `genererIdTransaction()` (distribue l'id suivant
  et avance le compteur — reste ici car le compteur est un champ de l'entité, donc sauvegardé),
  `ajouterTransaction(Transaction)` et `retirerTransaction(Transaction)` (l'ajout/le retrait
  passent par une méthode dédiée, pas par un accès direct à la liste).
- `getTotalParCategorie`/`getTotalRevenusEtDepenses` (restent dans `Portefeuille` en attendant
  `ServiceStatistique`) réécrites pour filtrer la période en ligne plutôt que d'appeler
  `filtrerParDate()`, qui n'existe plus dans l'entité.
- `Menu`/`Main` mis à jour pour passer par `ServiceTransaction`.

### Choix de conception

**`genererIdTransaction()` reste dans l'entité.** Le compteur `prochainIdTransaction` est un
champ de `Portefeuille` (donc sérialisé par Gson) : le sortir de l'entité l'aurait rendu
inaccessible aux services sans casser l'encapsulation. Distribuer l'id suivant est une opération
mécanique sur une donnée de structure, pas une règle métier — elle reste défendable comme
méthode d'entité.

**`getTotalParCategorie`/`getTotalRevenusEtDepenses` réécrites en ligne plutôt que déplacées.**
Elles appelaient `filtrerParDate()`, qui vient de partir dans `ServiceTransaction`. Les déplacer
aussi aurait débordé sur `ServiceStatistique`, une étape que je n'avais pas la consigne
d'attaquer maintenant. Solution retenue : réécrire le filtrage par période directement dans ces
deux méthodes, pour qu'elles continuent de compiler sans dépendre d'une méthode déplacée. Ce
n'est pas la version finale — ces deux méthodes contiennent toujours un calcul et doivent
encore migrer vers `ServiceStatistique`.

### Points à savoir défendre

- **Pourquoi `ServiceCategorie` n'a-t-il qu'une seule méthode ?** Parce que `ServiceTransaction`
  en avait besoin immédiatement pour la règle "catégorie active", et que la règle du projet est
  de déplacer les méthodes bloquantes ensemble plutôt que de laisser l'une attendre l'autre dans
  l'entité.
- **Pourquoi `ServiceTransaction` ne détient-il pas `Portefeuille` ?** Seul
  `ServicePortefeuille` a cette référence (visibilité de paquet sur `getDonnees()`) ;
  `ServiceTransaction` passe systématiquement par lui, comme `ServiceEpargne`.

### Pièges rencontrés

Aucun cette fois : la compilation a échoué une fois comme prévu (appels de `Menu` vers les
méthodes tout juste retirées de `Portefeuille`), corrigée en redirigeant ces appels vers
`ServiceTransaction`.

### Reste à faire

`ServiceCategorie` (le reste : `activerCategorie`, `desactiverCategorie`,
`getCategoriesDisponibles`, `aCategorieActiveDeType`), la suite de `ServiceEpargne`
(`creerObjectif`, `retirerObjectif`, `supprimerObjectif`), `ServiceStatistique`
(`getTotalParCategorie`, `getTotalRevenusEtDepenses`), puis les vues et les contrôleurs.

## 2026-08-19 — Correction : validation du nom et du montant cible d'`Epargne`

### Ce qui a été écrit

`Epargne` : ajout de `validerNom(String)` et `validerMontantCible(double)` (privées), appelées
dans le constructeur, sur le même modèle que `Transaction`.

### Choix de conception

Ce n'était pas un manquement de conception (une méthode mal placée), mais un vrai bug : la règle
de gestion "Nom d'objectif, montant cible : nom non vide, cible strictement positive" (tableau
du `CLAUDE.md`, colonne "Entité") n'était tout simplement pas implémentée. Traité en priorité,
séparément du reste de la migration.

### Points à savoir défendre

**Pourquoi cette règle est-elle dans l'entité et pas dans un service ?** Parce qu'elle porte
uniquement sur les propres champs d'`Epargne` (son nom, son montant cible) — exactement ce que
les entités ont le droit de valider elles-mêmes, contrairement à une règle qui dépendrait d'un
autre objet (comme "contribution > solde disponible", qui elle regarde l'état du portefeuille).

### Pièges rencontrés

Aucun.

### Reste à faire

Voir l'entrée précédente.

## 2026-08-19 — Visibilité des validateurs de `MouvementEpargne`

### Ce qui a été écrit

`MouvementEpargne` : `validerMontant`, `validerSens`, `validerDate` passées de `public` à
`private`.

### Choix de conception

Incohérence de style relevée lors de l'audit précédent : `Transaction` déclare ses méthodes de
validation équivalentes en `private` (elles ne servent qu'en interne, au constructeur et aux
setters), alors que `MouvementEpargne` les avait laissées `public` sans raison. Vérifié avant
la modification qu'aucune classe extérieure ne les appelait (`grep` sur tout `src/`) : seul le
constructeur de `MouvementEpargne` s'en sert. Aucun appelant à corriger.

### Points à savoir défendre

**Pourquoi ces méthodes doivent-elles être `private` plutôt que `public` ?** Elles ne font sens
que comme étape interne de construction d'un `MouvementEpargne` valide. Les laisser publiques
suggérait à tort qu'elles pouvaient être appelées indépendamment depuis l'extérieur, ce qui
n'a pas de raison d'être et aurait pu induire en erreur un futur lecteur du code.

### Pièges rencontrés

Aucun.

### Reste à faire

`ServiceEpargne` complet (`getMontantActuel`, `getPourcentageAtteint`, `contribuer`, `retirer`,
`depasseraCible`, `estVide`, `estAtteint` depuis `Epargne` ; `creerObjectif`, `retirerObjectif`,
`supprimerObjectif` depuis `Portefeuille`), `ServiceCategorie` (le reste), `ServiceStatistique`,
puis les vues et les contrôleurs.

## 2026-08-19 — `ServiceEpargne` complet : dernières règles d'épargne extraites d'`Epargne` et `Portefeuille`

### Ce qui a été écrit

- **`Epargne`** : réduite à sa structure — attributs, constructeur avec ses deux validations,
  getters, et une seule méthode de comportement, `ajouterMouvement(MouvementEpargne)`. Toutes
  les méthodes de calcul (`getMontantActuel`, `getPourcentageAtteint`, `depasseraCible`,
  `estVide`, `estAtteint`) et `contribuer`/`retirer` ont été retirées, ainsi que `toString()`
  (qui appelait ces calculs).
- **`ServiceEpargne`** : récupère toutes les méthodes retirées d'`Epargne`, sous forme de
  méthodes qui prennent l'`Epargne` concernée en paramètre (`getMontantActuel(objectif)`,
  etc.), plus `creerObjectif`, `retirerObjectif`, `supprimerObjectif`, déplacées depuis
  `Portefeuille`. `contribuerObjectif` et `retirerObjectif` construisent désormais eux-mêmes le
  `MouvementEpargne` et l'ajoutent via `ajouterMouvement`, au lieu d'appeler `objectif.contribuer()`/`retirer()`.
- **`Portefeuille`** : `creerObjectif`, `retirerObjectif` (l'ancienne version, qui retirait de
  l'argent) et `supprimerObjectif` ont disparu. À la place, trois méthodes structurelles,
  symétriques de celles qui existaient déjà pour les transactions : `genererIdObjectif()`,
  `ajouterObjectif(Epargne)`, et un `retirerObjectif(Epargne)` qui ne fait plus que retirer
  l'objectif de la liste (la vérification "objectif vide" est faite en amont par
  `ServiceEpargne`). `getObjectif(int)` n'a pas bougé : c'est un accès par clé, pas un calcul.
- **`ServicePortefeuille.getTotalEpargne()`** : ne pouvait plus appeler
  `objectif.getMontantActuel()` (méthode retirée). Réécrite pour parcourir directement
  `objectif.getMouvements()`, exactement comme `getSoldeDisponible()` parcourt déjà les
  transactions sans passer par `ServiceTransaction`.
- **`Menu`** : les quatre appels à `portefeuille.creerObjectif/retirerObjectif/supprimerObjectif`
  et `objectif.depasseraCible(...)` remplacés par les équivalents sur `serviceEpargne`.
  `afficherObjectifs()` construisait sa ligne d'affichage via `objectif.toString()` ; comme ce
  `toString()` a disparu, la ligne est reconstruite explicitement avec `serviceEpargne.getMontantActuel(objectif)`
  et `serviceEpargne.getPourcentageAtteint(objectif)`.

### Choix de conception

**Pourquoi les méthodes de calcul de `ServiceEpargne` prennent-elles un `Epargne` en paramètre,
plutôt qu'un `idObjectif` comme `contribuerObjectif` ?** Ces méthodes (`getMontantActuel`,
`estVide`...) ne font aucune vérification qui nécessite de retrouver l'objectif dans le
portefeuille : `Menu` a déjà l'`Epargne` en main (récupérée via `portefeuille.getObjectif(id)`
pour l'afficher) au moment où il en a besoin. Refaire une recherche par id à chaque appel aurait
été un aller-retour inutile. `contribuerObjectif`/`retirerObjectif`/`supprimerObjectif`, eux,
prennent un id : ce sont des opérations déclenchées directement depuis le menu à partir de la
saisie de l'utilisateur, sans qu'aucun code intermédiaire n'ait déjà l'objet en main.

**Pourquoi `getTotalEpargne()` recalcule-t-il lui-même la somme des mouvements plutôt que
d'appeler `serviceEpargne.getMontantActuel(objectif)` ?** Parce que `ServiceEpargne` dépend déjà
de `ServicePortefeuille` dans son constructeur (pour lire le solde disponible et déclencher la
sauvegarde) : le faire dans l'autre sens aurait créé une dépendance circulaire entre les deux
services, que le constructeur ne peut pas résoudre. La solution retenue est la même que celle
déjà en place pour `getSoldeDisponible()`, qui parcourt directement les transactions sans passer
par `ServiceTransaction` : chaque service reste autonome pour les calculs qu'il expose au niveau
du portefeuille entier.

**Pourquoi supprimer `toString()` d'`Epargne` plutôt que le garder ?** Il appelait
`getMontantActuel()` et `getPourcentageAtteint()`, deux calculs. La consigne pour cette étape
était stricte : `Epargne` ne garde que ses attributs, son constructeur, ses getters, et
`ajouterMouvement`. Garder un `toString()` qui recalcule aurait réintroduit exactement le genre
de logique que cette étape avait pour but de sortir de l'entité. L'affichage correspondant a été
reconstruit dans `Menu`, qui a déjà accès à `serviceEpargne`.

**Pourquoi `Portefeuille.retirerObjectif(Epargne)` réutilise-t-il ce nom, alors qu'il désignait
avant une tout autre opération (retirer de l'argent) ?** Une fois l'opération "retirer de
l'argent" déplacée vers `ServiceEpargne.retirerObjectif(id, montant, date)`, le nom se libère
dans `Portefeuille` pour désigner ce qu'il fait réellement maintenant : une suppression
structurelle de la liste, exactement le rôle que joue déjà `retirerTransaction(Transaction)`
pour les transactions. Les deux méthodes sont maintenant symétriques.

### Points à savoir défendre

- **Le montant épargné d'un objectif est-il stocké quelque part ?** Non, jamais : ni dans
  `Epargne` (qui ne garde que la liste des mouvements), ni dans `ServiceEpargne`. Il est
  recalculé à chaque appel de `getMontantActuel(objectif)` en parcourant les mouvements, comme
  l'exige la règle de gestion.
- **Que se passe-t-il si on essaie d'appeler `objectif.getMontantActuel()` directement depuis
  `Menu`, comme avant cette étape ?** Ça ne compile plus : la méthode n'existe plus sur
  `Epargne`. C'est voulu — la seule façon d'obtenir ce montant est de passer par
  `serviceEpargne.getMontantActuel(objectif)`, ce qui garantit qu'aucun calcul métier ne peut se
  retrouver ailleurs que dans un service.
- **Pourquoi la suppression d'un objectif est-elle refusée avec `IllegalStateException` et pas
  `IllegalArgumentException` ?** Le montant du retrait ou l'identifiant ne sont pas en cause :
  c'est l'état actuel de l'objectif (non vide) qui empêche l'opération à cet instant précis. Une
  contribution plus tard, un retrait entre-temps, et l'opération redeviendrait possible sans que
  rien n'ait changé dans les arguments passés.

### Pièges rencontrés

Aucun — la compilation a servi de garde-fou à chaque déplacement : `ServicePortefeuille` ne
compilait plus tant que `getTotalEpargne()` n'avait pas été réécrite, ce qui a permis de repérer
cette dépendance cachée sur `Epargne.getMontantActuel()` immédiatement plutôt qu'à l'exécution.

### Reste à faire

`ServiceCategorie` (le reste : `activerCategorie`, `desactiverCategorie`,
`getCategoriesDisponibles`, `aCategorieActiveDeType`), `ServiceStatistique`, puis la séparation
de `Menu` en vues (`vue/`) et contrôleurs (`controleur/`) — `Menu` détient encore directement
`Portefeuille`, ce qui n'est plus permis une fois cette séparation faite.

## 2026-08-19 — `CalculEpargne` : un seul calcul du montant actuel, partagé sans dépendance circulaire

### Ce qui a été écrit

- **`CalculEpargne`** (nouvelle classe, `modele.service`) : une seule méthode statique,
  `calculerMontantActuel(Epargne)`, qui parcourt les mouvements de l'objectif et renvoie somme
  des contributions moins somme des retraits. Constructeur privé : c'est une classe utilitaire,
  jamais destinée à être instanciée.
- **`ServiceEpargne.getMontantActuel(Epargne)`** : ne recalcule plus elle-même, délègue à
  `CalculEpargne.calculerMontantActuel(objectif)`.
- **`ServicePortefeuille.getTotalEpargne()`** : délègue de la même façon, au lieu de reparcourir
  les mouvements en double de ce que faisait `ServiceEpargne`.

### Choix de conception

**Pourquoi une classe séparée plutôt qu'une méthode statique sur `ServiceEpargne` ?** Envisagé,
puis écarté : `ServiceEpargne` a un constructeur qui prend `ServicePortefeuille`, ses méthodes
sont toutes des méthodes d'instance qui portent des règles de gestion (contribution refusée si
solde insuffisant, etc.). Y ajouter une méthode statique sans rapport avec une instance aurait
mélangé deux natures de code différentes dans la même classe : difficile à expliquer d'un coup
("pourquoi celle-ci n'a pas besoin d'objet ServiceEpargne pour être appelée, contrairement aux
autres ?"). `CalculEpargne` n'a qu'un seul travail, il est visible au premier coup d'œil sur la
classe entière : un pur calcul, sans état, sans dépendance.

**Pourquoi ça règle le problème de dépendance circulaire.** `ServiceEpargne` dépend de
`ServicePortefeuille` (pour lire le solde disponible et déclencher la sauvegarde) : c'est fixé
dans le sens constructeur. Si `ServicePortefeuille.getTotalEpargne()` avait dû appeler une
méthode d'instance de `ServiceEpargne`, il aurait fallu que `ServicePortefeuille` détienne à son
tour une référence sur `ServiceEpargne` — un cycle que `Main` n'aurait pas pu construire (chacun
des deux aurait eu besoin de l'autre déjà construit). `CalculEpargne` n'est l'instance de rien :
les deux services l'appellent comme ils appelleraient `Math.abs(...)`, sans qu'aucun des deux
n'ait de référence sur l'autre.

### Points à savoir défendre

- **Le montant actuel d'un objectif est-il calculé à plusieurs endroits dans le code ?** Non,
  une seule fois, dans `CalculEpargne.calculerMontantActuel`. `ServiceEpargne` et
  `ServicePortefeuille` appellent tous les deux cette méthode plutôt que de reparcourir les
  mouvements chacun de leur côté — avant cette étape, `getTotalEpargne()` dupliquait exactement
  la boucle de `ServiceEpargne.getMontantActuel()`.
- **Pourquoi le constructeur de `CalculEpargne` est-il privé ?** La classe n'a aucune raison
  d'être instanciée : elle ne porte aucun état, seulement une méthode statique. Un constructeur
  privé l'empêche explicitement, plutôt que de laisser un constructeur par défaut public sans
  usage.

### Pièges rencontrés

Aucun.

### Reste à faire

Inchangé : `ServiceCategorie` (le reste), `ServiceStatistique`, puis la séparation de `Menu` en
vues et contrôleurs.

## 2026-08-19 — `ServiceCategorie` et `ServiceStatistique` : `Portefeuille` devient une entité pure

### Ce qui a été écrit

- **`ServiceCategorie`** : deux méthodes de plus, déplacées depuis `Portefeuille` sans
  changement de logique — `aCategorieActiveDeType(TypeTransaction)` et
  `getCategoriesDisponibles()`. `activerCategorie`/`desactiverCategorie` restent pour l'instant
  dans `Portefeuille` : pas demandées pour cette étape.
- **`ServiceStatistique`** (nouvelle classe, `modele.service`) : `getTotalParCategorie(debut, fin)`
  et `getTotalRevenusEtDepenses(debut, fin)`, déplacées depuis `Portefeuille` sans changement de
  logique. Lecture seule : pas d'appel à `sauvegarder()`, rien n'est modifié.
- **`Portefeuille`** : perd les quatre méthodes ci-dessus, et les imports devenus inutiles
  (`LocalDate`, `HashMap`, `Map`, `TypeTransaction`). Le commentaire d'en-tête de la classe, qui
  annonçait encore "de la logique métier à extraire progressivement", est mis à jour : ce n'est
  plus vrai.
- **`Menu`** et **`Main`** : `Menu` reçoit deux nouveaux paramètres de constructeur,
  `ServiceCategorie` et `ServiceStatistique` (le premier existait déjà dans `Main` pour
  `ServiceTransaction`, mais n'était pas passé à `Menu` ; le second est construit dans `Main`).
  Les quatre appels `portefeuille.aCategorieActiveDeType(...)`, `.getCategoriesDisponibles()`,
  `.getTotalParCategorie(...)`, `.getTotalRevenusEtDepenses(...)` remplacés par les équivalents
  sur les services.

### Choix de conception

**Pourquoi les mouvements d'épargne n'ont demandé aucun traitement particulier dans
`ServiceStatistique` ?** La règle de gestion ("mouvements d'épargne exclus des statistiques")
était déjà respectée avant même cette étape : `getTotalParCategorie` et
`getTotalRevenusEtDepenses` n'ont jamais parcouru que `getTransactions()`, et les mouvements
d'épargne n'ont jamais été stockés dans cette liste (ils vivent dans `Epargne.mouvements`,
depuis le sprint 4). Déplacer ces deux méthodes tel quel suffisait donc à préserver la règle —
rien à ajouter, juste à vérifier que le déplacement ne changeait rien à ce qui est parcouru.

**Pourquoi `ServiceStatistique` n'appelle-t-il jamais `sauvegarder()` ?** Contrairement aux
autres services, il ne modifie jamais `Portefeuille` : ses deux méthodes ne font que lire et
agréger. Appeler `sauvegarder()` ici aurait été une écriture disque inutile à chaque
consultation des statistiques.

### Points à savoir défendre

- **`Portefeuille` est-elle maintenant une entité "pure" au sens du `CLAUDE.md` ?** Oui : elle ne
  contient plus que des attributs privés, un constructeur, des getters (toujours des vues non
  modifiables pour les listes/ensembles), des méthodes d'ajout/retrait dans ses collections, la
  génération de ses compteurs d'identifiants, et un seul accès par clé (`getObjectif`/
  `trouverObjectif`, une recherche, pas un calcul). Aucune boucle n'y agrège plus de valeurs, ce
  qui était le test donné dans le `CLAUDE.md` pour repérer un calcul égaré dans une entité.
- **Pourquoi `activerCategorie`/`desactiverCategorie` restent-elles dans `Portefeuille` alors
  que le reste de la gestion des catégories est parti dans `ServiceCategorie` ?** Ce ne sont pas
  des calculs : ce sont des ajouts/retraits dans un `Set`, strictement structurels, au même
  titre que `ajouterTransaction` ou `ajouterObjectif`. Rien n'empêche de les y laisser
  définitivement, mais leur migration n'a pas été demandée pour cette étape — pas d'anticipation.
- **`getObjectif`/`trouverObjectif` ne sont-elles pas des exceptions à la règle "pas de boucle
  dans une entité" ?** Elles parcourent bien une liste, mais pour retrouver un élément par son
  identifiant, pas pour produire une valeur agrégée (somme, total, booléen dérivé de plusieurs
  éléments). C'est la distinction déjà actée pour cette méthode avant cette étape : un accès par
  clé plutôt qu'un calcul.

### Pièges rencontrés

Aucun.

### Reste à faire

La séparation de `Menu` en vues (`vue/`) et contrôleurs (`controleur/`) — `Menu` détient encore
directement `Portefeuille` (pour `getCategoriesActives`, `getObjectif`, `getObjectifs`,
`activerCategorie`, `desactiverCategorie`), ce qui n'est plus permis une fois cette séparation
faite.

## 2026-08-19 — Début de la séparation vue/contrôleur : `VueConsole` et `ControleurPrincipal`

### Ce qui a été écrit

- **`VueConsole`** (nouvelle classe, paquet `vue`) : les briques de saisie/affichage
  réutilisées par tous les écrans à venir — `lireLigne`, `lireEntier`, `lireMontant`,
  `lireDate`, `confirmer`, `afficherMessage`, `afficherErreur`. Reprises quasi telles quelles
  depuis les méthodes privées équivalentes de `Menu` (`demanderMontant`, `demanderDate`,
  `demanderConfirmation`, `lireEntier`), avec un seul changement réel : chaque méthode passe
  désormais par `afficherMessage`/`System.out.println` plutôt que par un `System.out.print`
  local, pour que toute sortie console ait un point de passage unique dans la classe.
- **`ControleurPrincipal`** (nouvelle classe, paquet `controleur`) : reprend la boucle
  `lancer()` de `Menu` et l'aiguillage du menu principal. Pour l'instant, seule l'option
  "Voir le solde" est câblée (elle n'a besoin que de `ServicePortefeuille`, déjà disponible).
  Les options 2 à 7 affichent "Fonctionnalité en cours de migration, pas encore disponible." —
  un message clair, pas une erreur ni un plantage — en attendant que chaque écran soit migré à
  son tour.
- **`Main`** : bascule sur `ControleurPrincipal`/`VueConsole` à la place de `Menu`. Ne construit
  plus que `GestionnaireFichier`, `Portefeuille` et `ServicePortefeuille` : les autres services
  (`ServiceEpargne`, `ServiceCategorie`, `ServiceTransaction`, `ServiceStatistique`) seront
  reconstruits ici au fur et à mesure que leurs contrôleurs respectifs seront écrits.
- **`Menu`** : non modifiée, mais plus référencée nulle part. Elle reste dans le dépôt comme
  source pour extraire les cinq écrans restants (transactions, épargne, catégories,
  statistiques, et l'affichage détaillé du menu principal), et sera supprimée à l'étape de
  nettoyage, une fois tous les écrans migrés — pas avant.

### Choix de conception

**Pourquoi couper `Main` sur `ControleurPrincipal` dès maintenant, plutôt que de garder `Menu`
actif jusqu'à la fin de la migration ?** Décidé explicitement avant d'écrire le code : la
bascule immédiate permet de tester chaque écran migré directement dans l'application réelle
(`java Main`), pas seulement par un harnais de test à part. Le prix à payer est une régression
temporaire assumée : les écrans 2 à 7 sont indisponibles jusqu'à ce qu'ils soient migrés un par
un dans les prochaines étapes. C'est le même type d'état transitoire que celui déjà traversé
pendant la migration des services (le trou de sauvegarde temporaire documenté plus haut dans ce
journal) : accepté et documenté plutôt que masqué.

**Pourquoi la mise en forme du solde (`%.2f FCFA`) reste dans `ControleurPrincipal` pour
l'instant, plutôt que dans une vue dédiée ?** Il n'existe pas encore de `VuePrincipale` : cette
étape ne construit que `VueConsole` (les briques génériques) et `ControleurPrincipal`. La
formule d'affichage du solde est donc assemblée dans le contrôleur avec `String.format`, puis
transmise déjà construite à `vueConsole.afficherMessage(String)`. Ce n'est pas une règle
métier (aucun calcul, aucune décision), donc ça ne viole pas la règle "un contrôleur ne contient
aucune règle métier" — mais ce n'est pas non plus l'endroit définitif : cet affichage migrera
vers `VuePrincipale` dès qu'elle existera, pour que la présentation (le format d'un montant)
reste uniquement du ressort des vues.

**Pourquoi le menu principal est-il affiché ligne par ligne par `ControleurPrincipal` plutôt
que par une vue ?** Même raison : `VuePrincipale` n'existe pas encore. `afficherMenu()` reste
temporairement dans le contrôleur, qui délègue chaque ligne à `vueConsole.afficherMessage(...)`
(donc aucun `System.out.println` direct dans le contrôleur, la règle est respectée). Cette
méthode migrera vers `VuePrincipale` à l'étape suivante.

### Points à savoir défendre

- **`ControleurPrincipal` contient-il un `System.out.println` ou une règle métier ?** Non : tout
  affichage passe par `vueConsole.afficherMessage(...)`, et `gererVoirSolde()` ne fait que lire
  deux valeurs déjà calculées par `ServicePortefeuille` et les transmettre. Aucun calcul, aucune
  décision métier n'a lieu dans le contrôleur.
- **`VueConsole` importe-t-elle `modele.service` ?** Non, uniquement `java.time` et
  `java.util.Scanner` : elle ne fait qu'entrées/sorties, jamais d'appel à un service.
- **Pourquoi `Menu.java` n'est-elle pas supprimée tout de suite, si elle n'est plus utilisée ?**
  Elle sert encore de référence directe pour extraire la logique des cinq écrans restants (le
  code n'a pas besoin d'être réinventé, seulement redistribué entre vue et contrôleur). La
  supprimer maintenant obligerait à retrouver cette logique dans l'historique Git à chaque
  écran. Elle disparaîtra à la toute dernière étape, une fois qu'elle sera vraiment sans usage.

### Pièges rencontrés

Aucun.

### Reste à faire

`VuePrincipale` (affichage du menu principal et de l'écran solde, à extraire du contrôleur) ;
`VueTransaction`/`ControleurTransaction`, `VueEpargne`/`ControleurEpargne`,
`VueCategorie`/`ControleurCategorie`, `VueStatistique`/`ControleurStatistique`, chacun câblé un
par un dans `ControleurPrincipal` et `Main` ; puis suppression de `Menu.java`.

## 2026-08-19 — Correction : `VuePrincipale` sort tout l'affichage de `ControleurPrincipal`

### Ce qui a été écrit

- **`VuePrincipale`** (nouvelle classe, `vue`, hérite de `VueConsole`) : trois méthodes —
  `afficherMenuPrincipal()`, `afficherSolde(double soldeDisponible, double totalEpargne)`,
  `afficherFonctionnaliteIndisponible()`. Elles contiennent exactement le texte et la mise en
  forme (`String.format("%.2f FCFA", ...)`) qui vivaient jusque-là dans `ControleurPrincipal`.
- **`ControleurPrincipal`** : `afficherMenu()` a disparu, remplacée par un appel à
  `vuePrincipale.afficherMenuPrincipal()`. `gererVoirSolde()` ne fait plus de `String.format` :
  elle lit `soldeDisponible` et `totalEpargne` auprès de `ServicePortefeuille`, et les transmet
  telles quelles à `vuePrincipale.afficherSolde(...)`. Le contrôleur ne construit plus aucun
  texte d'écran ; il ne fait plus que lire des valeurs et les transmettre.
- **`Main`** : construit et relie `VuePrincipale` à la place de `VueConsole` (héritage oblige,
  `VuePrincipale` reste utilisable partout où `VueConsole` l'était, avec les trois méthodes en
  plus).

### Choix de conception

**Pourquoi corriger tout de suite, plutôt que continuer et corriger plus tard sur les six
écrans ?** Remarque explicite reçue avant de commiter : la limite entre "un contrôleur peut
transmettre un message via `afficherMessage`" et "un contrôleur construit lui-même l'affichage"
n'était pas assez nette dans la première version — `afficherMenu()` et le `String.format` du
solde en étaient la preuve concrète. Corriger dès le premier écran fixe le patron à suivre pour
`ControleurTransaction`, `ControleurEpargne`, `ControleurCategorie` et `ControleurStatistique` :
chaque contrôleur appelle un service, récupère des valeurs déjà calculées, les passe telles
quelles à une méthode de vue nommée pour l'écran concerné (`afficherSolde`, et bientôt
`afficherHistorique`, `afficherObjectifs`...). Recorriger a posteriori sur six écrans aurait
coûté six fois plus cher que de fixer la règle maintenant.

**Où tracer la limite, concrètement ?** Un contrôleur peut toujours appeler
`vuePrincipale.afficherMessage("...")` avec un texte de contrôle de flux générique et non
formaté (`"Choix invalide, veuillez recommencer."`, `"Au revoir !"`) : ce n'est pas de la mise
en forme de données métier, juste un texte fixe transmis à une méthode déjà générique de
`VueConsole`. Ce qui doit obligatoirement passer par une méthode de vue dédiée, c'est tout ce
qui dépend de données (`String.format` sur un montant, une date, une liste à parcourir) ou tout
bloc d'affichage structuré propre à un écran (le menu à neuf lignes). C'est la distinction
appliquée ici : `afficherMenuPrincipal()` et `afficherSolde(...)` existent parce qu'ils
répondaient à ce critère, pas les deux messages de contrôle de flux qui restent des appels
directs à `afficherMessage`.

### Points à savoir défendre

- **`ControleurPrincipal` contient-il encore un `System.out.println` ou une mise en forme
  d'écran ?** Non : `afficherMenu()` et le `String.format` du solde ont disparu, remplacés par
  des appels à `vuePrincipale.afficherMenuPrincipal()` et `vuePrincipale.afficherSolde(...)`.
  Les deux seuls textes qui restent dans le contrôleur (`"Choix invalide..."`, `"Au revoir !"`)
  sont des littéraux fixes, sans donnée à mettre en forme, transmis à une méthode déjà générique
  héritée de `VueConsole`.
- **Pourquoi `VuePrincipale` hérite-t-elle de `VueConsole` plutôt que de la détenir en
  attribut ?** C'est exactement le schéma annoncé au départ ("VuePrincipale... qui en
  héritent") : hériter donne accès direct à `afficherMessage`, `lireEntier` etc. sans avoir à
  écrire de méthodes de délégation une par une pour chacune.

### Pièges rencontrés

Aucun — la correction a été appliquée avant tout commit, sur la seule classe concernée.

### Reste à faire

Inchangé : `VueTransaction`/`ControleurTransaction`, `VueEpargne`/`ControleurEpargne`,
`VueCategorie`/`ControleurCategorie`, `VueStatistique`/`ControleurStatistique`, chacun câblé un
par un ; puis suppression de `Menu.java`.

## 2026-08-19 — Règle définitive : aucune chaîne destinée à l'utilisateur dans un contrôleur

### Ce qui a été écrit

- **`VuePrincipale`** : deux méthodes de plus, `afficherChoixInvalide()` et
  `afficherAuRevoir()`, qui remplacent les deux derniers littéraux qui restaient dans
  `ControleurPrincipal` (`"Choix invalide, veuillez recommencer."` et `"Au revoir !"`).
  `afficherEchecSauvegarde(String messageErreur)` ajoutée avec elles : les deux lignes
  affichées quand `ErreurSauvegardeException` est attrapée étaient elles aussi des littéraux
  dans le contrôleur, alors que la règle du jour ne prévoit aucune exception.
- **`ControleurPrincipal`** : `default -> vuePrincipale.afficherMessage("Choix invalide...")`
  devient `default -> vuePrincipale.afficherChoixInvalide()`. Le bloc `catch` et la ligne finale
  de `lancer()` appellent désormais `vuePrincipale.afficherEchecSauvegarde(erreur.getMessage())`
  et `vuePrincipale.afficherAuRevoir()`. Il ne reste plus aucune chaîne de caractères destinée à
  l'utilisateur dans la classe — seulement des appels à des méthodes de vue nommées pour ce
  qu'elles affichent.

### Choix de conception

**Règle retenue, à appliquer sans exception sur les six écrans restants (`ControleurTransaction`,
`ControleurEpargne`, `ControleurCategorie`, `ControleurStatistique`, et tout contrôleur à
venir) : un contrôleur n'écrit jamais de texte destiné à l'utilisateur, même un littéral fixe
sans donnée à mettre en forme, même passé à une méthode déjà générique comme
`afficherMessage(String)`.** Chaque message — y compris "Choix invalide", "Au revoir !", ou un
message d'erreur de sauvegarde — passe par une méthode de vue nommée pour ce qu'elle affiche
(`afficherChoixInvalide()`, `afficherAuRevoir()`, `afficherEchecSauvegarde(String)`...). Cette
entrée annule la distinction "littéral de contrôle de flux vs. donnée mise en forme" adoptée
dans l'entrée précédente : elle s'est révélée trop fine à appliquer de façon fiable sur six
écrans, et une règle sans exception est plus simple à vérifier qu'une règle à cas particuliers.

**Pourquoi `afficherEchecSauvegarde(String messageErreur)` a été ajoutée sans qu'elle ait été
demandée explicitement.** La demande ne citait que "Choix invalide" et "Au revoir !", mais la
règle énoncée dans le même message ("aucune chaîne destinée à l'utilisateur dans un contrôleur,
sans exception") s'appliquait tout aussi bien aux deux lignes du bloc `catch`, qui étaient
restées des littéraux après la correction précédente. Les laisser en l'état aurait rouvert
immédiatement la règle qu'on venait de poser dans le même fichier — autant les traiter
maintenant plutôt que d'attendre une troisième correction sur cette classe.

### Points à savoir défendre

- **Pourquoi ne pas avoir gardé la distinction "texte fixe / texte avec donnée" de l'entrée
  précédente ?** Parce qu'elle demande un jugement à chaque nouvelle ligne de code ("ce
  littéral a-t-il vraiment besoin de sa propre méthode ?"), un jugement qui peut varier d'un
  écran à l'autre. Une règle absolue ("toujours une méthode de vue, jamais de littéral") ne
  laisse pas de zone grise : elle se vérifie d'un coup d'œil sur n'importe quel contrôleur.
- **Est-ce que ça alourdit `VuePrincipale` de méthodes très courtes (une ligne chacune) ?** Oui,
  et c'est assumé : chaque méthode reste facile à lire et à nommer, et le gain (aucune ambiguïté
  sur ce qui doit être une méthode de vue) compte plus que le nombre de méthodes.

### Pièges rencontrés

Aucun.

### Reste à faire

Inchangé : `VueTransaction`/`ControleurTransaction`, `VueEpargne`/`ControleurEpargne`,
`VueCategorie`/`ControleurCategorie`, `VueStatistique`/`ControleurStatistique`, chacun câblé un
par un, en appliquant dès l'écriture la règle "aucune chaîne dans le contrôleur" ; puis
suppression de `Menu.java`.

## 2026-08-20 — `VueTransaction` et `ControleurTransaction` : écrans "Ajouter une dépense"/"Ajouter un revenu"

### Ce qui a été écrit

- **`VueTransaction`** (nouvelle classe, `vue`, hérite de `VueConsole`) : affichage et saisies
  propres à ces deux écrans — `afficherAucuneCategorieActive(TypeTransaction)`,
  `demanderCategorie(List<Categorie>)` (numérote, lit, boucle tant que le numéro est hors
  limites), `demanderDescription(String)`, `afficherRecapitulatif(...)`,
  `afficherAvertissementSoldeNegatif(double)`, `afficherOperationAnnulee()`,
  `afficherDepenseEnregistree()`, `afficherRevenuEnregistre()`.
- **`ControleurTransaction`** (nouvelle classe, `controleur`) : `gererAjouterDepense()` et
  `gererAjouterRevenu()`, extraites de `Menu.gererAjouterDepense`/`gererAjouterRevenu`. Dépend de
  `ServiceTransaction` (enregistrement), `ServiceCategorie` (catégorie active) et
  `ServicePortefeuille` (solde après dépense, et nouvelle tentative de sauvegarde, voir
  plus bas).
- **`ServiceCategorie`** : une méthode de plus, `getCategoriesActivesDeType(TypeTransaction)` —
  les catégories actives d'un type donné, nécessaire à `ControleurTransaction` pour proposer le
  bon choix (règle "catégorie cohérente"). Même logique de naissance incrémentale que les
  services précédents : ajoutée parce que cet écran en avait besoin, pas avant.
- **`VueConsole`** : trois méthodes génériques de plus —
  `demanderNouvelleTentativeSauvegarde(String)`, `afficherSauvegardeReussie()`,
  `afficherSauvegardeAbandonnee()`. Placées ici plutôt que dans `VueTransaction` : le mécanisme
  de nouvelle tentative après un échec de sauvegarde ne concerne pas que les transactions, tous
  les écrans qui modifient réellement les données (épargne, catégories) en auront besoin plus
  tard.
- **`ControleurPrincipal`** : reçoit `ControleurTransaction` par le constructeur ; les cases 2 et
  3 du switch délèguent à `controleurTransaction.gererAjouterDepense()`/`gererAjouterRevenu()`
  au lieu d'afficher "en cours de migration". Commentaire d'en-tête mis à jour en conséquence.
- **`Main`** : construit `ServiceCategorie`, `ServiceTransaction`, `VueTransaction`,
  `ControleurTransaction`, et les relie à `ControleurPrincipal`.

### Choix de conception

**La question posée avant de coder : que faire d'un échec de sauvegarde sur un écran qui modifie
réellement les données ?** Décision prise avec l'utilisateur du projet : proposer une nouvelle
tentative, sans jamais bloquer l'utilisateur s'il refuse. Concrètement, `ServiceTransaction`
applique déjà l'opération en mémoire *avant* d'appeler `servicePortefeuille.sauvegarder()` — si
cet appel échoue, la transaction existe déjà dans la liste du `Portefeuille`. `sauvegarder()` est
donc une opération **idempotente** : la rappeler ne fait que réécrire l'état courant sur le
disque, sans rejouer l'ajout. `ControleurTransaction.confirmerNouvelleSauvegarde(...)` boucle
sur `vueTransaction.demanderNouvelleTentativeSauvegarde(...)` et rappelle directement
`servicePortefeuille.sauvegarder()` (jamais `ajouterDepense`/`ajouterRevenu`, ce qui créerait un
doublon) tant que l'utilisateur accepte. S'il refuse, `afficherSauvegardeAbandonnee()` s'affiche
et l'application continue normalement — pas de blocage, la décision explicitement écartée
(risque de boucle sans issue si le disque reste inaccessible durablement, dans une appli console
sans thread pour faire autre chose en attendant).

**Pourquoi `demanderNouvelleTentativeSauvegarde`/`afficherSauvegardeReussie`/`afficherSauvegardeAbandonnee`
vivent dans `VueConsole` et pas dans `VueTransaction`.** Ce mécanisme n'a rien de spécifique aux
transactions : `ControleurEpargne` et `ControleurCategorie` en auront besoin exactement de la
même façon dès qu'ils modifieront réellement des données. `VueConsole` est justement l'endroit
"briques réutilisées par toutes les vues" (cf. son propre commentaire de classe) — l'y placer
maintenant évite de dupliquer ces trois méthodes dans chaque vue d'écran à venir.

**Pourquoi le contrôleur peut appeler `vueTransaction.confirmer("Confirmer l'enregistrement de
cette dépense ?")` avec un littéral, sans violer la règle "aucune chaîne dans le contrôleur" ?**
Cette règle (posée dans l'entrée du 2026-08-19) vise les méthodes d'affichage de sortie
(`afficherXxx`), pas les méthodes de saisie génériques de `VueConsole`
(`lireEntier`/`lireMontant`/`lireDate`/`confirmer`), qui prennent un texte de prompt en
paramètre par construction — exactement comme `ControleurPrincipal` le fait déjà avec
`vuePrincipale.lireEntier("Votre choix : ")`. Étendre la règle à ces prompts aurait obligé à
créer une méthode de vue dédiée pour chaque question posée à l'utilisateur, ce qui n'apporte
rien : le texte n'est ni calculé ni réutilisé ailleurs, juste un prompt d'entrée comme un autre.

**Pourquoi `gererAjouterDepense` et `gererAjouterRevenu` restent deux méthodes séparées, sans
extraire un helper commun malgré leur ressemblance.** C'est déjà la structure de `Menu.java` (qui
ne les fusionnait pas). Les deux écrans se distinguent par plus que leur verbe (l'avertissement
de solde négatif n'existe que pour la dépense, le service appelé diffère) ; un helper générique
aurait dû prendre un paramètre pour ces différences, ce qui aurait été moins lisible qu'écrire
les deux méthodes en clair, ligne par ligne — le critère "je dois pouvoir défendre chaque ligne"
prime ici sur la déduplication.

### Points à savoir défendre

- **Où est implémentée la règle "dépense > solde disponible autorisée avec avertissement" ?**
  Dans `ControleurTransaction.gererAjouterDepense()` (`if (soldeApres < 0)`), pas dans la vue :
  le tableau des règles de gestion du `CLAUDE.md` place cette règle dans "Contrôleur". La vue ne
  fait qu'afficher un montant qu'on lui donne, elle ne décide jamais si l'avertissement doit
  sortir.
- **Que se passe-t-il si la sauvegarde échoue deux fois de suite ?** La boucle continue de
  proposer une nouvelle tentative tant que l'utilisateur répond "oui" : chaque échec remplace le
  message affiché par celui de la nouvelle exception, sans jamais perdre la transaction déjà en
  mémoire.
- **Pourquoi `servicePortefeuille.sauvegarder()` est-il rappelable sans risque, contrairement à
  `serviceTransaction.ajouterDepense(...)` ?** Parce que `sauvegarder()` ne fait qu'écrire l'état
  courant du `Portefeuille` sur le disque (aucune modification de données), alors que
  `ajouterDepense` créerait une deuxième transaction identique si on la rappelait.

### Pièges rencontrés

**Deux `Scanner(System.in)` en même temps.** `VueConsole` créait un `new Scanner(System.in)` par
instance, dans son constructeur — sans conséquence tant qu'une seule vue existait
(`VuePrincipale`). Avec `VueTransaction` comme deuxième vue instanciée dans `Main`, deux `Scanner`
se sont retrouvés ouverts sur la même entrée standard. Repéré concrètement en testant l'écran :
`demanderCategorie` levait `NoSuchElementException: No line found` alors que l'entrée fournie
contenait bien la ligne attendue — un `Scanner` avait bufferisé par avance des lignes destinées à
l'autre. Corrigé en rendant le champ `scanner` de `VueConsole` **statique**, partagé par toutes
les vues qui en héritent, puisque `System.in` est un flux unique pour tout le programme. Ce
correctif profite à toutes les vues à venir (`VueEpargne`, `VueCategorie`,
`VueStatistique`) sans qu'elles aient à s'en soucier.

### Reste à faire

`VueEpargne`/`ControleurEpargne`, `VueCategorie`/`ControleurCategorie`,
`VueStatistique`/`ControleurStatistique`, chacun câblé un par un ; puis suppression de
`Menu.java`. Signalé mais non traité : `GestionnaireFichier.sauvegarder()` n'écrit pas de façon
atomique (pas de fichier `.tmp` puis renommage), alors que le `CLAUDE.md` l'exige — une coupure
en pleine écriture pourrait corrompre `portefeuille.json`. Hors périmètre de cette étape, à
traiter séparément.

## 2026-08-20 — Écran "Voir l'historique des transactions" : consultation, filtres, modification, suppression

### Ce qui a été écrit

- **`VueTransaction`** : six méthodes de plus — `afficherMenuHistorique()` et
  `afficherMenuModifierSupprimer()` (sous-menus à plusieurs lignes, sur le même modèle que
  `VuePrincipale.afficherMenuPrincipal()`), `afficherTransactions(List<Transaction>)` (affiche
  chaque transaction via son `toString()`, ou un message dédié si la liste est vide),
  `demanderCategorieParmiToutes()` (délègue à `demanderCategorie(...)` déjà existante, avec
  `List.of(Categorie.values())` au lieu des seules catégories actives), `demanderType()`,
  `afficherTransactionModifiee()`, `afficherTransactionSupprimee()`. Aucune nouvelle classe :
  ces méthodes rejoignent `VueTransaction`, qui couvre maintenant les trois écrans du domaine
  transaction.
- **`ControleurTransaction`** : `gererHistorique()` (nouvelle méthode publique, extraite de
  `Menu.gererHistorique`) et `gererModificationSuppressionTransaction()` (privée, extraite de
  `Menu.gererModificationSuppressionTransaction`), qui réutilise le
  `confirmerNouvelleSauvegarde(...)` déjà écrit pour les écrans 2 et 3.
- **`ControleurPrincipal`** : le `case 4` du switch délègue à
  `controleurTransaction.gererHistorique()` au lieu d'afficher "en cours de migration".

### Choix de conception

**Pourquoi ne pas créer `VueHistorique`/`ControleurHistorique` séparés ?** Consigne explicite
pour cette étape : réutiliser `VueTransaction`/`ControleurTransaction` plutôt que multiplier les
classes pour un même domaine. Les trois écrans (ajouter une dépense, ajouter un revenu, gérer
l'historique) manipulent tous des `Transaction` via `ServiceTransaction` ; les séparer en
classes distinctes aurait dupliqué `demanderDescription`, `demanderCategorie`,
`confirmerNouvelleSauvegarde` sans bénéfice.

**`gererModificationSuppressionTransaction` garde un seul bloc `try/catch` à deux niveaux
d'exception, plutôt que découpé en deux méthodes privées (une pour modifier, une pour
supprimer).** C'est fidèle à la structure de `Menu.java`, qui traitait déjà les deux cas
(`choix == 1` / `choix == 2`) dans le même bloc `try`. Deux familles d'exceptions à traiter
différemment : `ErreurSauvegardeException` déclenche la nouvelle tentative
(`confirmerNouvelleSauvegarde`, réutilisée telle quelle) ; `IllegalArgumentException` (id
inconnu) et `IllegalStateException` (catégorie inactive) affichent l'erreur lisible via
`vueTransaction.afficherErreur(...)`, déjà présente dans `VueConsole` depuis le début — aucune
nouvelle méthode nécessaire pour ce cas.

**`afficherTransactions` décide elle-même d'afficher "Aucune transaction à afficher." si la
liste est vide.** Ce n'est pas une règle de gestion (rien dans le tableau du `CLAUDE.md` ne
porte sur ce cas), juste un choix de présentation d'une liste : contrairement à
"dépense > solde" (règle explicitement assignée au contrôleur), ici il n'y a pas de décision
métier à extraire, seulement une mise en forme conditionnelle, à la même place que dans
`Menu.afficherTransactions` d'origine.

### Points à savoir défendre

- **Pourquoi `demanderCategorieParmiToutes()` propose-t-elle des catégories inactives, alors que
  l'ajout d'une dépense/d'un revenu (écrans 2 et 3) ne montre que les actives ?** Parce qu'on
  filtre ou modifie ici des transactions déjà enregistrées, potentiellement avec une catégorie
  désactivée depuis (règle de gestion : "Désactivation d'une catégorie : sans effet sur les
  transactions existantes"). Restreindre aux catégories actives aurait rendu impossible de
  retrouver ou corriger une transaction dans ce cas.
- **La suppression d'une transaction rejoue-t-elle un risque de doublon en cas de nouvelle
  tentative de sauvegarde ?** Non : comme pour l'ajout, `confirmerNouvelleSauvegarde` ne rappelle
  jamais `serviceTransaction.supprimerTransaction(id)` (qui chercherait de nouveau la
  transaction, déjà retirée de la liste), seulement `servicePortefeuille.sauvegarder()` — la
  suppression a déjà eu lieu en mémoire au moment où l'exception est levée.

### Pièges rencontrés

Aucun cette fois : le correctif du `Scanner` statique (entrée précédente) a suffi, testé à la
main sur tous les chemins (tout afficher, filtre date/catégorie/type, modification, suppression,
id inconnu, catégorie inactive, annulation par id à 0) sans reproduire le problème de saisies
perdues.

### Reste à faire

`VueEpargne`/`ControleurEpargne`, `VueCategorie`/`ControleurCategorie`,
`VueStatistique`/`ControleurStatistique`, chacun câblé un par un ; puis suppression de
`Menu.java`. Le défaut de sauvegarde non atomique dans `GestionnaireFichier` reste non traité
(voir entrée précédente).

## 2026-08-20 — Correction : catégories actives seulement à la modification d'une transaction

### Ce qui a été écrit

- **`ServiceTransaction`** : `getTransaction(int id)`, méthode publique qui délègue à la
  recherche interne déjà existante (`trouverTransaction`, restée privée). Nécessaire pour que
  `ControleurTransaction` connaisse le type de la transaction avant de choisir quelles
  catégories proposer.
- **`ControleurTransaction.gererModificationSuppressionTransaction()`** : au choix "Modifier",
  récupère d'abord le type de la transaction (`serviceTransaction.getTransaction(id).getType()`),
  vérifie qu'il existe au moins une catégorie active de ce type (sinon
  `vueTransaction.afficherAucuneCategorieActive(type)` et retour, même garde que pour l'ajout
  d'une dépense/d'un revenu), puis appelle `vueTransaction.demanderCategorie(...)` avec
  `serviceCategorie.getCategoriesActivesDeType(type)` au lieu de `demanderCategorieParmiToutes()`.

### Choix de conception

**Signalé par l'utilisateur du projet en relisant le journal : la modification proposait toutes
les catégories, y compris inactives, un choix que le service refuse ensuite.** Avant cette
correction, `demanderCategorieParmiToutes()` était utilisée aussi bien pour filtrer (case 3) que
pour modifier (case 5 → Modifier) — la même méthode, pour deux besoins différents. Pour filtrer,
voir les catégories inactives a du sens (retrouver une transaction enregistrée avec une
catégorie désactivée depuis). Pour modifier, ça n'en a aucun : `ServiceTransaction.modifierTransaction`
appelle `validerCategorieActive`, donc choisir une catégorie inactive aboutit systématiquement à
une `IllegalStateException`, après que l'utilisateur a déjà saisi le montant, la date et la
description. Autant ne jamais proposer ce choix.

**Pourquoi restreindre aux catégories actives *du même type* que la transaction, pas à toutes
les catégories actives ?** Le type d'une transaction ne se modifie jamais (`Transaction` n'a pas
de `setType`) : `setCategorie` vérifie que la nouvelle catégorie correspond toujours au type
d'origine (règle "catégorie cohérente"). Proposer une catégorie active mais du mauvais type
aboutirait cette fois à une `IllegalArgumentException` — même problème, réglé de la même façon.

**Pourquoi vérifier qu'il existe au moins une catégorie active de ce type avant d'appeler
`demanderCategorie`, plutôt que de la laisser gérer une liste vide ?** `demanderCategorie`
boucle tant qu'aucun numéro valide n'est saisi ; avec une liste vide, aucun numéro ne peut jamais
être valide, ce qui bloque l'utilisateur dans une boucle sans issue. Le cas est rare (il faudrait
avoir désactivé après coup toutes les catégories du type concerné) mais réel, donc traité avec
la même garde que celle déjà écrite pour l'ajout d'une dépense/d'un revenu.

### Points à savoir défendre

- **Pourquoi le filtre par catégorie (case 3) continue-t-il d'utiliser
  `demanderCategorieParmiToutes()`, sans la même restriction ?** Parce que filtrer et modifier ne
  cherchent pas la même chose : filtrer sert à retrouver des transactions existantes, quelle que
  soit la catégorie sous laquelle elles ont été enregistrées (active ou non aujourd'hui) ;
  modifier sert à choisir une nouvelle catégorie, qui doit être valide pour être acceptée.
  Restreindre le filtre aux catégories actives aurait rendu impossible de retrouver une
  transaction dont la catégorie a été désactivée depuis.
- **`ControleurTransaction` fait-il un calcul en appelant `.getType()` sur la transaction
  récupérée ?** Non : c'est un accès à un attribut déjà calculé/stocké via un getter, pas un
  calcul. La règle "aucun calcul dans le contrôleur" porte sur les opérations qui combinent ou
  agrègent des données (comme `soldeApres < 0`, comparaison faite plus haut dans ce même
  contrôleur) — lire un champ pour décider quel service appeler ensuite reste de l'enchaînement,
  pas du calcul métier.

### Pièges rencontrés

Aucun — testé à la main : modification avec deux catégories actives du bon type (proposées
seules, sans les sept autres), et cas limite d'aucune catégorie active du type concerné (message
clair, retour immédiat, pas de blocage).

### Reste à faire

Inchangé : `VueEpargne`/`ControleurEpargne`, `VueCategorie`/`ControleurCategorie`,
`VueStatistique`/`ControleurStatistique`, puis suppression de `Menu.java`. Le défaut de
sauvegarde non atomique dans `GestionnaireFichier` reste non traité.

## 2026-08-20 — Écran "Gérer mes objectifs d'épargne" : création, contribution, retrait, consultation, suppression

### Ce qui a été écrit

- **`ServiceEpargne`** : deux méthodes de plus, `getObjectifs()` et `getObjectif(int idObjectif)`
  (délèguent toutes les deux à `servicePortefeuille.getDonnees()`) — nécessaires pour que
  `ControleurEpargne` puisse afficher la liste des objectifs et récupérer celui choisi par
  l'utilisateur, sans jamais détenir `Portefeuille` lui-même. Message de
  `contribuerObjectif` corrigé : il rappelle maintenant le solde disponible
  (`"... (150000.0 FCFA)."`), comme demandé — il ne le faisait pas encore, seul le montant
  restant était déjà rappelé côté `supprimerObjectif`.
- **`VueEpargne`** (nouvelle classe, `vue`, hérite de `VueConsole`) : affichage et saisies
  propres à cet écran — menu, ligne de progression d'un objectif (montant actuel et pourcentage
  reçus en paramètres, jamais recalculés dans la vue), détail des mouvements, récapitulatifs de
  création/contribution/retrait, `lireDateLimite(...)` (variante facultative de `lireDate()`,
  future autorisée), messages de confirmation.
- **`ControleurEpargne`** (nouvelle classe, `controleur`) : `gererObjectifsEpargne()` aiguille
  vers cinq méthodes privées (`gererCreationObjectif`, `gererContribution`, `gererRetrait`,
  `gererConsultationObjectifs`, `gererSuppressionObjectif`), extraites de
  `Menu.gererObjectifsEpargne`. Dépend de `ServiceEpargne` et `ServicePortefeuille` (solde,
  sauvegarde).
- **`VueConsole`** : `afficherOperationAnnulee()` déplacée depuis `VueTransaction` — devenue
  générique dès qu'un deuxième écran (épargne) en a eu besoin à l'identique.
- **`ControleurPrincipal`**/**`Main`** : option 5 branchée sur `ControleurEpargne`.

### Choix de conception

**`ServiceEpargne.getObjectifs()`/`getObjectif(id)` : pourquoi les ajouter maintenant plutôt que
de continuer avec `ServicePortefeuille.getDonnees()` directement ?** Cette dernière est à
visibilité de paquet, réservée à `modele.service` : `ControleurEpargne`, dans `controleur`, ne
peut pas y accéder (ce que la migration précédente avait explicitement verrouillé). Comme pour
`ServiceTransaction.getTransaction(id)` (entrée précédente), le contrôleur a besoin d'un point
d'accès public porté par le service du domaine concerné.

**Le solde disponible, dans le message de refus d'une contribution, corrigé pour être rappelé.**
Demandé explicitement pour cet écran. Avant cette étape, seul `supprimerObjectif` rappelait une
valeur dans son message d'erreur (le montant restant) ; `contribuerObjectif` se contentait de
"dépasse le solde disponible.", sans le chiffre. Corrigé pour rester cohérent avec la règle du
`CLAUDE.md` et avec le style déjà en place dans `supprimerObjectif` (concaténation brute, pas de
`String.format`, comme la ligne voisine déjà écrite pour l'objectif non vide).

**`afficherListeObjectifs()` (dans `ControleurEpargne`) boucle sur les objectifs et appelle
`vueEpargne.afficherObjectif(objectif, montantActuel, pourcentage)` un par un, plutôt que de
passer toute la liste à la vue comme `VueTransaction.afficherTransactions(List)` le fait pour les
transactions.** Différence assumée : une transaction s'affiche avec son propre `toString()`, sans
donnée externe. Un objectif, lui, a besoin de deux valeurs calculées par `ServiceEpargne`
(montant actuel, pourcentage) que la vue ne peut pas calculer elle-même (elle n'importe jamais
`modele.service`). Le contrôleur doit donc aller chercher ces deux valeurs avant de les
transmettre — la boucle vit côté contrôleur pour cette raison précise, pas par choix de style.

**`afficherOperationAnnulee()` déplacée de `VueTransaction` vers `VueConsole`.** Elle existait
déjà mot pour mot dans `VueTransaction` ; `ControleurEpargne` en avait besoin à l'identique pour
les quatre opérations de cet écran qui demandent confirmation. Plutôt que de la dupliquer dans
`VueEpargne`, elle rejoint `demanderNouvelleTentativeSauvegarde`/`afficherSauvegardeReussie`/
`afficherSauvegardeAbandonnee` (entrée du 2026-08-20 sur les écrans 2/3) dans `VueConsole` : un
comportement identique nécessaire à deux écrans devient une brique commune. Aucun changement de
comportement sur les écrans déjà migrés (2, 3, 4) : `VueTransaction` en hérite désormais au lieu
de la définir elle-même.

**Deux méthodes de récapitulatif distinctes (`afficherRecapitulatifContribution`/
`afficherRecapitulatifRetrait`) plutôt qu'une seule avec un mot ("vers"/"de") passé en
paramètre.** Un paramètre du genre `preposition` aurait fait construire une partie du texte
affiché depuis le contrôleur — exactement ce que la règle "aucune chaîne destinée à l'utilisateur
dans le contrôleur" interdit. Deux méthodes, chacune avec son texte fixe, gardent toute la phrase
côté vue.

### Points à savoir défendre

- **Où est vérifiée la règle "contribution > solde disponible" ?** Dans
  `ServiceEpargne.contribuerObjectif`, pas dans le contrôleur : `ControleurEpargne` ne fait que
  transmettre montant et id, c'est le service qui compare au solde (obtenu lui-même auprès de
  `ServicePortefeuille`) et lève l'exception. Le contrôleur, lui, se contente d'afficher le solde
  actuel avant la saisie (information, pas une vérification) et de signaler un dépassement de
  cible (`serviceEpargne.depasseraCible(...)`, un appel de service, pas un calcul).
- **Pourquoi `gererConsultationObjectifs` peut-elle afficher "Aucun mouvement pour le moment"
  après avoir déjà affiché la liste des objectifs ?** Deux vérifications différentes : la liste
  des objectifs peut être non vide (on peut choisir un id) alors que l'objectif choisi n'a encore
  aucun mouvement (montant actuel à 0, cas d'un objectif tout juste créé) — les deux messages
  vides (`afficherAucunObjectif`, dans `afficherMouvements`) répondent chacun à leur propre
  liste.
- **La suppression d'un objectif peut-elle créer un doublon en cas de nouvelle tentative de
  sauvegarde ?** Non, même raisonnement que pour les transactions : `confirmerNouvelleSauvegarde`
  ne rappelle jamais `serviceEpargne.supprimerObjectif(id)`, seulement
  `servicePortefeuille.sauvegarder()` — l'objectif est déjà retiré de la liste en mémoire au
  moment où l'exception de sauvegarde est levée.

### Pièges rencontrés

Aucun — testé à la main : création, contribution (dans la limite du solde, avec dépassement de
cible signalé puis confirmé), contribution refusée (solde insuffisant, message avec le solde
rappelé), retrait valide, retrait refusé (montant > épargné), consultation de la progression et
du détail des mouvements, suppression refusée (objectif non vide, montant restant rappelé) puis
acceptée une fois l'objectif vidé par retrait.

### Reste à faire

`VueCategorie`/`ControleurCategorie`, `VueStatistique`/`ControleurStatistique`, chacun câblé un
par un ; puis suppression de `Menu.java`. Le défaut de sauvegarde non atomique dans
`GestionnaireFichier` reste non traité.

## 2026-08-20 — Précision : `getObjectif(id)` s'appuie sur une propriété de fait, pas structurelle

### Choix de conception

Question posée après coup sur l'écran épargne : `ServiceEpargne.getObjectif(id)` renvoie une
référence directe sur l'`Epargne` du portefeuille (pas une copie) à `ControleurEpargne` — est-ce
que ça permettrait de la modifier en contournant le service ? Vérifié dans le code : non,
aujourd'hui. `ControleurEpargne` n'appelle que des getters sur cette référence (`getNom()`,
`getMouvements()`), et `Epargne` n'expose aucun setter — sa seule méthode de mutation,
`ajouterMouvement(MouvementEpargne)`, n'est appelée nulle part hors de `ServiceEpargne`.

**Mais cette absence de risque tient uniquement à l'absence de setters sur `Epargne`
aujourd'hui — ce n'est pas une protection structurelle**, contrairement à l'accès à
`Portefeuille` : celui-ci est verrouillé par la visibilité de paquet de
`ServicePortefeuille.getDonnees()` (le compilateur refuse la compilation si un contrôleur essaie
d'y accéder, cf. entrée du 2026-08-19 sur `getDonnees()`). Ici, rien n'empêcherait
`ControleurEpargne` d'appeler un setter sur l'`Epargne` reçue si `Epargne` en exposait un un
jour : le compilateur laisserait passer, puisque `Epargne` (dans `modele.entite`) est
accessible depuis `controleur`.

### Points à savoir défendre

- **Si un setter est ajouté un jour à `Epargne` (par exemple pour renommer un objectif), que se
  passe-t-il ?** `ControleurEpargne` pourrait alors modifier l'objectif directement via la
  référence obtenue par `getObjectif(id)`, sans passer par `ServiceEpargne` ni déclencher
  `sauvegarder()` — la modification resterait en mémoire pour la session en cours, mais
  disparaîtrait au prochain chargement du fichier, sans qu'aucune règle de gestion n'ait été
  vérifiée au passage.
- **Pourquoi ne pas corriger ça dès maintenant, par exemple en ne renvoyant plus l'`Epargne`
  elle-même ?** Parce qu'il n'y a rien à corriger tant qu'aucun setter n'existe : ce serait de la
  protection contre un risque qui n'existe pas encore, pour une méthode qui aujourd'hui ne fait
  que lire. La bonne réaction est de revoir `getObjectif(id)` au moment où un setter serait
  effectivement ajouté à `Epargne`, pas avant.

### Reste à faire

Si `Epargne` reçoit un setter un jour, revoir l'exposition de `ServiceEpargne.getObjectif(id)` :
par exemple ne plus renvoyer l'`Epargne` elle-même aux contrôleurs, ou n'autoriser sa
modification que via des méthodes dédiées de `ServiceEpargne`, sur le modèle de
`ServicePortefeuille.getDonnees()`.

## 2026-08-20 — Écran "Gérer mes catégories" : consultation, activation, désactivation

### Ce qui a été écrit

- **`ServiceCategorie`** : trois méthodes de plus — `getCategoriesActives()` (délègue à
  `servicePortefeuille.getDonnees()`, pour l'affichage), `activerCategorie(Categorie)` et
  `desactiverCategorie(Categorie)`. Ces deux dernières appellent la méthode structurelle
  correspondante de `Portefeuille` (ajout/retrait dans un `Set`, inchangées), puis
  `servicePortefeuille.sauvegarder()`.
- **`VueCategorie`** (nouvelle classe, `vue`, hérite de `VueConsole`) : menu, ligne de résumé des
  catégories actives (construite avec les libellés, séparés par des virgules, plutôt que le
  `toString()` brut du `Set` que `Menu.java` affichait), sélection numérotée d'une catégorie
  parmi une liste reçue (boucle tant que le numéro n'est pas valide), messages de statut.
- **`ControleurCategorie`** (nouvelle classe, `controleur`) : `gererCategories()`, extraite de
  `Menu.gererCategories`, avec la nouvelle tentative de sauvegarde en cas d'échec.
- **`ControleurPrincipal`**/**`Main`** : option 6 branchée.

### Choix de conception

**`activerCategorie`/`desactiverCategorie` migrées vers `ServiceCategorie`, déclenchent
maintenant `sauvegarder()` — elles ne le faisaient pas avant cette étape.** Un trou de
sauvegarde resté ouvert depuis le début de la migration (signalé dans l'entrée du 2026-08-19
"Correction... Portefeuille devient une entité pure" : ces deux méthodes étaient restées dans
`Portefeuille` "pas demandées pour cette étape", et `Menu.gererCategories` ne sauvegardait
jamais après les avoir appelées). Ce n'était pas visible en cours de session (les données
restaient correctes en mémoire), mais activer ou désactiver une catégorie ne survivait pas à un
redémarrage. Cette étape le referme : `ControleurCategorie` ne pouvait de toute façon plus
appeler `Portefeuille.activerCategorie` directement (pas de référence sur `Portefeuille`), donc
migrer ces deux méthodes vers `ServiceCategorie` était obligatoire pour brancher l'écran — et
comme tous les autres services qui modifient le portefeuille, la sauvegarde va avec.

**La sélection d'une catégorie boucle maintenant jusqu'à un numéro valide, alors que
`Menu.gererCategories` abandonnait l'opération au premier numéro invalide
(`if (numero < 1 || numero > ...) { ...; return; }`).** Écart assumé par rapport au code
d'origine : les écrans déjà migrés (2 à 5) bouclent tous jusqu'à une saisie valide plutôt que
d'abandonner l'opération entière. Réaligner cet écran sur le même comportement, plutôt que de
reproduire l'ancien abandon immédiat, rend l'application cohérente d'un écran à l'autre — testé
à la main (numéro invalide, puis numéro valide, sans devoir relancer l'écran).

**Le résumé des catégories actives est reconstruit avec les libellés plutôt que d'afficher le
`Set<Categorie>` brut.** `Menu.gererCategories` affichait
`"Catégories actives : " + portefeuille.getCategoriesActives()`, ce qui imprime les noms de
constantes Java (`[ALIMENTATION, SALAIRE]`) plutôt que les libellés utilisés partout ailleurs
dans l'application (`Alimentation`). Reconstruit avec une boucle simple dans `VueCategorie`
plutôt que de reproduire cet affichage brut, pour rester cohérent avec le reste de l'écran (menu
numéroté avec les mêmes libellés).

### Points à savoir défendre

- **Où est vérifiée la règle "la désactivation n'affecte pas les transactions déjà
  enregistrées" ?** Elle n'est vérifiée nulle part explicitement, parce qu'elle est garantie par
  construction : `Portefeuille.desactiverCategorie` ne touche qu'à `categoriesActives` (un
  `Set`), jamais à la liste `transactions`. Testé à la main : une transaction enregistrée avec
  "Alimentation" garde cette catégorie après la désactivation d'"Alimentation".
- **Pourquoi `ServiceCategorie.getCategoriesActives()` renvoie-t-elle directement le `Set` de
  `Portefeuille`, sans le recopier ?** Parce que `Portefeuille.getCategoriesActives()` le renvoie
  déjà via `Collections.unmodifiableSet(...)` : le recopier aurait été redondant, la protection
  contre la modification existe déjà à la source.
- **`ControleurCategorie` ne contient qu'un seul `try/catch(ErreurSauvegardeException)`, sans
  `IllegalArgumentException`/`IllegalStateException` comme les autres contrôleurs — pourquoi ?**
  Parce qu'aucune règle de gestion ne peut être violée sur cet écran : la catégorie proposée
  vient toujours d'une liste déjà filtrée par le service (disponibles ou actives), jamais d'une
  saisie libre. `Menu.gererCategories` d'origine n'avait pas non plus ce genre de `try/catch`,
  pour la même raison.

### Pièges rencontrés

Aucun — testé à la main : activation, désactivation, numéro invalide (boucle jusqu'à un numéro
valide), aucune catégorie disponible à activer (toutes déjà actives), aucune catégorie active à
désactiver, et vérification que la désactivation ne modifie pas une transaction existante.

### Reste à faire

`VueStatistique`/`ControleurStatistique`, puis suppression de `Menu.java`. Le défaut de
sauvegarde non atomique dans `GestionnaireFichier` reste non traité.

## 2026-08-20 — Audit : tous les points de mutation d'entité déclenchent bien une sauvegarde

### Choix de conception

Demande explicite après la correction du trou sur les catégories : vérifier qu'aucun autre point
de mutation ne s'en tire sans sauvegarde. Deux passes :

1. `grep` sur tous les mutateurs d'entité (`setMontant`/`setCategorie`/`setDate`/`setDescription`
   sur `Transaction`, `ajouterMouvement` sur `Epargne`, `ajouterTransaction`/`retirerTransaction`/
   `ajouterObjectif`/`retirerObjectif`/`activerCategorie`/`desactiverCategorie` sur `Portefeuille`)
   dans tout `src/`. Résultat : tous les appels viennent de `modele.service`, aucun depuis un
   contrôleur. Les seuls autres appels sont dans `Menu.java`, qui n'est plus référencé nulle
   part depuis que `Main` utilise `ControleurPrincipal`.
2. Relecture des dix méthodes de service qui mutent réellement des données
   (`ServiceTransaction.ajouterDepense/ajouterRevenu/modifierTransaction/supprimerTransaction`,
   `ServiceEpargne.creerObjectif/contribuerObjectif/retirerObjectif/supprimerObjectif`,
   `ServiceCategorie.activerCategorie/desactiverCategorie`) : chacune appelle
   `servicePortefeuille.sauvegarder()` après sa mutation, et chacune valide avant de muter (pas
   de mutation partielle possible en cas de refus métier).

Aucun trou trouvé cette fois : celui sur les catégories, corrigé à l'étape précédente, était le
dernier. Rien à corriger dans le code ; cette entrée trace la vérification elle-même.

### Points à savoir défendre

- **Comment être sûr qu'aucun contrôleur ne contourne un service pour muter une entité
  directement ?** Deux garanties différentes selon l'entité : pour `Portefeuille`, le
  compilateur l'empêche (`ServicePortefeuille.getDonnees()` à visibilité de paquet, entrée du
  2026-08-19) ; pour `Transaction` et `Epargne`, accessibles depuis `controleur`, la garantie
  est seulement observée dans le code actuel (aucun contrôleur n'appelle de setter), pas imposée
  par le compilateur — c'est exactement le point soulevé dans l'entrée précédente sur
  `getObjectif(id)`.

### Reste à faire

Inchangé : `VueStatistique`/`ControleurStatistique`, puis suppression de `Menu.java`. Le défaut
de sauvegarde non atomique dans `GestionnaireFichier` reste non traité.

## 2026-08-20 — Écran "Voir les statistiques" : dernier écran migré

### Ce qui a été écrit

- **`VueStatistique`** (nouvelle classe, `vue`, hérite de `VueConsole`) : deux méthodes —
  `afficherTotauxParCategorie(Map<Categorie, Double>)` (avec le message dédié si la période ne
  contient aucune dépense) et `afficherTotalRevenusEtDepenses(double, double)`.
- **`ControleurStatistique`** (nouvelle classe, `controleur`) : `gererStatistiques()`, extraite
  de `Menu.gererStatistiques`. Ne dépend que de `ServiceStatistique` : pas de
  `ServicePortefeuille`, puisque l'écran ne sauvegarde jamais rien.
- **`ControleurPrincipal`**/**`Main`** : option 7 branchée. Les sept écrans du menu principal ont
  désormais chacun leur contrôleur dédié.
- **`VuePrincipale.afficherFonctionnaliteIndisponible()`** supprimée : plus aucun `case` du
  switch de `ControleurPrincipal` ne l'appelait (elle ne servait qu'aux écrans pas encore
  migrés), vérifié par recherche dans tout `src/` avant suppression.

### Choix de conception

**`ControleurStatistique` ne reçoit pas `ServicePortefeuille`.** Contrairement aux six écrans
précédents, cet écran ne modifie jamais rien : les deux méthodes de `ServiceStatistique` ne font
que lire et agréger (déjà noté dans l'entrée du 2026-08-19 sur `ServiceStatistique`), donc pas de
`sauvegarder()` à appeler, pas de nouvelle tentative à prévoir, et pas besoin de la dépendance
correspondante. Un contrôleur qui ne peut structurellement pas déclencher de sauvegarde n'a pas
de raison de détenir la référence qui le permettrait.

**Pourquoi supprimer `afficherFonctionnaliteIndisponible()` maintenant plutôt que la laisser
inutilisée ?** Elle n'avait de sens que comme message temporaire pour les écrans pas encore
câblés ; une fois les sept écrans migrés, plus aucun appelant ne pouvait exister par
construction (le `switch` de `ControleurPrincipal` couvre les cas 1 à 8 un par un). La garder
aurait laissé du code mort dans une classe déjà migrée, sans utilité pour la suite.

### Points à savoir défendre

- **Où est appliquée la règle "les mouvements d'épargne sont exclus des statistiques" ?** Nulle
  part explicitement dans ce nouvel écran : elle est garantie par construction depuis la
  migration de `ServiceStatistique` (entrée du 2026-08-19), dont les deux méthodes ne parcourent
  que `getTransactions()` — les mouvements d'épargne n'y ont jamais été stockés (ils vivent dans
  `Epargne.mouvements`). Vérifié à la main : une contribution de 200000 FCFA à un objectif
  n'apparaît dans aucun des deux totaux affichés.
- **`ControleurStatistique` fait-il un calcul en extrayant `totaux[0]`/`totaux[1]` du tableau
  renvoyé par `getTotalRevenusEtDepenses` ?** Non : c'est un accès positionnel à un résultat déjà
  calculé par le service, pas une opération arithmétique. Même raisonnement que pour
  `.getType()` sur une transaction (entrée du 2026-08-20 sur la modification d'une transaction).

### Pièges rencontrés

Aucun — testé à la main : totaux par catégorie et comparaison revenus/dépenses sur une période
contenant des transactions des deux types et une contribution d'épargne (exclue, comme attendu),
période sans aucune dépense (message dédié), et vérification que le fichier `portefeuille.json`
reste strictement identique (`md5sum` avant/après) après consultation.

### Reste à faire

Les sept écrans du menu principal sont migrés. Il ne reste que la suppression de `Menu.java`,
devenue entièrement sans usage (à faire sur demande, pas avant). Le défaut de sauvegarde non
atomique dans `GestionnaireFichier` reste non traité.

## 2026-08-20 — `GestionnaireFichier` : sauvegarde atomique, UTF-8 explicite, chargement défensif

### Ce qui a été écrit

- **`ErreurChargementException`** (nouvelle classe, `metier`, sur le même modèle que
  `ErreurSauvegardeException`) : levée uniquement pour une vraie erreur de lecture disque
  (droits d'accès, panne), pas pour les trois autres cas défensifs ci-dessous.
- **`Portefeuille.reparerApresChargement()`** : réinitialise `transactions`, `categoriesActives`
  et `objectifs` à des collections vides s'ils sont `null`. Publique (obligatoire :
  `GestionnaireFichier`, dans `persistance`, ne peut pas accéder à des champs privés d'un autre
  paquet), mais n'a de sens qu'appelée juste après une désérialisation Gson.
- **`GestionnaireFichier.sauvegarder()`** : écrit maintenant dans `portefeuille.json.tmp`
  (`Files.newBufferedWriter`, UTF-8 explicite), puis renomme ce fichier temporaire vers
  `portefeuille.json` avec `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
- **`GestionnaireFichier.charger()`** : réécrite pour traiter les quatre cas défensifs demandés
  — fichier absent (déjà géré), fichier vide (`gson.fromJson` renvoie `null`, détecté et remplacé
  par un portefeuille neuf), JSON malformé (`JsonSyntaxException` attrapée, même traitement),
  listes à `null` après désérialisation (`reparerApresChargement()` appelée avant de renvoyer le
  résultat). Lecture aussi passée en UTF-8 explicite (`Files.newBufferedReader`). Seule une vraie
  `IOException` (pas les trois cas ci-dessus) lève `ErreurChargementException`.
- **`Main`** : `VuePrincipale` construite avant le chargement, pour pouvoir attraper
  `ErreurChargementException` et afficher un message lisible (`vuePrincipale.afficherErreur(...)`)
  avant d'arrêter proprement, plutôt que de laisser une trace d'exception brute empêcher le
  démarrage.

### Choix de conception

**`Files.move(..., ATOMIC_MOVE)` plutôt que `File.renameTo()`.** `renameTo()` renvoie un simple
`boolean` sans dire pourquoi il a échoué, et ne garantit rien sur l'atomicité selon les systèmes.
`Files.move` avec `ATOMIC_MOVE` est l'outil standard de `java.nio.file` pour cette garantie
précise : soit le renommage a lieu en entier, soit il échoue en entier (et lève une exception
explicite) — jamais un état intermédiaire où `portefeuille.json` serait à moitié écrit. Écrire
d'abord dans un fichier séparé (`.tmp`) puis renommer garantit qu'une coupure pendant l'écriture
laisse le fichier `.tmp` incomplet mais ne touche jamais au fichier existant.

**Pourquoi `charger()` ne lève jamais d'exception pour un fichier vide ou un JSON malformé, mais
en lève une pour une erreur de lecture disque.** Les deux premiers cas sont réparables sans
perte réelle : un fichier vide ou corrompu ne contenait de toute façon aucune donnée exploitable,
repartir d'un portefeuille neuf est le seul choix raisonnable et rejoint la définition du
"chargement défensif" du `CLAUDE.md` ("charger() renvoie toujours un portefeuille exploitable").
Une erreur de lecture disque est différente : le fichier existe peut-être avec de vraies données
dedans, simplement inaccessibles à cet instant (droits, panne) — y répondre par un portefeuille
vide masquerait silencieusement des données réelles. C'est pour ça que ce cas-là, seul, lève une
exception que `Main` attrape explicitement.

**`reparerApresChargement()` réinitialise les collections plutôt que de rejeter tout le
portefeuille si une seule liste est `null`.** Un JSON valide mais légèrement incomplet (par
exemple un fichier de sauvegarde très ancien, avant l'ajout d'`objectifs`) contient quand même
de vraies transactions à ne pas perdre. Réinitialiser uniquement le ou les champs manquants,
plutôt que de tout jeter comme pour un JSON malformé, préserve tout ce qui a pu être lu
correctement.

### Points à savoir défendre

- **Pourquoi `reparerApresChargement()` doit-elle être publique, alors que la règle du projet est
  de garder les entités aussi fermées que possible ?** Parce que `GestionnaireFichier` (paquet
  `persistance`) et `Portefeuille` (paquet `modele.entite`) sont dans des paquets différents :
  contrairement à `ServicePortefeuille.getDonnees()` (même paquet que les autres services,
  visibilité de paquet possible), il n'existe pas de visibilité intermédiaire entre `private` et
  `public` qui couvre deux paquets différents. La méthode reste malgré tout étroite (un seul
  travail, pas d'accès en écriture arbitraire) et n'a de sens que juste après une
  désérialisation — un contrôleur qui l'appellerait n'importe où ailleurs ne casserait rien,
  juste ne servirait à rien.
- **Que se passe-t-il concrètement si l'application est interrompue (coupure de courant, `kill
  -9`) pendant `sauvegarder()` ?** Deux moments possibles : pendant l'écriture du `.tmp` (le
  fichier `portefeuille.json` d'origine n'est jamais touché, il reste valide, seul le `.tmp`
  reste incomplet et sera écrasé à la prochaine sauvegarde) ; ou pendant le renommage lui-même
  (impossible d'observer un état intermédiaire, `Files.move(..., ATOMIC_MOVE)` garantit que le
  système de fichiers voit soit l'ancien fichier, soit le nouveau, jamais un mélange).
- **Pourquoi le fichier vide et le JSON malformé sont-ils traités par le même `return new
  Portefeuille()`, alors que ce sont deux causes différentes ?** Parce que la conséquence pour
  l'utilisateur est la même dans les deux cas : aucune donnée exploitable n'a pu être lue, la
  seule réponse sensée est de repartir d'un portefeuille vide plutôt que de distinguer l'origine
  exacte du problème dans un message que personne ne pourrait de toute façon corriger à la main.

### Pièges rencontrés

Aucun — testé à la main : fichier absent, fichier vide, JSON malformé (les trois sans plantage,
application utilisable normalement ensuite), listes à `null` après désérialisation (écran
historique et écran épargne consultés sans `NullPointerException`, chacun affichant son message
"aucun(e)... pour le moment"), sauvegarde suivie d'une relecture (solde retrouvé correctement),
absence de fichier `.tmp` résiduel après une sauvegarde réussie, et caractères accentués
(`café à la crème brûlée`) préservés à l'identique dans le JSON après écriture.

### Reste à faire

Les sept écrans du menu principal sont migrés, la persistance est maintenant sûre. Il ne reste
que la suppression de `Menu.java`, devenue entièrement sans usage (à faire sur demande, pas
avant).

## 2026-08-21 — Interfaces de service (`modele.IService`) et contrôleurs typés sur les interfaces

Étape codée directement par l'étudiant (changement de méthode de travail demandé pour cette
étape) : l'assistant a fourni les signatures exactes à reproduire et relu le résultat, sans
écrire de fichier lui-même.

### Ce qui a été écrit

- **`modele.IService`** (nouveau paquet) : cinq interfaces, une par service —
  `IServiceCategorie`, `IServiceEpargne`, `IServicePortefeuille`, `IServiceStatistique`,
  `IServiceTransaction`. Chacune déclare exactement les méthodes publiques déjà existantes du
  service correspondant, recopiées telles quelles, sans ajout ni omission volontaire.
- **Les cinq classes de service** (`ServiceCategorie`, `ServiceEpargne`, `ServicePortefeuille`,
  `ServiceStatistique`, `ServiceTransaction`) déclarent désormais `implements IServiceXxx`, sans
  aucun changement de comportement.
- **Les cinq contrôleurs** (`ControleurCategorie`, `ControleurEpargne`, `ControleurPrincipal`,
  `ControleurStatistique`, `ControleurTransaction`) déclarent maintenant leurs champs et leurs
  paramètres de constructeur avec le type de l'interface (`IServiceTransaction` au lieu de
  `ServiceTransaction`, etc.) plutôt qu'avec la classe concrète.
- **`Main.java`** : aucune modification. Il continue d'instancier les classes concrètes
  (`new ServiceTransaction(...)`) ; Java accepte de les passer à un constructeur qui attend le
  type interface, puisque chaque classe l'implémente.

### Choix de conception

**Pourquoi une interface par service plutôt qu'une seule interface commune.** Chaque service a
un contrat différent (les méthodes de `IServiceEpargne` n'ont rien à voir avec celles de
`IServiceStatistique`) ; une interface unique n'aurait rien décrit de précis et aurait forcé
chaque service à "implémenter" des méthodes qui ne le concernent pas.

**Pourquoi `estActive` (`ServiceCategorie`) et `getDonnees` (`ServicePortefeuille`) ne figurent
dans aucune interface.** Les deux sont à visibilité de paquet dans leur classe (aucun
modificateur), utilisables uniquement par les autres services de `modele.service` — c'est la
protection documentée à l'entrée du 2026-08-19 contre le contournement des services depuis un
contrôleur. Une méthode d'interface Java est toujours implicitement publique : l'écrire dans
l'interface aurait forcé la méthode à devenir `public` dans la classe (Java interdit de
restreindre la visibilité d'une méthode en l'implémentant), et donc rouvert exactement le trou
que la visibilité de paquet avait fermé. Les deux méthodes existent toujours, simplement en
dehors du contrat public.

**Pourquoi `Main.java` n'a rien à changer.** Le typage par interface ne concerne que la façon
dont un contrôleur *déclare* sa dépendance, pas la façon dont l'objet est construit. `Main` reste
le seul endroit qui sait quelle implémentation concrète existe ; les contrôleurs, eux, n'en ont
plus besoin de le savoir.

### Points à savoir défendre

- **Qu'est-ce qu'une interface apporte concrètement ici, par rapport à avant ?** Un contrôleur
  qui déclare `IServiceTransaction serviceTransaction` ne peut appeler que les méthodes du
  contrat — jamais une méthode interne comme `trouverTransaction`, qui n'existe pas dans
  l'interface. La frontière entre "ce qu'un contrôleur peut faire" et "détail d'implémentation du
  service" est vérifiée par le compilateur, pas seulement par une convention.
- **Pourquoi une interface Java ne peut-elle pas déclarer de méthode `private` sans corps ?**
  `private` sur une méthode d'interface n'existe que depuis Java 9, et uniquement pour une
  méthode qui a un corps (un utilitaire interne partagé entre les méthodes `default`/`static` de
  l'interface elle-même). Une méthode abstraite (sans corps, à implémenter) est toujours
  publique : `private boolean estVide(Epargne objectif);` ne compile pas, message du compilateur
  "missing method body, or declare abstract". C'est pour ça que les méthodes privées des services
  (`estVide`, `trouverTransaction`, `validerCategorieActive`...) ne peuvent de toute façon pas
  apparaître dans une interface, indépendamment de la question d'encapsulation.
- **Le paquet s'appelle `IService` et non `eService` comme demandé au départ — est-ce un
  problème ?** Non, c'est un renommage assumé par l'étudiant en cours de route ; le contenu (une
  interface par service, limitée aux méthodes publiques) respecte la consigne, seul le nom du
  paquet et le préfixe des interfaces diffèrent.

### Pièges rencontrés

Deux erreurs de compilation réelles, corrigées après coup :

- **Import incohérent avec le paquet créé** : `ServiceCategorie.java` importait
  `modele.eService.EServiceCategorie` (nom prévu initialement) alors que le paquet réellement
  créé était `modele.IService.IServiceCategorie`. Message du compilateur : "package
  modele.eService does not exist". Corrigé en alignant l'import et le nom d'interface sur ce qui
  existait vraiment sur le disque.
- **Méthodes privées recopiées telles quelles dans les interfaces** : `estVide`,
  `validerCategorieActive` et `trouverTransaction` avaient d'abord été ajoutées à
  `IServiceEpargne`/`IServiceTransaction` avec leur modificateur `private` d'origine, sans corps
  — erreur "missing method body, or declare abstract" (voir ci-dessus). Retirées des interfaces,
  laissées uniquement dans les classes.
- **`estActive` et `getDonnees` brièvement rendues publiques** pour satisfaire une première
  version des interfaces qui les incluait. Repéré avant la validation finale et corrigé : les
  deux méthodes sont revenues à leur visibilité de paquet d'origine, retirées des interfaces.

### Reste à faire

Rien pour cette étape précise. La suppression de `Menu.java` reste la seule tâche en attente,
inchangée depuis l'entrée précédente.

## 2026-08-21 — Contrôleurs sans traitement (1/3) : `ControleurCategorie`

Consigne de la maîtresse de stage : un contrôleur ne doit faire que des appels de méthode —
aucune boucle, aucune construction ou transformation de liste, aucune correspondance entre un
numéro saisi et un objet, aucun `if` portant sur une règle métier. Relevé d'abord sur
`ControleurCategorie`, à vérifier ensuite sur les quatre autres contrôleurs.

### Ce qui a été écrit

- **`VueCategorie`** : deux méthodes, `demanderCategorieActivation(List<Categorie>)` et
  `demanderCategorieDesactivation(Set<Categorie>)`. Chacune teste si la liste/l'ensemble reçu
  est vide (et affiche alors le message dédié, en renvoyant `null`), sinon délègue à
  `demanderCategorie(...)`, déjà existante, pour numéroter et lire le choix.
- **`ControleurCategorie`** : `gererActivationCategorie()` et `gererDesactivationCategorie()`
  réduites à un appel à la nouvelle méthode de vue, un test `if (categorie == null) return;`, et
  les deux appels de service déjà en place. L'import `java.util.List`, plus utilisé, a été
  retiré.
- **Correction sans rapport avec cette étape** : les cinq classes de `modele.service`
  importaient encore `modele.IService.IServiceXxx` (majuscule), reliquat du renommage de paquet
  `IService` → `iService` fait au commit précédent mais jamais répercuté sur ces imports. La
  compilation ne passait plus du tout. Corrigé en réalignant les cinq imports sur le nom réel du
  paquet.

### Choix de conception

**Pourquoi le test de liste vide et la conversion `Set` → `List` partent dans la vue plutôt que
de rester dans le contrôleur.** Décider quel message afficher selon qu'une liste est vide ou non
est une décision de présentation, pas une règle de gestion sur les catégories ou l'argent.
Convertir un `Set` en `List` pour pouvoir numéroter les catégories à l'écran est un besoin
d'affichage (il faut un ordre indexable), pas une transformation de donnée métier. Les deux
n'avaient donc rien à faire dans le contrôleur.

**Pourquoi la vue renvoie `null` plutôt qu'un indice ou une exception.** Le principe donné pour
cette étape : "une vue qui propose une liste doit renvoyer l'objet choisi, pas un indice que le
contrôleur devrait convertir". Pour le cas où il n'y a rien à choisir, `null` est le signal le
plus simple — il existe déjà un précédent dans le projet (`VueEpargne.lireDateLimite` renvoie
`null` pour "pas de date saisie"). Le contrôleur n'a plus qu'à réagir à ce résultat avec un test
de nullité, qui n'est pas une règle métier : c'est une réaction à une décision déjà prise par la
vue, pas une inspection de liste faite par le contrôleur lui-même.

### Points à savoir défendre

- **Le `if (categorie == null) return;` qui reste dans le contrôleur n'est-il pas lui-même un
  traitement ?** Non : il ne construit rien, ne parcourt rien, ne compare aucune valeur métier.
  C'est une réaction à un résultat déjà tranché par la vue, comme un `if (choix == 1)` réagit à
  un menu déjà affiché.
- **Pourquoi avoir corrigé les imports de `modele.service` dans la même étape, alors que ce
  n'était pas demandé ?** Sans cette correction, plus rien ne compilait, y compris le travail sur
  `ControleurCategorie` : impossible de vérifier quoi que ce soit sans lever ce blocage d'abord.

### Pièges rencontrés

Le renommage de paquet `IService` → `iService` du commit précédent n'avait mis à jour que le
`package` déclaré dans les cinq interfaces elles-mêmes, pas les `import` des cinq classes de
service qui les implémentent. Sur un système de fichiers sensible à la casse (Linux), `javac`
refusait de trouver `modele.IService` : "package modele.IService does not exist". Pas détecté au
commit précédent faute d'avoir recompilé après le renommage.

### Reste à faire

`ControleurTransaction` et `ControleurEpargne`, dans cet ordre, pour le même nettoyage.

## 2026-08-21 — Contrôleurs sans traitement (2/3 et 3/3) : `ControleurConsole`, `ControleurTransaction`, `ControleurEpargne`

Deuxième relecture, avec un critère plus strict donné par l'étudiant après la première passe :
toute ligne qui n'est ni un appel de méthode, ni une affectation du résultat d'un appel, ni un
aiguillage de menu (boucle de menu, `switch` de choix, `if (id == 0)`), est un traitement à
sortir du contrôleur. Cette relecture a fait remonter un traitement manqué à l'étape précédente :
la boucle de reprise après échec de sauvegarde (`confirmerNouvelleSauvegarde`), identique dans
`ControleurCategorie`, `ControleurTransaction` et `ControleurEpargne`.

### Ce qui a été écrit

- **`ControleurConsole`** (nouvelle classe, `controleur`) : porte désormais la seule
  `confirmerNouvelleSauvegarde(ErreurSauvegardeException)`, écrite une fois. `ControleurCategorie`,
  `ControleurTransaction` et `ControleurEpargne` en héritent tous les trois et n'écrivent plus
  cette boucle chacun de leur côté — même principe que `VueConsole` côté vue.
- **`ServicePortefeuille`** : une méthode, `depenseRendraSoldeNegatif(double montant)`, qui
  calcule le seuil de la règle "dépense > solde ⇒ avertissement". `ControleurTransaction` ne fait
  plus la comparaison `soldeApres < 0` lui-même, il branche sur ce booléen — même patron que
  `ServiceEpargne.depasseraCible()`, qui existait déjà.
- **`ServiceEpargne`** : deux méthodes, `getMontantsActuels(List<Epargne>)` et
  `getPourcentagesAtteints(List<Epargne>)`, qui calculent la progression de toute une liste
  d'objectifs d'un coup (une boucle chacune, à l'intérieur du service).
- **`VueEpargne`** : une méthode, `afficherObjectifs(List<Epargne>, List<Double>, List<Double>)`,
  qui reçoit les trois listes déjà calculées, teste si la liste d'objectifs est vide, et boucle
  pour afficher chaque ligne via `afficherObjectif()` (déjà existante).
- **`ControleurTransaction`** : hérite de `ControleurConsole` (retire sa propre
  `confirmerNouvelleSauvegarde` et son champ `servicePortefeuille`, devenu hérité). Dans
  `gererAjouterDepense()`, le test devient `if (servicePortefeuille.depenseRendraSoldeNegatif(montant))`.
- **`ControleurEpargne`** : hérite de `ControleurConsole` (mêmes retraits qu'au-dessus).
  `afficherListeObjectifs()` ne boucle plus et ne teste plus la liste vide : elle appelle
  `vueEpargne.afficherObjectifs(...)` avec les trois listes, et renvoie la liste des objectifs
  (type de retour `void` → `List<Epargne>`). `gererConsultationObjectifs()` réutilise cette liste
  renvoyée au lieu de rappeler `serviceEpargne.getObjectifs()` une deuxième fois rien que pour
  retester si elle est vide.
- **`ControleurCategorie`** : hérite lui aussi de `ControleurConsole` (même retrait qu'au-dessus),
  en plus du nettoyage déjà fait à l'étape précédente.
- **`IServicePortefeuille`**, **`IServiceEpargne`** : les nouvelles méthodes publiques des
  services correspondants y sont ajoutées, comme pour toute méthode publique de service.

### Choix de conception

**Pourquoi la boucle de reprise ne pouvait pas descendre plus bas que le contrôleur.** Elle
alterne deux natures d'appel : poser une question (la vue) et retenter l'écriture (le service).
La faire porter par `ServicePortefeuille` aurait obligé le service à appeler une vue — une classe
de `modele` qui dépend d'une classe de `vue`, ce que l'architecture MVC du projet interdit
justement (le test du CLAUDE.md : "si je remplace la console par une interface graphique, est-ce
que je dois toucher à cette classe ?" — oui, pour un service qui appellerait une vue). La faire
porter par la vue aurait obligé la vue à appeler un service, ce que la règle du projet interdit
explicitement et sans exception. Le contrôleur reste donc le seul endroit possible pour cette
boucle précise ; ce qui a changé, c'est qu'elle n'est plus recopiée trois fois.

**Pourquoi `ControleurConsole` plutôt que, par exemple, passer `VueConsole` et
`IServicePortefeuille` en paramètres d'une méthode statique.** Une méthode statique partagée
existe déjà dans le projet pour un besoin comparable (`CalculEpargne`), mais elle sert un calcul
pur, sans appel à une vue ni effet de bord. Ici, il fallait un mécanisme hérité par plusieurs
contrôleurs, exactement comme `VueConsole` l'est déjà par plusieurs vues : le même patron des
deux côtés de l'architecture, plus simple à expliquer que d'introduire une deuxième façon de
partager du code.

**Pourquoi `getMontantsActuels`/`getPourcentagesAtteints` renvoient deux `List<Double>` plutôt
qu'un nouveau type qui regrouperait objectif, montant et pourcentage.** Décidé avec l'étudiant :
pas de nouveau type. Deux listes qui avancent au même index sont plus simples à lire et à
défendre que d'introduire une classe supplémentaire pour ce seul écran, même si l'association
entre les trois listes n'est pas imposée par le type — elle est assurée par construction
(`getMontantsActuels`/`getPourcentagesAtteints` parcourent la même liste `objectifs` reçue en
paramètre, dans le même ordre).

### Points à savoir défendre

- **Comment as-tu tracé la limite entre "aiguillage acceptable dans un contrôleur" et
  "traitement à en sortir" ?** Un `if` reste dans le contrôleur quand il ne fait que réagir à un
  résultat déjà entièrement calculé ailleurs : un choix de menu, un `null`/`id == 0` signalant
  "rien à faire", une confirmation oui/non de l'utilisateur, ou un booléen renvoyé par un service
  (`depasseraCible`, `depenseRendraSoldeNegatif`, `aCategorieActiveDeType`). Un `if` sort du
  contrôleur dès qu'il inspecte lui-même une donnée pour en tirer une décision (une liste vide,
  une comparaison de montant) : c'est le service ou la vue qui doit trancher, pas lui.
- **`gererConsultationObjectifs()` teste encore `objectifs.isEmpty()` — n'est-ce pas la même
  chose que ce qui vient d'être sorti d'`afficherListeObjectifs()` ?** Ce n'est pas une nouvelle
  inspection : c'est la liste déjà récupérée par l'appel précédent, réutilisée pour décider si la
  suite de l'écran (demander un identifiant) a un sens. C'est la même famille que `if (id == 0)` :
  une garde de flux, pas un calcul sur les données.
- **Pourquoi avoir touché `ServicePortefeuille` et `ServiceEpargne` alors que la consigne ne
  parlait que des contrôleurs ?** Parce que sortir un traitement d'un contrôleur veut toujours
  dire le faire atterrir quelque part : sur une donnée calculée (ici, un seuil et deux
  progressions), la destination est le service, pas la vue.

### Pièges rencontrés

Aucun cette fois — chaque étape a été vérifiée par une compilation complète avant de passer à la
suivante.

### Reste à faire

Les cinq contrôleurs respectent maintenant le critère "aiguillage seulement". Reste toujours la
suppression de `Menu.java`, inchangée depuis les entrées précédentes.

## 2026-08-21 — Contrôleurs sans traitement, dernière passe : critère à quatre catégories

Troisième et dernière relecture, avec un critère formulé sans ambiguïté par l'étudiant : dans un
contrôleur, une ligne n'est acceptée que si elle est (1) un appel de méthode sur une vue ou un
service, (2) une affectation qui reçoit directement le résultat d'un tel appel, (3) un `if`/`switch`
qui réagit à une valeur déjà obtenue d'un appel (choix de menu, booléen de service, confirmation,
résultat `null`), ou (4) la boucle du menu principal. Tout le reste — regarder dans une
collection, la parcourir, indexer un tableau, comparer des valeurs, calculer — est un traitement.
Cette relecture a fait remonter trois derniers cas, plus un quatrième examiné et jugé
indéplaçable.

### Ce qui a été écrit

- **`ServiceEpargne`** : une méthode, `aAuMoinsUnObjectif()`, qui renvoie
  `!getObjectifs().isEmpty()`. `ControleurEpargne.gererConsultationObjectifs()` teste ce booléen
  au lieu d'appeler `.isEmpty()` sur une liste qu'il aurait dû recevoir puis inspecter lui-même.
  `afficherListeObjectifs()` redevient `void` : elle n'a plus besoin de renvoyer sa liste à qui
  que ce soit.
- **`ServiceStatistique`** : `getTotalRevenusEtDepenses(...)`, qui renvoyait un `double[]`, est
  remplacée par deux méthodes séparées, `getTotalRevenus(...)` et `getTotalDepenses(...)`.
  `ControleurStatistique` ne fait plus `totaux[0]`/`totaux[1]` : indexer un tableau, c'est
  manipuler une donnée, ce qui n'a rien à faire dans un contrôleur.
- **`ControleurTransaction.gererHistorique()`** : la variable `resultat`, remplie dans quatre
  branches du `switch` puis affichée après coup, a disparu. Chaque branche appelle maintenant
  directement `vueTransaction.afficherTransactions(...)` avec son propre résultat.
- **`ControleurConsole.confirmerNouvelleSauvegarde`** : examinée pour savoir si elle pouvait
  sortir du contrôleur. Conclusion : non, et le commentaire de la classe documente maintenant
  précisément pourquoi (voir "Choix de conception"). Aucun changement de code sur cette méthode
  elle-même, seulement sur son commentaire.
- **`IServiceEpargne`**, **`IServiceStatistique`** : mises à jour avec les nouvelles/nouvelles
  signatures de méthodes publiques.

### Choix de conception

**Pourquoi `aAuMoinsUnObjectif()` plutôt que de laisser `afficherListeObjectifs()` renvoyer la
liste.** La version précédente (étape du 2/3) faisait déjà un compromis : elle évitait de
recalculer la liste, mais obligeait le contrôleur à faire `objectifs.isEmpty()` sur ce qu'elle
renvoyait — une inspection de collection, même sur une liste déjà en main. Le critère de cette
étape ne laisse plus cette marge : le contrôleur ne doit jamais regarder dans une collection, même
la sienne. La solution retenue redonne la décision au service, sous forme d'un booléen tout fait,
au prix d'un deuxième appel à `getObjectifs()` en interne (une fois dans `afficherListeObjectifs()`,
une fois dans `aAuMoinsUnObjectif()`) — un coût négligeable pour une liste d'objectifs d'épargne
d'un usage personnel, largement compensé par la clarté de la règle : le contrôleur ne fait jamais
`.isEmpty()`, point final.

**Pourquoi `getTotalRevenusEtDepenses()` a disparu plutôt que de garder le tableau et de le
transmettre tel quel à la vue.** Une des deux solutions proposées à l'étudiant. Transmettre le
tableau tel quel aurait voulu dire changer la signature de
`VueStatistique.afficherTotalRevenusEtDepenses(double, double)` en
`afficherTotalRevenusEtDepenses(double[])`, ce qui aurait déplacé le problème dans la vue au lieu
de le résoudre : la vue aurait dû, elle, indexer `totaux[0]`/`totaux[1]` pour construire ses deux
lignes de texte. Deux méthodes de service séparées, chacune renvoyant une seule valeur, évitent le
tableau des deux côtés.

**La boucle de reprise sur échec de sauvegarde, dernière vraie boucle de traitement du projet :
pourquoi elle reste dans `ControleurConsole`.** Réexaminée sérieusement à la demande explicite de
l'étudiant, avec obligation de répondre clairement si aucun déplacement n'était possible. Elle
alterne deux natures d'appel à chaque tour : poser une question (capacité d'une vue) et retenter
l'écriture (capacité d'un service), avec une décision différente selon la réponse de chacun. Trois
pistes examinées et écartées : la faire porter par `ServicePortefeuille` (un service qui
appellerait une vue, ce que la règle 1 du CLAUDE.md interdit déjà pour l'affichage/la saisie
directs, et que le test "remplacer la console par une IHM graphique" condamnerait) ; la faire
porter par la vue (une vue qui appellerait un service, interdit sans exception par la règle 2) ;
confier la retentative au service via un objet passé en paramètre — un mécanisme de rappel, qui
n'existe pas en Java de base sans lambda, classe anonyme ou interface fonctionnelle dédiée, exactement
ce que le CLAUDE.md interdit sans accord préalable. Seul un contrôleur a le droit d'appeler à la
fois une vue et un service : cette boucle n'a donc pas d'autre endroit possible dans cette
architecture. Ce qui pouvait être amélioré l'avait déjà été à l'étape précédente (elle n'est plus
écrite trois fois) ; ce qui reste ne l'est pas par oubli, mais par nécessité.

### Points à savoir défendre

- **Le contrôleur ne fait-il vraiment plus jamais `liste.isEmpty()` nulle part ?** Vérifié sur les
  cinq contrôleurs après cette étape : le seul test qui restait
  (`gererConsultationObjectifs`) est remplacé par un appel à
  `serviceEpargne.aAuMoinsUnObjectif()`.
  Aucune collection n'est plus inspectée, parcourue, ni indexée dans `controleur`.
- **`ControleurConsole.confirmerNouvelleSauvegarde` n'est-elle pas la preuve que le critère "un
  contrôleur n'enchaîne que des appels" a une limite ?** Non : la boucle *est* un enchaînement
  d'appels (poser la question, tenter la sauvegarde, poser la question à nouveau...), simplement
  répété conditionnellement. Ce n'est ni un calcul, ni un parcours de collection métier, ni une
  règle de gestion — les trois choses que le critère vise à sortir des contrôleurs.
- **Pourquoi ne pas avoir demandé la permission d'utiliser une interface fonctionnelle (un
  callback) pour sortir complètement cette boucle ?** Parce que la question a été posée à
  l'envers : introduire un mécanisme de rappel aurait ajouté de la complexité (une nouvelle
  notion, un nouveau type) pour un gain douteux — la boucle resterait quelque part, simplement
  écrite différemment, pour un concept plus difficile à défendre à l'oral qu'une boucle `while`
  ordinaire.

### Pièges rencontrés

Aucun — chaque changement a été vérifié par une compilation complète.

### Reste à faire

Les cinq contrôleurs sont maintenant conformes au critère à quatre catégories, y compris son seul
cas limite documenté (`ControleurConsole`). Reste toujours la suppression de `Menu.java`,
inchangée depuis les entrées précédentes.

## 2026-08-21 — Nouvelle architecture en couches, étape 1 : déplacement pur

Le CLAUDE.md a été mis à jour par la maîtresse de stage : nouvelle arborescence en couches
(`presentation`, `application`, `domain`, `infrastructure`, `exception`), à mettre en place en
cinq étapes. Cette entrée couvre l'étape 1 : le déplacement des fichiers existants, sans aucun
changement de comportement, de signature, ni de nouvelle classe.

### Ce qui a été écrit

Aucun fichier créé, aucun retiré, aucune méthode touchée. Seulement des déplacements et la mise à
jour des `package`/`import` qui en découle :

- `controleur/` → `presentation/controller/` (les six contrôleurs, `controleur` devient
  `presentation.controller`)
- `vue/` → `presentation/view/` (les six vues, `vue` devient `presentation.view`)
- `modele/iService/` → `application/service/interfaces/` (les cinq `IServiceXxx`, `modele.iService`
  devient `application.service.interfaces` — paquet nommé `interfaces` avec un s, `interface`
  étant un mot réservé)
- `modele/service/` → `application/service/implementation/` (les cinq `ServiceXxx` et
  `CalculEpargne`, `modele.service` devient `application.service.implementation`)
- `modele/entite/` → `domain/entity/` (les quatre entités)
- `modele/enumeration/` → `domain/enumeration/` (les trois énumérations)
- `modele/persistance/` → `infrastructure/persistence/` (`GestionnaireFichier`)
- `modele/exception/` → `exception/` (les deux exceptions, paquet racine `exception`, au même
  niveau que `presentation`/`application`/`domain`/`infrastructure`, comme demandé)
- `application/dto/` créé, vide : réservé à l'étape 3, rien dedans pour l'instant. Un dossier vide
  n'est pas suivi par Git ; il réapparaîtra dans le dépôt dès que le premier DTO y sera ajouté.
- `Main.java` reste à la racine de `src/`, ses imports mis à jour vers les nouveaux paquets.
- Un commentaire de `ControleurConsole` qui parlait encore de "classe de modele" a été corrigé
  pour refléter la nouvelle terminologie (`application`, couche service).
- Les anciens dossiers `controleur/`, `vue/`, `modele/` (et ses sous-dossiers) ont été supprimés,
  devenus vides après le déplacement.

### Choix de conception

**Pourquoi un simple `mv` plus une substitution de texte, plutôt que de retaper les fichiers.**
La consigne était stricte : déplacement pur, aucun changement de comportement. Retaper un fichier,
même sans intention de le modifier, est le genre d'étape qui introduit des fautes de frappe
invisibles à l'œil. Déplacer le fichier tel quel puis ne changer que les lignes `package`/`import`
(un remplacement de texte mécanique, identique partout où le même ancien chemin apparaît) réduit
le risque au minimum : soit le remplacement est fait partout à l'identique, soit la compilation
échoue immédiatement et le signale.

**Pourquoi les commentaires qui citent un ancien chemin de paquet ont aussi été corrigés par le
même mécanisme.** Plusieurs classes expliquent leur rôle en citant le chemin d'un paquet voisin
(par exemple "les autres services de modele.service"). Ces mentions étaient devenues fausses avec
le déplacement ; les laisser en l'état aurait rendu le journal et les commentaires trompeurs pour
la relecture. Comme la chaîne remplacée (`modele.service`, `modele.entite`...) est la même dans le
code et dans les commentaires, une seule substitution a suffi pour les deux à la fois.

**Pourquoi `application/dto/` reste un dossier physique vide plutôt que ne pas être créé du
tout.** La cible de l'étape 1 liste ce dossier explicitement, même vide : il marque la place
réservée pour l'étape 3, et évite d'avoir à se souvenir plus tard de son emplacement exact dans
l'arborescence.

### Points à savoir défendre

- **`ServicePortefeuille.getDonnees()` est-elle toujours protégée après le déplacement ?** Oui,
  vérifié concrètement : un fichier de test placé dans `presentation.controller` qui tente
  `servicePortefeuille.getDonnees()` refuse de compiler, avec le message "getDonnees() is not
  public in ServicePortefeuille; cannot be accessed from outside package". La visibilité de
  paquet ne dépend pas du nom du paquet, seulement du fait que l'appelant soit dans le même
  paquet ou non — `application.service.implementation` protège exactement comme `modele.service`
  le faisait avant.
- **Pourquoi le paquet des interfaces s'appelle-t-il `interfaces` avec un `s` ?** `interface` est
  un mot réservé du langage Java (comme `class` ou `public`) : il ne peut pas être utilisé comme
  identifiant, donc pas comme nom de paquet. `interfaces` (au pluriel) est le contournement
  imposé par la maîtresse de stage.
- **`exception` n'est-il pas mal placé, isolé au même niveau que les quatre autres paquets
  racine ?** C'est une décision explicite de la maîtresse de stage, pas une erreur : les
  exceptions ne sont spécifiques à aucune des quatre couches (une erreur de sauvegarde concerne
  `infrastructure`, une erreur de chargement aussi, mais rien n'empêche une future couche d'en
  définir une autre) — les garder à la racine évite de choisir arbitrairement une couche pour les
  héberger.

### Pièges rencontrés

Aucun — chaque étape (déplacement, substitution des paquets/imports, suppression des anciens
dossiers) a été vérifiée par une compilation complète avant de passer à la suivante, et un test
de bout en bout sur les sept écrans a été fait avant de conclure. Une seule vérification a
demandé de l'attention : m'assurer que la substitution automatique des chemins de paquets ne
touchait pas les mots ordinaires "vue" et "contrôleur" employés comme noms communs dans les
commentaires en français (par exemple "transmet le résultat à la vue") — la substitution a donc
été limitée aux formes qualifiées (`vue.VueXxx`, `controleur.ControleurXxx`, les lignes
`package`), jamais au mot seul.

### Reste à faire

Étapes 2 à 5 du CLAUDE.md : `PortefeuilleRepository`, DTO, réduction des dépendances autour de
`ServicePortefeuille`, nettoyage final. Aucune prise d'avance pour l'instant.

En marge de cette étape : `Menu.java`, que les entrées précédentes du journal signalaient comme
restant à supprimer, n'existe déjà plus sur le disque — sa suppression a dû être faite lors d'un
nettoyage antérieur non documenté ici. Ce point est donc clos, sans qu'une action supplémentaire
soit nécessaire.

## 2026-08-21 — Étape 2 de la migration : `PortefeuilleRepository`

Priorité 5 de la maîtresse de stage : séparer clairement la logique métier de la persistance.
Conception proposée et validée avant tout code (voir échange précédent) : interface minimale à
deux méthodes, dans `infrastructure/persistence` avec son implémentation, `GestionnaireFichier`
inchangée en interne, `ServicePortefeuille` typé sur l'interface, `Main` inchangé.

### Ce qui a été écrit

- **`PortefeuilleRepository`** (nouvelle interface, `infrastructure.persistence`) : deux méthodes,
  `charger()` et `sauvegarder(Portefeuille)`, copiées telles quelles depuis les signatures déjà
  utilisées par `GestionnaireFichier`. Rien de plus : ni recherche par id, ni mise à jour
  partielle, l'application ne charge qu'au démarrage et ne sauvegarde qu'un portefeuille entier.
- **`GestionnaireFichier`** : ajoute `implements PortefeuilleRepository`. Aucune autre ligne
  changée — sauvegarde atomique, encodage UTF-8, adaptateur `LocalDate`, chargement défensif à
  quatre cas, tout reste identique. Seul le commentaire d'en-tête est mis à jour pour dire que
  cette classe est désormais "l'unique implémentation de `PortefeuilleRepository`".
- **`ServicePortefeuille`** : le champ et le paramètre de constructeur passent du type
  `GestionnaireFichier` au type `PortefeuilleRepository`, renommés `gestionnaireFichier` →
  `portefeuilleRepository`. L'import de `GestionnaireFichier` disparaît : cette classe ne connaît
  plus du tout cette classe concrète, seulement l'interface. `sauvegarder()` appelle
  `portefeuilleRepository.sauvegarder(portefeuille)` au lieu de
  `gestionnaireFichier.sauvegarder(portefeuille)` — même appel, juste renommé.
- **Commentaires mis à jour** : le commentaire de classe de `ServicePortefeuille` (ajout d'un
  paragraphe expliquant le passage par l'interface), sa méthode `sauvegarder()` ("PortefeuilleRepository
  ou Portefeuille" au lieu de "GestionnaireFichier ou Portefeuille"), et le commentaire de classe
  de `Portefeuille` ("détenir le PortefeuilleRepository" au lieu de "détenir le
  GestionnaireFichier"). Les mentions de `GestionnaireFichier` qui restent ailleurs
  (`Main.java`, `Portefeuille.reparerApresChargement()`, `ErreurChargementException`) sont
  restées inchangées : elles décrivent toutes un comportement propre à cette classe concrète
  (Main l'instancie directement, les deux autres documentent ce que fait précisément son
  `charger()`), pas un contrat que l'interface devrait porter à sa place.
- **`Main.java`** : **aucune ligne changée**. Il continue d'instancier `GestionnaireFichier`
  directement et de le passer au constructeur de `ServicePortefeuille`, qui accepte maintenant un
  `PortefeuilleRepository` — Java accepte la conversion implicite, `GestionnaireFichier`
  implémentant cette interface.

### Choix de conception

Le détail de la conception (nom de l'interface, emplacement, sort de `GestionnaireFichier`) a été
proposé et discuté avant d'écrire une seule ligne — voir l'échange qui précède cette entrée. Deux
points qui n'y étaient pas encore tranchés en détail :

**Pourquoi les mentions de `GestionnaireFichier` dans `Main`, `Portefeuille` et
`ErreurChargementException` ne changent pas.** La consigne était de corriger les commentaires
"là où c'est maintenant l'interface qui compte". Dans ces trois cas, ce n'est pas l'interface qui
compte mais la classe concrète elle-même : `Main` est le seul endroit du projet qui a le droit de
connaître `GestionnaireFichier` par son nom (règle 5 du CLAUDE.md) ; le commentaire de
`reparerApresChargement()` documente que c'est précisément le `charger()` de cette classe qui
l'appelle après désérialisation (un détail d'implémentation, pas un contrat d'interface) ; et
`ErreurChargementException` documente ce que son `charger()` à elle ne peut pas absorber. Changer
ces trois-là en "PortefeuilleRepository" aurait été inexact : l'interface ne fait rien de tout ça
par elle-même, seule cette implémentation le fait.

### Points à savoir défendre

- **`ServicePortefeuille` importe-t-il encore `GestionnaireFichier` ?** Non, plus du tout —
  vérifié dans le fichier après modification : le seul import de persistance restant est
  `infrastructure.persistence.PortefeuilleRepository`. C'est la preuve concrète que la
  séparation demandée (priorité 5) est en place : ce service ne peut plus, même par erreur,
  appeler une méthode propre à `GestionnaireFichier` qui ne serait pas dans le contrat.
- **Pourquoi `Main` n'a-t-il rien à changer ?** Parce que le typage par interface ne change que la
  façon dont un consommateur *déclare* sa dépendance (ici, le paramètre du constructeur de
  `ServicePortefeuille`), jamais la façon dont l'objet est construit. C'est exactement le même
  raisonnement que pour les `IServiceXxx` à l'étape des interfaces de service : seul `Main` sait
  quelle implémentation concrète existe, tout le reste du projet n'en a plus besoin.
- **`getDonnees()` est-elle toujours protégée après ce changement ?** Oui, vérifié à nouveau
  concrètement, même méthode qu'à l'étape 1 : un fichier de test placé dans
  `presentation.controller` qui tente `servicePortefeuille.getDonnees()` refuse toujours de
  compiler ("getDonnees() is not public in ServicePortefeuille; cannot be accessed from outside
  package"). Ce changement ne touchait que la façon dont `ServicePortefeuille` parle au disque,
  pas la façon dont il protège le `Portefeuille` en mémoire vis-à-vis des autres couches — les
  deux sujets sont indépendants.

### Pièges rencontrés

Aucun — chaque modification (interface, `GestionnaireFichier`, `ServicePortefeuille`,
commentaires) a été suivie d'une compilation complète, et l'application a été relancée sur les
vraies données (`portefeuille.json`) pour confirmer que le chargement au démarrage fonctionne de
bout en bout à travers la nouvelle interface — fichier resté identique après coup (vérifié par
empreinte MD5), aucune écriture déclenchée par une simple consultation du solde.

### Reste à faire

Étapes 3 à 5 du CLAUDE.md : DTO, réduction des dépendances autour de `ServicePortefeuille`,
nettoyage final. Aucune prise d'avance pour l'instant.

## 2026-08-24 — Contrôleurs par domaine, plus aucun aiguillage entre eux

Nouvelle exigence de la maîtresse de stage : le code des contrôleurs était devenu difficile à
suivre (actions éclatées entre plusieurs méthodes privées enchaînées, noms `gererXxx()` peu
parlants, `ControleurPrincipal` qui appelait les autres contrôleurs). Cinq règles à appliquer :
noms de méthode explicites, une action = une méthode, aucun contrôleur n'en appelle un autre,
un contrôleur par domaine, les règles déjà en vigueur (aucun traitement, aucune chaîne
utilisateur dans un contrôleur, exceptions attrapées et affichées par la vue) ne changent pas.
Conception proposée et discutée avant tout code (voir l'échange qui précède cette entrée).

### Ce qui a été écrit

- **`ControleurPortefeuille`** (nouveau) : une seule méthode, `afficherSolde()`, reprise de
  l'ancienne `ControleurPrincipal.gererVoirSolde()` (qui était privée).
- **`ControleurCategorie`** : `activerCategorie()`/`desactiverCategorie()`, chacune reprise de
  l'ancienne méthode privée équivalente, promue publique. Ne dépend plus de
  `IServicePortefeuille` : elle ne s'en servait que pour la reprise sur échec de sauvegarde,
  disparue d'ici.
- **`ControleurTransaction`** : `ajouterDepense()`/`ajouterRevenu()` inchangées à l'intérieur,
  simplement débarrassées de leur `try/catch (ErreurSauvegardeException)`. `afficherHistorique()`
  garde les quatre variantes d'affichage (tout/date/catégorie/type) dans une seule méthode, avec
  son propre sous-menu réduit à cinq lignes (le choix "modifier ou supprimer" en est sorti).
  `modifierTransaction()`/`supprimerTransaction()` (nouvelles, publiques) reprennent les deux
  branches de l'ancienne `gererModificationSuppressionTransaction()`, avec chacune son propre
  `catch (IllegalArgumentException | IllegalStateException)`.
- **`ControleurEpargne`** : `creerObjectif()`, `contribuerObjectif()`, `retirerObjectif()`,
  `supprimerObjectif()` reprises des anciennes méthodes privées équivalentes, promues publiques,
  chacune avec son propre `catch` métier (avant, un seul `catch` partagé couvrait les cinq
  actions dans `gererObjectifsEpargne()`). `afficherObjectifs()` (nouvelle, reprend
  `gererConsultationObjectifs()`) comble un nom manquant dans la liste donnée : "voir mes
  objectifs" n'y figurait pas, ce nom a été proposé et validé avant d'écrire le code.
- **`ControleurStatistique`** : `gererStatistiques()` renommée `afficherStatistiques()`, rien
  d'autre ne change.
- **`ControleurPrincipal` et `ControleurConsole` supprimées.**
- **`Main.java`** : reprend la boucle du menu principal (inchangée) et, en plus, la lecture des
  trois sous-menus (historique, épargne, catégories) — chaque vue affiche son sous-menu et
  renvoie le choix en un seul appel (`demanderChoixMenu()`, `demanderChoixMenuHistorique()`),
  Main fait directement son `switch` sur cette valeur, sans méthode intermédiaire. La reprise sur
  échec de sauvegarde (`confirmerNouvelleSauvegarde`, méthode privée statique) est désormais
  écrite une seule fois ici, autour de l'appel au contrôleur, reprise presque telle quelle depuis
  `ControleurConsole`.
- **`VueCategorie`/`VueEpargne`** : `afficherMenuCategories()`/`afficherMenuEpargne()` passées en
  privé, chacune gagne un `demanderChoixMenu()` public (affiche + lit + renvoie le choix).
- **`VueTransaction`** : le sous-menu historique se scinde en deux niveaux. Premier niveau,
  nouveau, `demanderChoixMenuHistorique()` (Consulter/Modifier/Supprimer/Retour). Second niveau,
  `afficherMenuHistorique()` inchangée dans son rôle mais réduite à cinq lignes (tout/date/
  catégorie/type/retour), pour l'usage interne d'`afficherHistorique()`.
  `afficherMenuModifierSupprimer()` a disparu, devenue inutile.
- **`VuePrincipale`** : `afficherEchecSauvegarde(String)` retirée. Elle ne servait qu'au filet de
  sécurité de l'ancien `ControleurPrincipal.lancer()`, un `catch` qui ne se déclenchait en
  pratique jamais (chaque contrôleur attrapait déjà `ErreurSauvegardeException` avant que
  l'exception ne remonte jusque-là). Vérifié par recherche dans tout le code avant suppression :
  aucun autre appelant.

### Choix de conception

**Pourquoi la lecture des trois sous-menus atterrit dans `Main`, alors que la consigne ne parlait
explicitement que du menu principal.** Chaque sous-menu (catégories : activer/désactiver ;
épargne : créer/contribuer/retirer/afficher/supprimer) proposait un choix entre des actions qui
ont chacune leur propre nom dans la liste donnée — donc aucune des deux n'a vocation à "porter"
l'autre. Aucun contrôleur ne peut donc héberger ce choix sans redevenir un aiguilleur, exactement
le problème que `ControleurPrincipal` posait en plus grand. Seul `Main`, qui connaît déjà
plusieurs contrôleurs à la fois, peut trancher entre eux sans violer "aucun contrôleur n'en
appelle un autre". Le cas de l'historique est différent : les quatre variantes d'affichage
(tout/date/catégorie/type) n'ont qu'un seul nom dans la liste (`afficherHistorique()`) parce que
ce sont des variantes de la même action, pas des actions séparées — ce choix-là reste donc dans
le contrôleur, sous cette unique méthode.

**Pourquoi le sous-menu historique se scinde en deux niveaux plutôt que de rester un seul menu à
six lignes.** Discuté avant de coder : "modifier" et "supprimer" ont chacune leur propre nom dans
la liste, contrairement aux quatre variantes d'affichage — ce choix devait donc, comme pour les
catégories et l'épargne, remonter dans `Main`. Faire lire tout le menu à six lignes par `Main`
directement aurait obligé `Main` à connaître les dates/catégorie/type pour les trois premiers
choix, c'est-à-dire réimplémenter ce qu'`afficherHistorique()` fait déjà — de la logique de
contrôleur qui se serait retrouvée dans `Main`. Scinder en "Consulter / Modifier / Supprimer /
Retour" (Main) puis, à l'intérieur de "Consulter", "Tout / Date / Catégorie / Type / Retour"
(`afficherHistorique()`) évite ce problème : chaque niveau ne connaît que ce qui le concerne.

**Pourquoi chaque vue "affiche et renvoie le choix" en une seule méthode, plutôt que deux appels
séparés comme avant.** Consigne explicite : pas de méthode intermédiaire du genre
`gererMenuXxx()` dans `Main`, qui aurait recréé exactement ce qu'on venait de sortir des
contrôleurs. En combinant affichage et lecture dans une seule méthode de vue
(`demanderChoixMenu()`), `Main` peut faire son `switch` directement sur l'appel, sans variable ni
méthode intermédiaire — tout le cheminement se lit dans la boucle de `Main`, du menu principal
jusqu'à l'action choisie.

**Pourquoi `Main` appelle `serviceCategorie.getCategoriesActives()` directement avant le
sous-menu catégories, plutôt que de passer par un contrôleur.** Seule exception à "Main
n'appelle que des contrôleurs". Avant ce sprint, `ControleurCategorie.gererCategories()`
affichait les catégories actives avant de proposer le choix ; cet affichage n'appartient à aucune
des deux actions désormais séparées (`activerCategorie()`/`desactiverCategorie()`), donc il n'a
pas suivi l'une plus que l'autre en migrant. C'est un simple accès en lecture (un getter, aucun
calcul), du même ordre que ce qu'un contrôleur aurait fait — la seule différence est que c'est
`Main` qui le fait, parce que rien d'autre n'en a la responsabilité une fois l'aiguillage retiré
de `ControleurCategorie`.

### Points à savoir défendre

- **Pourquoi `IllegalArgumentException`/`IllegalStateException` sont-elles attrapées à cinq
  endroits différents dans `ControleurEpargne` maintenant, au lieu d'un seul avant ?** Parce que
  ces cinq actions sont désormais cinq méthodes indépendantes, chacune devant se lire seule de
  haut en bas sans dépendre d'un `try` partagé par un aiguilleur qui n'existe plus. Le prix est un
  peu de répétition (la même ligne de `catch` cinq fois) ; le gain est qu'aucune méthode ne
  dépend plus d'une autre pour être comprise.
- **`ErreurSauvegardeException` n'est catchée nulle part dans les contrôleurs — n'est-ce pas
  risqué ?** Non : c'est une exception non vérifiée (`RuntimeException`), Java ne l'exige pas.
  Elle remonte naturellement jusqu'au seul `catch` qui la concerne, dans `Main`, exactement comme
  prévu. Les exceptions métier, elles, sont attrapées localement parce que `Main` ne doit jamais
  avoir à connaître une règle de gestion propre à un écran particulier.
- **`getDonnees()` est-elle toujours protégée après ce remaniement ?** Oui, revérifié
  concrètement, même méthode qu'aux étapes précédentes : un fichier de test placé dans
  `presentation.controller` qui tente `servicePortefeuille.getDonnees()` refuse toujours de
  compiler. Ce sprint n'a touché qu'à la façon dont les contrôleurs sont organisés et appelés,
  jamais à la protection du `Portefeuille` en mémoire.

### Pièges rencontrés

Aucun — chaque fichier a été vérifié par une compilation complète avant de passer au suivant, et
l'application a été rejouée de bout en bout (les huit écrans, y compris les nouveaux sous-menus
à deux niveaux de l'historique) sur les vraies données de `portefeuille.json`, sans jamais
confirmer une écriture : fichier resté identique après coup (vérifié par empreinte MD5).

### Reste à faire

Étapes 3 à 5 du CLAUDE.md, inchangées : DTO, réduction des dépendances autour de
`ServicePortefeuille`, nettoyage final.

## 2026-08-24 — Corrections après revue sur les contrôleurs

Retour de revue sur le sprint précédent (restructuration des contrôleurs par domaine), avec des
corrections précises à appliquer, et deux points explicitement écartés.

### Ce qui a été écrit

- **Champs `final`** : tous les champs d'instance des cinq contrôleurs et des cinq services (plus
  `PortefeuilleRepository` dans `ServicePortefeuille`) sont maintenant `final`. Aucun n'était
  réaffecté après le constructeur ; `final` le rend visible dans la signature du champ plutôt que
  de dépendre d'une relecture attentive du reste de la classe.
- **`ServiceStatistique`** : nouvelle méthode privée `validerPeriode(debut, fin)`, appelée en
  première ligne des trois méthodes publiques. Lève `IllegalArgumentException` si `debut` est
  postérieur à `fin`. **`ControleurStatistique.afficherStatistiques()`** : les trois appels de
  service sont maintenant dans un `try`, avec un `catch (IllegalArgumentException erreur)` qui
  transmet le message à la vue — premier `try/catch` de ce contrôleur, qui était jusque-là
  entièrement en lecture seule sans jamais avoir eu besoin d'en attraper.
- **`ControleurEpargne.contribuerObjectif()`** : `afficherSoldeDisponible(...)` passe avant
  `lireMontant("Montant à ajouter : ")`, pas après. L'utilisateur voit maintenant combien il peut
  se permettre de donner avant qu'on le lui demande, plutôt qu'après avoir déjà répondu.
- **`ControleurEpargne.afficherObjectifs()`** : le test `if (!serviceEpargne.aAuMoinsUnObjectif())`
  a disparu. La méthode demande directement un identifiant ; si aucun objectif n'existe (ou si
  l'identifiant ne correspond à rien), `serviceEpargne.getObjectif(id)` lève
  `IllegalArgumentException`, déjà attrapée par le `catch` existant de cette méthode. Le
  contrôleur n'a donc plus besoin de vérifier une condition que le service vérifie déjà.
  **`aAuMoinsUnObjectif()`** est retirée de `ServiceEpargne` et `IServiceEpargne` : plus aucun
  appelant après ce retrait.
- **`ControleurTransaction.afficherHistorique()`** : éclatée en quatre méthodes publiques —
  `afficherHistorique()` (tout, sans filtre), `filtrerParDate()`, `filtrerParCategorie()`,
  `filtrerParType()` — chacune une seule action, sans sous-menu interne. **`VueTransaction`** :
  le sous-menu historique redevient un seul niveau à sept lignes (les six actions désormais
  toutes nommées, plus "Retour"), au lieu des deux niveaux de l'étape précédente. **`Main`** : le
  `case 4` du menu principal fait maintenant un `switch` à six branches (une par action) sur la
  valeur renvoyée par `vueTransaction.demanderChoixMenuHistorique()`, au lieu de deux niveaux de
  `switch` imbriqués.

### Ce qui n'a pas été touché, sur demande explicite

Pas d'interfaces de vue, pas de `VuePortefeuille` séparée de `VuePrincipale`, pas de
factorisation des blocs `catch` répétés entre les méthodes des contrôleurs, pas de factorisation
d'une méthode `verifierCategorieDisponible()` partagée entre `ajouterDepense()`,
`ajouterRevenu()` et `modifierTransaction()`. Ces quatre pistes de simplification ont été
examinées en revue et écartées : la répétition qui reste est jugée plus lisible que
l'abstraction qui l'aurait remplacée.

### Choix de conception

**Pourquoi `afficherHistorique()` redevient quatre méthodes distinctes, après avoir été gardée
volontairement fusionnée à l'étape précédente.** À l'étape précédente, la justification pour
garder les quatre variantes ensemble était qu'aucune des quatre n'apparaissait séparément dans la
liste de noms donnée à l'époque. Ce sprint revient sur cette lecture : une fois
`modifierTransaction()`/`supprimerTransaction()` sorties de l'écran historique (déjà fait à
l'étape précédente), il devenait incohérent que les quatre variantes d'affichage restent, elles,
cachées derrière un sous-menu interne au contrôleur alors que toutes les autres actions de
l'application (catégories, épargne, modifier/supprimer une transaction) passent par un choix lu
directement dans `Main`. Aligner l'historique sur ce même patron — un seul niveau de menu, lu par
`Main`, chaque choix vers sa propre méthode — supprime cette exception et simplifie `Main` (un
seul `switch` par écran au lieu de deux imbriqués pour l'historique seulement).

**Pourquoi `validerPeriode()` est privée et appelée trois fois plutôt qu'une validation faite une
seule fois dans le contrôleur.** La règle "la date de début ne peut pas être postérieure à la
date de fin" est une règle de gestion sur des données, pas une question d'affichage : elle doit
vivre dans le service (comme toutes les validations de ce type dans ce projet), pas dans le
contrôleur. Elle est appelée dans les trois méthodes publiques de `ServiceStatistique` plutôt que
mutualisée autrement parce que les trois sont des points d'entrée indépendants (rien ne garantit
qu'elles seront toujours appelées ensemble) : chacune doit rester valide seule.

**Pourquoi retirer `aAuMoinsUnObjectif()` plutôt que la garder "au cas où".** Elle dupliquait une
protection qui existait déjà ailleurs (`getObjectif()` refuse déjà un identifiant inconnu) sans
rien ajouter que `afficherObjectifs()` n'ait pas déjà par son propre `catch`. La garder aurait
laissé deux façons différentes de refuser la même situation (liste vide vs identifiant
introuvable), pour un seul et même problème.

### Points à savoir défendre

- **`afficherObjectifs()` ne vérifie plus explicitement que la liste n'est pas vide — n'est-ce
  pas moins sûr ?** Non : la protection n'a pas disparu, elle est juste devenue unique. Avant, il
  y avait deux gardes-fous pour la même situation (liste vide dans le contrôleur, identifiant
  introuvable dans le service) ; il n'en reste qu'un, mais il n'a jamais cessé d'exister, et il
  couvre exactement les mêmes cas (liste vide *et* identifiant erroné sur une liste non vide).
- **Pourquoi `ControleurStatistique` a-t-il maintenant un `try/catch`, lui qui n'en avait jamais
  eu besoin ?** Parce qu'avant ce sprint, aucune règle de gestion ne portait sur la période
  demandée : n'importe quelles dates passaient. La règle "début <= fin" est nouvelle, portée par
  le service comme toute règle de ce type ; le contrôleur doit donc, comme les autres écrans,
  attraper l'exception métier qu'elle peut désormais lever.
- **`getDonnees()` est-elle toujours protégée après ces changements ?** Oui, revérifié
  concrètement une nouvelle fois : aucun de ces changements ne touche à `ServicePortefeuille` ni
  à la visibilité de paquet qui protège son `Portefeuille`.

### Pièges rencontrés

Aucun — chaque changement a été vérifié par une compilation complète, et l'application a été
rejouée de bout en bout sur les vraies données de `portefeuille.json`, y compris les deux
nouveaux chemins d'erreur (période invalide en statistiques, identifiant d'objectif inexistant),
pour confirmer qu'ils affichent un message lisible au lieu de planter. Fichier resté identique
après coup (vérifié par empreinte MD5).

### Reste à faire

Étapes 3 à 5 du CLAUDE.md, inchangées : DTO, réduction des dépendances autour de
`ServicePortefeuille`, nettoyage final.

## 2026-08-24 — Étape 3 (fin) : les invites de saisie sortent des contrôleurs

Deuxième retour de revue sur l'étape 3. Deux corrections : le nom des quatre méthodes
d'affichage de l'historique, et surtout une incohérence relevée sur la règle "aucune chaîne
destinée à l'utilisateur dans un contrôleur" — respectée pour les messages affichés, pas pour
les invites de saisie (`vueEpargne.lireMontant("Montant cible : ")` reste un contrôleur qui
choisit un texte, même si ce texte sert à demander plutôt qu'à afficher).

### Ce qui a été écrit

- **`ControleurTransaction`** : `afficherHistorique()`, `filtrerParDate()`,
  `filtrerParCategorie()`, `filtrerParType()` renommées `afficherHistoriqueComplet()`,
  `afficherHistoriqueParDate()`, `afficherHistoriqueParCategorie()`,
  `afficherHistoriqueParType()`. Les quatre affichent, avec un filtre différent ; un nom en
  `filtrerXxx()` laissait croire que c'était le contrôleur qui filtrait, alors que c'est
  toujours `ServiceTransaction`.
- **`VueCategorie`** : déjà conforme, sert de référence pour le reste — inchangée.
- **`VueEpargne`, `VueTransaction`, `VueStatistique`, `VuePrincipale`** : chacune gagne des
  méthodes `demanderXxx()` dédiées, sans paramètre `message`, le texte fixé à l'intérieur
  (`demanderNomObjectif()`, `demanderMontantDepense()`, `demanderIdentifiantAModifier()`,
  `demanderDateDebut()`, `demanderChoix()`...). Les méthodes génériques de `VueConsole`
  (`lireEntier`, `lireLigne`, `lireMontant`, `lireDate`, `confirmer`) ne sont plus appelées
  qu'à l'intérieur des vues elles-mêmes.
- **Les cinq contrôleurs et `Main`** : chaque appel du genre `vueXxx.lireMontant("...")` devient
  `vueXxx.demanderMontantXxx()`. Plus aucune chaîne de caractères, quelle qu'elle soit, dans le
  code d'un contrôleur — vérifié par recherche systématique après coup.
- **`ControleurTransaction`** : la méthode privée `choisirTransaction(String)` disparaît.
  `modifierTransaction()` et `supprimerTransaction()` avaient besoin du même texte
  d'introduction ("afficher l'historique") mais de deux invites différentes pour l'identifiant ;
  comme le texte ne peut plus être un paramètre, chacune appelle directement
  `afficherHistoriqueComplet()` (sa méthode sœur publique, pas une méthode privée dédiée) puis
  sa propre méthode `demanderIdentifiantAModifier()`/`demanderIdentifiantASupprimer()`.
- Quelques textes identiques entre deux actions ont été mutualisés dans une seule méthode de
  vue plutôt que dupliqués : `VueEpargne.demanderIdentifiantObjectif()` ("Identifiant de
  l'objectif : ", utilisée par `contribuerObjectif()` et `retirerObjectif()`) et
  `VueEpargne.demanderDate()` ("Date (JJ/MM/AAAA, vide = aujourd'hui) : ", mêmes deux
  appelantes) ; `VueTransaction.demanderDate()`/`demanderDescription()` (mêmes textes pour
  `ajouterDepense()` et `ajouterRevenu()`).

### Choix de conception

**Pourquoi le gain n'est pas cosmétique.** Un contrôleur qui écrit
`vueEpargne.lireMontant("Montant cible : ")` connaît un détail de présentation : la formulation
exacte de l'invite. Si la console était remplacée par une interface graphique demain, ce texte
n'aurait plus de sens tel quel (un champ de formulaire n'a pas besoin d'une phrase avec ":" à la
fin) — le contrôleur devrait être modifié en même temps que la vue. Avec
`vueEpargne.demanderMontantCible()`, le contrôleur ne sait même pas qu'il y a un texte : seule
`VueEpargne` changerait.

**Pourquoi certaines invites sont mutualisées et d'autres non, alors que le texte semble parfois
proche.** Seuls les textes strictement identiques entre deux appelantes ont été mutualisés
(`demanderIdentifiantObjectif()`, `demanderDate()` dans les deux vues concernées). "Montant à
ajouter" et "Montant à retirer", par exemple, ne le sont pas : les textes diffèrent, donc les
méthodes restent séparées (`demanderMontantContribution()`, `demanderMontantRetrait()`) — le
critère est le texte réellement affiché, pas la ressemblance de ce que fait le contrôleur.

**Pourquoi `choisirTransaction(String)` disparaît au lieu de perdre juste son paramètre.** Une
méthode privée partagée n'a de sens que si elle fait la même chose pour ses appelantes ; ici,
la seule partie vraiment commune (afficher l'historique) est déjà une méthode publique complète
à elle seule (`afficherHistoriqueComplet()`), et la partie qui différait (le texte de l'invite)
ne pouvait plus être un paramètre. Il ne restait donc rien à factoriser dans une méthode privée
séparée.

### Points à savoir défendre

- **Les vues ont grossi (jusqu'à 185 lignes pour `VueTransaction`) — n'est-ce pas devenu trop
  long ?** Vérifié : non, chaque nouvelle méthode fait une à trois lignes, regroupées par écran
  dans l'ordre où elles servent. C'est le prix annoncé à l'avance ("ça va multiplier les
  méthodes dans les vues") pour que les contrôleurs restent muets sur ce qu'ils demandent, pas
  un signe de dérive.
- **Comment as-tu vérifié qu'il ne restait plus aucune chaîne dans les contrôleurs, et pas
  seulement les invites visées par la revue ?** Recherche de toutes les occurrences de `"` dans
  les cinq fichiers de `presentation.controller` après les corrections : les seules restantes
  sont dans des commentaires (blocs `/* */` et `//`), aucune dans le code exécutable.
- **Le comportement de l'application a-t-il changé ?** Non, vérifié concrètement : la sortie
  console d'un même scénario de test, rejouée avant et après cette correction, est identique à
  l'octet près (comparée par `diff`). Seuls les noms de méthodes ont changé, jamais les textes
  affichés ni la logique.

### Pièges rencontrés

Aucun — chaque vue a été corrigée puis recompilée avant de passer à la suivante, et le scénario
de test de bout en bout (les huit écrans, y compris les deux chemins d'erreur ajoutés
précédemment) a été rejoué sur les vraies données de `portefeuille.json` pour confirmer une
sortie identique à celle d'avant la correction.

### Reste à faire

Étape 3 terminée. Étapes 4 à 6 du CLAUDE.md, à venir sur demande : DTO, réduction des
dépendances autour de `ServicePortefeuille`, nettoyage final.

## 2026-08-24 — Les quatre entités sans aucune logique de validation

Nouvelle exigence de la maîtresse de stage, appliquée à la lettre : les entités
(`Transaction`, `Epargne`, `MouvementEpargne`, `Portefeuille`) ne contiennent plus que leur
structure — attributs, constructeur, getters, setters. Tout le reste (validation, génération
d'identifiants, recherche par clé, réparation post-chargement) a été déplacé vers le service ou
la classe de persistance concernée.

### Ce qui a été écrit

- **`Transaction`** : `validerId`, `validerMontant`, `validerType`, `validerCategorie`,
  `validerDate` retirées. Constructeur et setters ne font plus qu'assigner. Toute la validation
  (montant positif, date pas dans le futur, catégorie cohérente avec le type) est désormais dans
  **`ServiceTransaction`**, plus une méthode nouvelle, `normaliserDescription()`, qui remplace
  une description `null` par une chaîne vide.
- **`Epargne`** : `validerNom`, `validerMontantCible` retirées, déplacées dans
  **`ServiceEpargne`** (`validerNomObjectif()`, `validerMontantCible()`).
- **`MouvementEpargne`** : `validerMontant`, `validerSens`, `validerDate` retirées, déplacées
  dans **`ServiceEpargne`** (`validerMontantMouvement()`, `validerSensMouvement()`,
  `validerDateMouvement()`), partagées par `contribuerObjectif()` et `retirerObjectif()`.
- **`Portefeuille`**, quatre points tranchés avec l'étudiant avant d'écrire le code :
  - `genererIdTransaction()`/`genererIdObjectif()` retirées, remplacées par des getters/setters
    classiques sur les deux compteurs (`getProchainIdTransaction()`/
    `setProchainIdTransaction(int)`, pareil pour objectif). La génération (lire, incrémenter)
    est désormais une méthode privée de `ServiceTransaction`/`ServiceEpargne`.
  - `ajouterTransaction()`/`retirerTransaction()`, `ajouterObjectif()`/`retirerObjectif()`,
    `activerCategorie()`/`desactiverCategorie()` **conservées** telles quelles : défendues comme
    des setters d'un élément d'une collection, pas comme des méthodes de calcul (voir "Choix de
    conception").
  - `getObjectif(int)`/`trouverObjectif(int)` retirées. La recherche par identifiant vit
    maintenant dans **`ServiceEpargne.trouverObjectif()`** (méthode privée), sur le modèle déjà
    en place dans `ServiceTransaction.trouverTransaction()`.
  - `reparerApresChargement()` retirée, déplacée dans **`GestionnaireFichier`** (voir plus bas).
  - `getTransactions()`/`getCategoriesActives()`/`getObjectifs()` : toujours des vues non
    modifiables, mais désormais null-safe (renvoient `null` si le champ sous-jacent est encore
    `null`, plutôt que de lever une `NullPointerException` en tentant de l'envelopper).
  - Trois nouveaux setters, `setTransactions(List)`, `setCategoriesActives(Set)`,
    `setObjectifs(List)` : remplacent le champ entier, à la différence des méthodes d'ajout/
    retrait ci-dessus. Réservés à l'usage de `GestionnaireFichier`.
- **`GestionnaireFichier`** : nouvelle méthode privée `reparerApresChargement(Portefeuille)`,
  appelée juste après la désérialisation dans `charger()`. Utilise les nouveaux setters de
  collection pour remplacer un champ resté `null` par une collection vide.
- **Aucun changement d'interface** : `IServiceTransaction`, `IServiceEpargne`,
  `IServicePortefeuille` inchangées, toutes les nouvelles méthodes sont privées.

### Choix de conception

**Pourquoi `ajouterTransaction()`/`retirerTransaction()` (et les quatre méthodes symétriques)
restent dans `Portefeuille`, alors qu'elles ne sont ni un attribut, ni le constructeur, ni un
getter, ni un setter au sens strict.** Discuté et validé avant d'écrire le code. Ce sont des
setters d'un élément d'une collection, pas des méthodes de calcul ou de décision. La seule
alternative pour respecter la lettre de la règle aurait été un `setTransactions(List<Transaction>)`
qui remplace la liste entière : chaque service aurait dû copier la liste actuelle, la modifier,
puis la réinjecter en entier pour un simple ajout ou retrait — plus lourd à lire, et pas plus
sûr, puisque le point qui compte (empêcher un contournement des services) est déjà garanti
autrement : `getTransactions()` reste une vue non modifiable, et `Portefeuille` lui-même n'est
accessible que via `ServicePortefeuille.getDonnees()`, à visibilité de paquet. Retirer ces
méthodes n'aurait rien ajouté à la protection réelle, seulement rendu le code plus lourd pour
paraître plus conforme à la lettre de la règle.

**Pourquoi les getters de liste ne sont pas devenus modifiables.** C'est la conséquence
directement liée au choix ci-dessus : comme `ajouterTransaction()`/`retirerTransaction()` etc.
restent la seule façon d'ajouter ou de retirer un élément, rien n'obligeait à ouvrir les getters.
Si ces méthodes avaient dû disparaître, les getters auraient dû renvoyer les collections réelles
pour que les services puissent les modifier — un vrai recul de protection, puisque n'importe quel
service (pas seulement celui qui devrait légitimement écrire) aurait alors pu modifier la liste
sans validation.

**Pourquoi `reparerApresChargement()` part dans `GestionnaireFichier`, contrairement à ma
première proposition.** Position de l'étudiant, tranchée : cette méthode ne répare aucune règle
du domaine, elle rattrape un comportement de Gson (contournement du constructeur à la
désérialisation, déjà documenté comme piège dans le CLAUDE.md). Elle appartient donc à la
persistance, pas à l'entité. Ça a nécessité d'exposer trois setters de collection sur
`Portefeuille` — la règle demandait de toute façon des setters — et de rendre les trois getters
de liste tolérants à un champ encore `null`, pour que `GestionnaireFichier` puisse tester leur
état sans lever de `NullPointerException` en les appelant.

**Pourquoi les getters de liste vérifient `null` avant d'envelopper.** Sans ce garde-fou,
`Collections.unmodifiableList(transactions)` lève immédiatement une `NullPointerException` si
`transactions` est `null` — impossible, alors, pour `GestionnaireFichier` de détecter l'état
"pas encore réparé" en appelant simplement `getTransactions()`. Avec le garde-fou, le getter dit
la vérité sur l'état du champ (`null` si rien n'a encore été chargé, une vue non modifiable
sinon), ce qui est en réalité plus correct que l'ancien comportement, qui aurait toujours planté
si quoi que ce soit avait appelé ce getter avant `reparerApresChargement()`.

### Points à savoir défendre

- **Pourquoi `validerId`/`validerType` (dans `ServiceTransaction`) et `validerSensMouvement`
  (dans `ServiceEpargne`) valident-elles des valeurs qui ne peuvent jamais être invalides en
  pratique (l'identifiant vient toujours du compteur, le type et le sens sont toujours des
  constantes fixées par le code) ?** Ce sont des contrôles défensifs, hérités tels quels des
  entités : rien ne garantit qu'ils resteront toujours inatteignables si le code évolue, et les
  retirer aurait fait disparaître une règle sans certitude qu'elle ne servira jamais. Ça n'a
  jamais été demandé de les juger utiles, seulement de les déplacer sans rien perdre.
- **`ajouterTransaction()` n'est-elle pas, en réalité, un traitement déguisé en setter ?** C'est
  la question la plus discutable de cette étape, assumée comme telle : elle ne calcule rien, ne
  décide rien, ne valide rien — elle ajoute un élément déjà construit et déjà validé à une liste.
  La différence avec un vrai traitement (comme l'ancien `genererIdTransaction()`, qui calculait
  une valeur à partir d'un état) est que `ajouterTransaction()` ne fait qu'exécuter l'ordre
  qu'on lui donne, sans rien décider elle-même.
- **Comment vérifier que les quatre cas de chargement défensif fonctionnent toujours ?** Testés
  un par un dans un répertoire isolé (jamais les vraies données) : fichier absent, fichier vide,
  JSON malformé (les trois sans plantage, solde à 0 dans les trois cas), et surtout listes
  explicitement `null` dans un JSON par ailleurs valide — non seulement l'application ne plante
  pas, mais une catégorie activée puis une dépense ajoutée après un tel chargement s'enregistrent
  correctement sur le disque, preuve que les collections réparées sont de vraies listes/ensembles
  mutables, pas seulement des valeurs qui ne plantent plus en lecture.
- **`getDonnees()` est-elle toujours protégée après ce remaniement ?** Oui, revérifié
  concrètement une nouvelle fois : ce travail n'a touché ni `ServicePortefeuille` ni sa
  visibilité de paquet.

### Pièges rencontrés

Aucun sur le code final, mais un aller-retour de conception noté ici parce qu'il explique un
choix : la première idée pour `reparerApresChargement()` était de la garder dans `Portefeuille`
comme exception pragmatique. Revue et abandonnée à la demande de l'étudiant (voir "Choix de
conception" ci-dessus), au profit du déplacement dans `GestionnaireFichier` — ce qui a fait
apparaître, en le concevant, le problème des getters non null-safe, résolu avant qu'il ne
devienne un bug réel.

### Reste à faire

Étapes 4 à 6 du CLAUDE.md, à venir sur demande : DTO, réduction des dépendances autour de
`ServicePortefeuille`, nettoyage final.

## 2026-08-27 — Étape 4 (1/2) : `TransactionAffichage`, premier DTO de l'application

### Ce qui a été écrit

- **`TransactionAffichage`** (nouvelle classe, `application.dto`) : six champs — id, montant,
  type, catégorie, date, description — exactement ce qu'une ligne d'historique affiche
  aujourd'hui. Attributs `final`, constructeur, getters, rien d'autre : ni calcul, ni mise en
  forme.
- **`IServiceTransaction`** : `getHistorique()`, `filtrerParDate()`, `filtrerParCategorie()`,
  `filtrerParType()`, `getTransaction(int)` renvoient désormais `TransactionAffichage`/
  `List<TransactionAffichage>` au lieu de `Transaction`/`List<Transaction>`.
  `ajouterDepense()`/`ajouterRevenu()` passent de `Transaction` à `void` : leur valeur de retour
  n'a jamais été lue par `ControleurTransaction`, la garder aurait été du code mort.
- **`ServiceTransaction`** : une méthode privée de plus, `versAffichage(Transaction)`, qui
  construit le DTO à partir de l'entité. Chaque méthode qui renvoyait une `Transaction` ou une
  liste de `Transaction` à la présentation passe désormais par elle juste avant de sortir du
  service — l'entité elle-même ne sort plus jamais de cette classe.
- **`VueTransaction`** : `afficherTransactions()` reçoit `List<TransactionAffichage>` au lieu de
  `List<Transaction>`. Elle ne délègue plus à `transaction.toString()` : une nouvelle méthode
  privée `formaterLigne(TransactionAffichage)` reprend exactement la même mise en forme, mais
  dans la vue.
- **`Transaction`** : `toString()` supprimée. Plus aucun appelant depuis que `VueTransaction`
  construit elle-même sa ligne d'affichage.
- **`ControleurTransaction`** : aucune modification. Il ne déclarait déjà aucune variable de
  type `Transaction` (le seul usage, `serviceTransaction.getTransaction(id).getType()`, est un
  simple enchaînement d'appel qui n'a jamais eu besoin d'importer le type) — le changement de
  signature ne s'y voit donc pas.

### Choix de conception

**Pourquoi la conversion vit dans `ServiceTransaction` et pas dans une classe dédiée.** Un seul
service concerné à cette étape, une poignée de méthodes à adapter : une classe séparée
(`ConvertisseurTransaction` ou équivalent) aurait ajouté une indirection sans réduire de
duplication réelle. `ServiceTransaction` connaît déjà les données de la `Transaction` qu'il
vient de manipuler ou de retrouver ; construire le DTO juste à côté est le trajet le plus court
entre la donnée et sa sortie vers la présentation.

**Pourquoi `ajouterDepense()`/`ajouterRevenu()` deviennent `void`.** Question posée avant de
coder, tranchée par l'étudiant : une valeur de retour que personne ne lit est du code mort, et
`activerCategorie()`, `supprimerTransaction()`, `contribuerObjectif()` renvoient déjà `void` —
une méthode qui exécute une action renvoie `void`, une méthode qui répond à une question renvoie
une valeur. `ajouterDepense`/`ajouterRevenu` sont clairement du premier type.

**Pourquoi `Transaction.toString()` disparaît plutôt que de rester inutilisée.** Un DTO ne
contient "ni calcul, ni mise en forme" (règle du `CLAUDE.md`) : une fois `VueTransaction`
alimentée par `TransactionAffichage`, c'est forcément elle qui construit la ligne affichée à
partir des champs bruts du DTO. `Transaction.toString()` n'avait donc plus aucun appelant.
Vérifié avant suppression (`grep` sur tout `src/`) qu'aucun message d'erreur, aucune
concaténation implicite (`+ transaction`) ni aucun affichage de secours ne s'appuyait dessus :
seuls l'appel explicite de `VueTransaction` (remplacé) et rien d'autre. Même précédent déjà posé
pour `Epargne.toString()` à l'étape 3 bis.

**Pourquoi `getHistorique()` trie toujours les `Transaction` avant de les convertir, plutôt que
de trier les DTO directement.** Le tri (`Comparator.comparing(Transaction::getDate).reversed()`)
existait déjà avant cette étape et n'a pas été touché : il porte sur des `Transaction`, pas sur
des `TransactionAffichage`, donc il doit rester avant la boucle de conversion, pas après.
Convertir d'abord puis trier aurait obligé à écrire un second comparateur pour le DTO, sans
aucun gain.

### Points à savoir défendre

- **`TransactionAffichage` a-t-il le droit de contenir un `Categorie` (enum du domaine) sans
  redevenir "une entité qui circule jusqu'à la présentation" ?** Oui : la règle du `CLAUDE.md`
  cible les entités (`Transaction`, `Epargne`...), pas les énumérations — les vues ont
  explicitement le droit d'importer `domain.enumeration` pour lire des getters et afficher
  (`categorie.getLibelle()`). Un DTO qui porte un `Categorie` respecte donc la règle telle
  qu'elle est écrite.
- **Pourquoi `ControleurTransaction` n'a-t-il rien à changer alors que quatre méthodes de
  service changent de type de retour ?** Parce qu'il ne fait qu'enchaîner des appels sans jamais
  déclarer de variable typée `Transaction` ou `TransactionAffichage` explicitement — il transmet
  directement le résultat d'un appel de service à un appel de vue. C'est la preuve concrète que
  ce contrôleur respecte déjà la règle "aucun traitement, seulement des appels".
- **Le DTO est-il "anémique" comme les entités ?** Oui, volontairement, et pour la même raison :
  aucune validation, aucun calcul, uniquement des données. La différence avec une entité, c'est
  qu'un DTO n'a jamais prétendu porter autre chose — il n'y a donc pas de garantie
  conventionnelle à documenter comme pour `Epargne`/`Transaction` (section 3 du `CLAUDE.md`) : un
  DTO invalide n'a pas de sens à interdire, puisqu'il ne fait que recopier des données déjà
  validées par le service qui le construit.

### Pièges rencontrés

Aucun — compilation propre du premier coup (`javac` sur l'ensemble de `src/`), la conversion
étant mécanique une fois le DTO écrit.

### Reste à faire

Étape 4 (2/2) : `ObjectifAffichage` et `MouvementAffichage` pour l'écran épargne — remplacent le
trio `List<Epargne>` + deux `List<Double>` parallèles de `VueEpargne.afficherObjectifs()`, et
`List<MouvementEpargne>` de `afficherMouvements()`. `IServiceEpargne.depasseraCible()` passera
d'un paramètre `Epargne` à un `idObjectif` ; `getMontantActuel`, `getPourcentageAtteint`,
`getMontantsActuels`, `getPourcentagesAtteints` sortiront de l'interface (deviendront des
détails privés de `ServiceEpargne`, plus aucun appelant externe une fois `getObjectifs()` fusionné).
`MouvementEpargne.toString()` disparaîtra dans la foulée, même raisonnement que
`Transaction.toString()` ci-dessus. Puis étapes 5 (réduction des dépendances autour de
`ServicePortefeuille`) et 6 (nettoyage final) du `CLAUDE.md`.

## 2026-08-27 — Correction : convention de nommage des DTO, `TransactionAffichage` devient `TransactionDTO`

### Ce qui a été écrit

`application.dto.TransactionAffichage` renommée en `TransactionDTO` (fichier, classe,
commentaire de classe), avec toutes ses références mises à jour dans `IServiceTransaction`,
`ServiceTransaction` et `VueTransaction`.

### Choix de conception

**Suffixe `DTO` retenu plutôt que `Affichage`, pour tous les DTO à venir.** Décidé par
l'étudiant avant même d'écrire le deuxième DTO : `Affichage` ne convient qu'aux DTO qui
transportent vers la présentation (le sens qui existait jusqu'ici), alors qu'un futur DTO
pourrait aussi bien porter des données dans l'autre sens. `DTO` est un suffixe neutre, déjà le
nom du paquet (`application.dto`) — pas d'ambiguïté possible, et c'est la convention qui sera
suivie pour la suite de l'étape 4 : `ObjectifDTO`, `MouvementDTO`.

### Points à savoir défendre

**Pourquoi cette correction a sa propre entrée plutôt que de réécrire l'entrée précédente ?**
Règle du journal, section 10 du `CLAUDE.md` : on n'modifie jamais une entrée existante, même
pour corriger un nom de classe qui vient de changer. L'entrée précédente reste donc exacte pour
ce qu'elle décrit — le raisonnement sur la conversion, le `void`, la suppression de
`toString()` — sauf le nom de la classe, corrigé ici.

### Reste à faire

Étape 4 (2/2), avec la convention `DTO` : `ObjectifDTO` et `MouvementDTO` pour l'écran épargne,
comme décrit dans l'entrée précédente (le contenu ne change pas, seul le nom des classes à
créer). Le code de cette sous-étape n'est pas encore écrit : l'étudiant teste et commite d'abord
l'écran transactions.

## 2026-08-27 — Étape 4 (2/2) : `ObjectifDTO` et `MouvementDTO`, plus aucune entité en présentation

### Ce qui a été écrit

- **`ObjectifDTO`** (nouvelle classe, `application.dto`) : id, nom, montantCible, montantActuel,
  pourcentageAtteint. Fusionne en un seul objet ce que trois paramètres séparés transportaient
  auparavant (`List<Epargne>` + deux `List<Double>` parallèles).
- **`MouvementDTO`** (nouvelle classe, `application.dto`) : montant, sens, date — exactement ce
  qu'une ligne de mouvement affiche.
- **`IServiceEpargne`** : `getObjectifs()` renvoie `List<ObjectifDTO>` (une seule liste, plus de
  parallèle) ; `getObjectif(int)` renvoie `ObjectifDTO` ; nouvelle méthode `getMouvements(int)`
  renvoyant `List<MouvementDTO>`, qui remplace l'accès direct `objectif.getMouvements()` que
  `ControleurEpargne` faisait sur l'entité. `depasseraCible` prend désormais un `idObjectif`
  plutôt qu'un `Epargne`. `creerObjectif` passe de `Epargne` à `void`, même raisonnement que
  `ajouterDepense`/`ajouterRevenu` à la sous-étape précédente : la valeur créée n'était jamais
  lue par le contrôleur. `getMontantActuel`, `getPourcentageAtteint`, `getMontantsActuels`,
  `getPourcentagesAtteints` sortent entièrement de l'interface.
- **`ServiceEpargne`** : `getMontantActuel(Epargne)`/`getPourcentageAtteint(Epargne)` passent en
  `private` (encore utilisées en interne par `depasseraCible`, `retirerObjectif`,
  `supprimerObjectif`) ; `getMontantsActuels(List<Epargne>)`/`getPourcentagesAtteints(List<Epargne>)`
  sont supprimées, pas seulement rendues privées : plus aucun code, pas même interne, ne les
  appelait une fois `getObjectifs()` réécrite pour construire directement la liste d'`ObjectifDTO`.
  Deux méthodes privées `versAffichage()` (surchargées, une par type d'entité) construisent les
  DTO juste avant que chaque méthode publique ne les renvoie.
- **`VueEpargne`** : `afficherObjectifs()` reçoit une seule `List<ObjectifDTO>` au lieu du trio.
  `afficherMouvements()` reçoit `List<MouvementDTO>` et ne délègue plus à
  `mouvement.toString()` (implicite, via `"  " + mouvement`) : une nouvelle méthode privée
  `formaterMouvement(MouvementDTO)` reprend exactement la même mise en forme (signe, libellé
  contribution/retrait), mais dans la vue.
- **`MouvementEpargne`** : `toString()` supprimée, même raisonnement et même vérification
  préalable (`grep` sur `src/`) que pour `Transaction.toString()`.
- **`ControleurEpargne`** : les trois variables locales `Epargne objectif` deviennent
  `ObjectifDTO objectif`. `afficherListeObjectifs()` perd sa liste intermédiaire et son double
  appel à `getMontantsActuels`/`getPourcentagesAtteints`, remplacés par un seul appel à
  `serviceEpargne.getObjectifs()`. `afficherObjectifs()` (l'action détail) appelle
  `serviceEpargne.getMouvements(id)` au lieu de `objectif.getMouvements()`. Import de
  `domain.entity.Epargne` retiré : plus aucune méthode de cette classe ne déclare de type du
  domaine.

### Choix de conception

**Pourquoi `getMontantsActuels`/`getPourcentagesAtteints` sont supprimées plutôt que rendues
privées, contrairement à `getMontantActuel`/`getPourcentageAtteint`.** Vérifié par lecture
complète de `ServiceEpargne` avant de trancher : les versions "liste entière" n'avaient qu'un
seul appelant, `ControleurEpargne.afficherListeObjectifs()`, qui vient de disparaître avec la
fusion dans `getObjectifs()`. Les garder, même en `private`, aurait laissé deux méthodes mortes
dans la classe — contraire à la règle du projet de ne jamais garder de code que personne
n'appelle.

**Pourquoi `getObjectif(int)` ne renvoie-t-il pas aussi les mouvements, pour éviter un second
appel de service dans `ControleurEpargne.afficherObjectifs()` ?** Question posée avant d'écrire
le DTO : `ObjectifDTO` sert à la fois à la ligne de la liste (id, nom, montantCible,
montantActuel, pourcentageAtteint) et au nom affiché avant les mouvements du détail — mais
aucun de ces deux usages n'a besoin des mouvements eux-mêmes, sauf l'écran détail. Ajouter un
champ `mouvements` à `ObjectifDTO` uniquement pour ce second cas aurait chargé toutes les autres
utilisations (la liste, en particulier) d'une donnée qu'elles n'affichent jamais — contraire à
la règle "un DTO ne contient que ce que l'écran affiche réellement". Une méthode séparée,
`getMouvements(int idObjectif)`, ne coûte qu'un appel de service de plus, déjà le patron suivi
partout ailleurs dans ce contrôleur.

**Pourquoi les deux `versAffichage()` (une pour `Epargne`, une pour `MouvementEpargne`) portent
le même nom plutôt que des noms distincts.** Surcharge de méthode : Java choisit la bonne
version selon le type de l'argument, un mécanisme de base du langage, pas une astuce. Même
patron que `ServiceTransaction.versAffichage(Transaction)` de la sous-étape précédente ; deux
noms différents (`objectifVersAffichage`/`mouvementVersAffichage`) n'auraient rien clarifié de
plus, seulement allongé les noms.

**`estAtteint(Epargne objectif)` n'a pas été touchée.** Toujours du code mort (aucun appelant,
ni contrôleur ni vue — vérifié à nouveau par `grep`), toujours hors du périmètre de cette étape.
`IServiceEpargne` garde donc un import `domain.entity.Epargne`, uniquement pour cette méthode :
ce n'est pas une violation de la règle "aucune entité en présentation" (la règle porte sur
`presentation`, pas sur `application.service`, qui manipule forcément des entités en interne),
mais c'est signalé une seconde fois ici en attendant une décision explicite.

### Points à savoir défendre

- **Les trois écrans transactions et épargne respectent-ils maintenant la priorité 6 du
  `CLAUDE.md` ("préparer progressivement l'usage des DTO, pour éviter de faire circuler les
  entités partout") ?** Oui, vérifié concrètement : `grep -rn "^import domain\.entity"
  src/presentation/` ne renvoie plus rien. Avant cette étape, `VueTransaction` importait
  `Transaction`, `VueEpargne` importait `Epargne` et `MouvementEpargne`, `ControleurEpargne`
  importait `Epargne` — les quatre imports ont disparu au fil des deux sous-étapes.
- **`ObjectifDTO`/`MouvementDTO` recalculent-ils quelque chose eux-mêmes ?** Non : `montantActuel`
  et `pourcentageAtteint` arrivent déjà calculés par `ServiceEpargne` au moment de la
  construction du DTO ; ni `VueEpargne` ni le DTO ne refont le calcul, ils le lisent seulement.
- **Pourquoi `depasseraCible` retrouve-t-il l'objectif via `trouverObjectif(idObjectif)` plutôt
  que d'exiger l'`ObjectifDTO` déjà en main dans `ControleurEpargne` ?** Un DTO ne porte aucune
  méthode de calcul (règle du `CLAUDE.md` : "pas de calcul dedans"), donc il ne peut pas servir
  de base à `depasseraCible`, qui doit relire le montant cible et recalculer le montant actuel
  au moment de l'appel. Prendre un `idObjectif` et laisser le service retrouver l'entité est
  la seule option qui respecte à la fois "le DTO ne calcule rien" et "le contrôleur ne fait
  aucun calcul".

### Pièges rencontrés

Aucun — compilation propre du premier coup sur l'ensemble de `src/` (`javac -Xlint:all`), les
seuls avertissements restants (`serialVersionUID` sur les deux exceptions, une classe interne de
Gson) préexistaient déjà avant cette étape et ne concernent pas le code modifié ici.

### Reste à faire

Étape 4 terminée pour les deux écrans qui faisaient circuler des entités (transactions, épargne).
Catégories, statistiques et solde n'en avaient pas besoin : ils ne manipulaient déjà que des
énumérations et des types simples. Reste, sur demande : étape 5 (réduction des dépendances
autour de `ServicePortefeuille`), étape 6 (nettoyage final).

## 2026-08-27 — Correction : suppression d'`estAtteint()`, du code mort

### Ce qui a été écrit

`estAtteint(Epargne)` supprimée d'`IServiceEpargne` et de `ServiceEpargne`. Avec elle,
`domain.entity.Epargne` disparaît des imports d'`IServiceEpargne` : l'interface ne mentionne
plus aucune entité, uniquement des DTO et des types simples.

### Choix de conception

**Pourquoi supprimer plutôt que garder "au cas où".** Signalée deux fois comme code mort sans
appelant (ni contrôleur ni vue) lors des deux entrées précédentes. Une méthode publique sans
appelant n'est pas une fonctionnalité en réserve, c'est du code que personne ne peut expliquer
un mois plus tard sans deviner à quoi il devait servir — contraire à la règle du projet de ne
jamais garder de code qui ne se défend pas.

**Idée notée pour plus tard, pas implémentée : signaler visuellement qu'un objectif est
atteint.** À l'affichage de la liste (`VueEpargne.afficherObjectifs`), une ligne dont le
pourcentage atteint dépasse 100 % pourrait porter une marque distincte (un astérisque, un texte
"(atteint)"...). Ce n'est pas demandé aujourd'hui et n'a pas été codé : la donnée nécessaire
existe déjà telle quelle dans `ObjectifDTO.getPourcentageAtteint()` (>= 100 signifie atteint),
donc le jour où cette évolution est demandée, aucun nouveau champ ni nouvelle méthode de service
ne sera nécessaire — seule `VueEpargne` aurait à changer, ce qui reste cohérent avec la règle
"le formatage à l'affichage est du ressort de la vue".

### Reste à faire

Sur demande : étape 5 (réduction des dépendances autour de `ServicePortefeuille`), étape 6
(nettoyage final).
