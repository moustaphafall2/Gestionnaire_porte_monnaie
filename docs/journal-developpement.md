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
