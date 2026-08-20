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
