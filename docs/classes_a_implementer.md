# Gestionnaire de Porte-Monnaie — Spécification des classes et énumérations

Ce document liste toutes les classes et énumérations à implémenter, avec leurs attributs, leurs méthodes, et une description détaillée du rôle de chacune. Il sert de base directe au codage.

---

## Sommaire

1. [TypeTransaction (enum)](#1-typetransaction-enum)
2. [SensMouvement (enum)](#2-sensmouvement-enum)
3. [Categorie (enum)](#3-categorie-enum)
4. [Transaction (classe)](#4-transaction-classe)
5. [MouvementEpargne (classe)](#5-mouvementepargne-classe)
6. [Epargne (classe)](#6-epargne-classe)
7. [Portefeuille (classe)](#7-portefeuille-classe)
8. [GestionnaireFichier (classe)](#8-gestionnairefichier-classe)
9. [Menu (classe)](#9-menu-classe)
10. [Main (classe)](#10-main-classe)

---

## 1. TypeTransaction (enum)

### Description
Énumération qui distingue les deux natures possibles d'une transaction. Elle porte le sens de l'opération : le montant d'une transaction est toujours positif, c'est le `TypeTransaction` qui indique s'il doit être ajouté ou soustrait du solde.

### Valeurs
| Valeur | Signification |
|---|---|
| `DEPENSE` | Sortie d'argent |
| `REVENU` | Entrée d'argent |

### Méthodes
Aucune méthode nécessaire au-delà de celles générées automatiquement par Java (`values()`, `valueOf()`).

---

## 2. SensMouvement (enum)

### Description
Énumération qui décrit le sens d'un mouvement d'épargne (différent d'une transaction : un mouvement d'épargne déplace de l'argent entre le solde disponible et un objectif, sans être une dépense ou un revenu).

### Valeurs
| Valeur | Signification |
|---|---|
| `CONTRIBUTION` | Argent placé depuis le solde disponible vers l'objectif |
| `RETRAIT` | Argent retiré de l'objectif vers le solde disponible |

### Méthodes
Aucune méthode nécessaire au-delà de celles générées automatiquement par Java.

---

## 3. Categorie (enum)

### Description
Énumération fermée qui représente la liste **complète** des catégories disponibles dans l'application (aucune saisie libre n'est permise). Chaque valeur porte un libellé lisible et le type de transaction auquel elle s'applique, ce qui permet de ne proposer à l'utilisateur que les catégories cohérentes avec le type d'opération en cours (par exemple, ne pas proposer "Salaire" pour une dépense).

Cette liste complète est distincte des "catégories actives" de l'utilisateur, qui sont un sous-ensemble géré au niveau du `Portefeuille`.

### Attributs (par valeur d'enum)
| Attribut | Type | Description |
|---|---|---|
| `libelle` | `String` | Nom affiché à l'utilisateur, ex. `"Alimentation"` |
| `type` | `TypeTransaction` | Type auquel la catégorie s'applique (`DEPENSE` ou `REVENU`) |

### Valeurs
| Valeur | Libellé | Type |
|---|---|---|
| `ALIMENTATION` | Alimentation | DEPENSE |
| `TRANSPORT` | Transport | DEPENSE |
| `LOGEMENT` | Logement | DEPENSE |
| `LOISIRS` | Loisirs | DEPENSE |
| `SANTE` | Santé | DEPENSE |
| `ABONNEMENTS` | Abonnements | DEPENSE |
| `AUTRE_DEPENSE` | Autre | DEPENSE |
| `SALAIRE` | Salaire | REVENU |
| `AUTRE_REVENU` | Autre | REVENU |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `getLibelle()` | `String` | Retourne le libellé lisible de la catégorie |
| `getType()` | `TypeTransaction` | Retourne le type de transaction associé à la catégorie |
| `static List<Categorie> parType(TypeTransaction type)` | `List<Categorie>` | Retourne toutes les catégories disponibles pour un type donné (utile pour proposer la bonne liste selon qu'on ajoute une dépense ou un revenu) |

---

## 4. Transaction (classe)

### Description
Représente un mouvement d'argent réel : une dépense ou un revenu. C'est l'entité centrale de l'historique financier de l'utilisateur. Une transaction est immuable une fois créée dans son principe (elle peut être modifiée ou supprimée explicitement par l'utilisateur, mais ne change jamais toute seule).

Règles de gestion associées :
- Le montant est toujours strictement positif ; le signe n'existe pas, c'est `type` qui porte le sens.
- La `date` ne peut jamais être postérieure à la date du jour (on n'enregistre que ce qui a eu lieu).
- La `categorie` doit être cohérente avec le `type` (ex. impossible d'associer `SALAIRE` à une `DEPENSE`).

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `id` | `int` | Identifiant unique de la transaction |
| `montant` | `double` | Montant de l'opération, toujours positif |
| `type` | `TypeTransaction` | Nature de l'opération : DEPENSE ou REVENU |
| `categorie` | `Categorie` | Catégorie associée, choisie parmi les catégories actives du portefeuille |
| `date` | `LocalDate` | Date à laquelle l'opération a eu lieu |
| `description` | `String` | Note libre, facultative (peut être `null` ou vide) |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `Transaction(int id, double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description)` | constructeur | Crée une transaction ; doit valider en interne que le montant est positif et que la date n'est pas future |
| `getId()` | `int` | Accesseur de l'identifiant |
| `getMontant()` | `double` | Accesseur du montant |
| `getType()` | `TypeTransaction` | Accesseur du type |
| `getCategorie()` | `Categorie` | Accesseur de la catégorie |
| `getDate()` | `LocalDate` | Accesseur de la date |
| `getDescription()` | `String` | Accesseur de la description |
| `setMontant(double montant)` | `void` | Modifie le montant, avec validation (positif) |
| `setCategorie(Categorie categorie)` | `void` | Modifie la catégorie |
| `setDate(LocalDate date)` | `void` | Modifie la date, avec validation (non future) |
| `setDescription(String description)` | `void` | Modifie la description |
| `toString()` | `String` | Représentation lisible pour l'affichage console (ex. `"[10/08/2026] Alimentation - Courses de la semaine : 15 000 FCFA"`) |

---

## 5. MouvementEpargne (classe)

### Description
Représente un déplacement d'argent entre le solde disponible et un objectif d'épargne : soit une **contribution** (l'utilisateur met de l'argent de côté), soit un **retrait** (l'utilisateur reprend de l'argent déjà épargné).

Un mouvement d'épargne n'est **jamais** comptabilisé dans l'historique des transactions et n'affecte jamais les statistiques de dépenses/revenus : ce n'est ni une dépense, ni un revenu, seulement un transfert interne.

Règles de gestion associées :
- Le montant est toujours strictement positif.
- Une contribution ne peut pas dépasser le solde disponible du portefeuille au moment de l'opération (on ne peut pas épargner de l'argent qu'on n'a pas).

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `montant` | `double` | Montant du mouvement, toujours positif |
| `sens` | `SensMouvement` | CONTRIBUTION ou RETRAIT |
| `date` | `LocalDate` | Date à laquelle le mouvement a eu lieu |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `MouvementEpargne(double montant, SensMouvement sens, LocalDate date)` | constructeur | Crée un mouvement ; valide que le montant est positif |
| `getMontant()` | `double` | Accesseur du montant |
| `getSens()` | `SensMouvement` | Accesseur du sens |
| `getDate()` | `LocalDate` | Accesseur de la date |
| `toString()` | `String` | Représentation lisible (ex. `"[10/08/2026] +120 000 FCFA (contribution)"`) |

---

## 6. Epargne (classe)

### Description
Représente un objectif d'épargne défini par l'utilisateur (ex. "Vacances", "Nouvel ordinateur"). Le montant actuellement épargné n'est **jamais stocké directement** : il est recalculé à partir de l'historique des `MouvementEpargne` (somme des contributions moins somme des retraits). Cette approche garantit que le montant affiché ne peut jamais être incohérent avec l'historique.

Règles de gestion associées :
- Une contribution supérieure au solde disponible du portefeuille est refusée.
- Un retrait supérieur au montant actuellement épargné dans l'objectif est refusé.
- Dépasser le montant cible est autorisé (le système avertit, sans bloquer).
- La suppression d'un objectif n'est possible que s'il est vide (montant actuel = 0) ; l'utilisateur doit d'abord tout retirer.

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `id` | `int` | Identifiant unique de l'objectif |
| `nom` | `String` | Nom de l'objectif, ex. "Vacances" |
| `montantCible` | `double` | Montant à atteindre |
| `dateLimite` | `LocalDate` | Date limite, facultative (peut être `null`) |
| `mouvements` | `List<MouvementEpargne>` | Historique des contributions et retraits sur cet objectif |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `Epargne(int id, String nom, double montantCible, LocalDate dateLimite)` | constructeur | Crée un objectif vide (liste de mouvements initialisée vide) |
| `getId()` | `int` | Accesseur de l'identifiant |
| `getNom()` | `String` | Accesseur du nom |
| `getMontantCible()` | `double` | Accesseur du montant cible |
| `getDateLimite()` | `LocalDate` | Accesseur de la date limite |
| `getMouvements()` | `List<MouvementEpargne>` | Retourne l'historique des mouvements (copie non modifiable recommandée) |
| `getMontantActuel()` | `double` | Calcule et retourne le montant actuellement épargné (somme des contributions − somme des retraits) |
| `getPourcentageAtteint()` | `double` | Calcule le pourcentage d'avancement (`montantActuel / montantCible * 100`) |
| `contribuer(double montant, LocalDate date)` | `void` | Ajoute un mouvement de type CONTRIBUTION ; la validation "montant ≤ solde disponible" est faite en amont, dans `Portefeuille`, car `Epargne` ne connaît pas le solde global |
| `retirer(double montant, LocalDate date)` | `void` | Ajoute un mouvement de type RETRAIT ; lève une exception si `montant > getMontantActuel()` |
| `estVide()` | `boolean` | Retourne `true` si `getMontantActuel() == 0`, condition nécessaire à la suppression de l'objectif |
| `estAtteint()` | `boolean` | Retourne `true` si `getMontantActuel() >= montantCible` |
| `toString()` | `String` | Représentation lisible (ex. `"Vacances : 120 000 / 500 000 FCFA (24%)"`) |

---

## 7. Portefeuille (classe)

### Description
Classe centrale de l'application. Elle regroupe l'ensemble des données de l'utilisateur (transactions, catégories actives, objectifs d'épargne) et porte toute la logique métier : c'est elle qui applique les règles de gestion, calcule le solde, et coordonne les opérations. Elle ne connaît rien de l'affichage console : elle expose des méthodes métier pures que la classe `Menu` viendra appeler.

Le solde disponible **n'est jamais stocké** : il est recalculé à chaque appel à partir des transactions et des objectifs, ce qui évite tout risque d'incohérence.

Règles de gestion associées (rappel, appliquées ici) :
- Solde disponible = total des revenus − total des dépenses − total actuellement placé dans les objectifs d'épargne.
- Une dépense supérieure au solde est autorisée après confirmation de l'utilisateur (avertissement de solde négatif).
- Une contribution supérieure au solde est refusée.
- La désactivation d'une catégorie n'affecte jamais les transactions déjà enregistrées.
- Chaque opération validée déclenche une sauvegarde automatique via `GestionnaireFichier`.

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `transactions` | `List<Transaction>` | Ensemble de toutes les transactions enregistrées |
| `categoriesActives` | `Set<Categorie>` | Sous-ensemble de `Categorie` que l'utilisateur a choisi d'utiliser |
| `objectifs` | `List<Epargne>` | Ensemble des objectifs d'épargne créés |
| `gestionnaireFichier` | `GestionnaireFichier` | Référence utilisée pour déclencher la sauvegarde après chaque opération |
| `prochainIdTransaction` | `int` | Compteur interne pour générer des identifiants uniques de transaction |
| `prochainIdObjectif` | `int` | Compteur interne pour générer des identifiants uniques d'objectif |

### Méthodes

**Solde et consultation**
| Méthode | Retour | Description |
|---|---|---|
| `getSoldeDisponible()` | `double` | Calcule le solde : total des revenus − total des dépenses − total actuellement épargné dans tous les objectifs |
| `getTotalEpargne()` | `double` | Calcule la somme des `getMontantActuel()` de tous les objectifs |

**Gestion des transactions**
| Méthode | Retour | Description |
|---|---|---|
| `ajouterDepense(double montant, Categorie categorie, LocalDate date, String description)` | `Transaction` | Crée et enregistre une dépense ; si elle rend le solde négatif, la méthode doit permettre à l'appelant (le `Menu`) de gérer la confirmation avant l'appel, ou retourner une information d'avertissement |
| `ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description)` | `Transaction` | Crée et enregistre un revenu |
| `modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription)` | `void` | Retrouve la transaction par id et met à jour ses champs |
| `supprimerTransaction(int id)` | `void` | Retire la transaction correspondant à l'id |
| `getHistorique()` | `List<Transaction>` | Retourne toutes les transactions, triées par date décroissante |
| `filtrerParDate(LocalDate debut, LocalDate fin)` | `List<Transaction>` | Retourne les transactions comprises dans l'intervalle de dates |
| `filtrerParCategorie(Categorie categorie)` | `List<Transaction>` | Retourne les transactions d'une catégorie donnée |
| `filtrerParType(TypeTransaction type)` | `List<Transaction>` | Retourne uniquement les dépenses ou uniquement les revenus |

**Gestion des catégories**
| Méthode | Retour | Description |
|---|---|---|
| `getCategoriesActives()` | `Set<Categorie>` | Retourne les catégories actuellement actives |
| `getCategoriesDisponibles()` | `List<Categorie>` | Retourne toutes les catégories de l'énumération `Categorie` non encore actives (utile pour proposer l'activation) |
| `activerCategorie(Categorie categorie)` | `void` | Ajoute une catégorie à `categoriesActives` |
| `desactiverCategorie(Categorie categorie)` | `void` | Retire une catégorie de `categoriesActives` ; ne modifie aucune transaction existante |

**Gestion des objectifs d'épargne**
| Méthode | Retour | Description |
|---|---|---|
| `creerObjectif(String nom, double montantCible, LocalDate dateLimite)` | `Epargne` | Crée un nouvel objectif d'épargne vide |
| `contribuerObjectif(int idObjectif, double montant, LocalDate date)` | `void` | Vérifie que `montant <= getSoldeDisponible()`, sinon refuse l'opération (règle de gestion) ; sinon appelle `Epargne.contribuer(...)` |
| `retirerObjectif(int idObjectif, double montant, LocalDate date)` | `void` | Appelle `Epargne.retirer(...)`, qui lève déjà l'exception si le retrait dépasse le montant épargné |
| `supprimerObjectif(int idObjectif)` | `void` | Vérifie que l'objectif est vide (`estVide()`), sinon refuse la suppression (règle de gestion) |
| `getObjectifs()` | `List<Epargne>` | Retourne la liste de tous les objectifs |

**Statistiques**
| Méthode | Retour | Description |
|---|---|---|
| `getTotalParCategorie(LocalDate debut, LocalDate fin)` | `Map<Categorie, Double>` | Calcule, pour une période donnée, le total dépensé par catégorie |
| `getTotalRevenusEtDepenses(LocalDate debut, LocalDate fin)` | `double[]` ou objet dédié | Calcule le total des revenus et le total des dépenses sur une période |

**Persistance**
| Méthode | Retour | Description |
|---|---|---|
| `sauvegarder()` | `void` | Délègue à `GestionnaireFichier.sauvegarder(this)` ; appelée automatiquement après chaque opération de modification validée |
| `static Portefeuille charger(GestionnaireFichier gf)` | `Portefeuille` | Charge un portefeuille existant depuis le fichier JSON, ou en crée un nouveau vide si aucun fichier n'existe |

---

## 8. GestionnaireFichier (classe)

### Description
Seule classe autorisée à lire ou écrire sur le disque. Elle isole toute la logique de sérialisation/désérialisation JSON (via Gson) du reste de l'application, ce qui permettrait de changer facilement de format de stockage plus tard (base de données, autre format) sans toucher à `Portefeuille`.

Elle doit gérer la conversion des types Java spécifiques (`LocalDate`, `enum`) vers et depuis JSON, ce qui nécessite de configurer des adaptateurs Gson (`GsonBuilder` avec `registerTypeAdapter`).

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `cheminFichier` | `String` | Chemin du fichier de sauvegarde, ex. `"portefeuille.json"` |
| `gson` | `Gson` | Instance configurée (avec les adaptateurs nécessaires pour `LocalDate`) |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `GestionnaireFichier(String cheminFichier)` | constructeur | Initialise le chemin et configure l'instance `Gson` |
| `sauvegarder(Portefeuille portefeuille)` | `void` | Sérialise le portefeuille en JSON et l'écrit dans le fichier |
| `charger()` | `Portefeuille` | Lit le fichier JSON et le désérialise en objet `Portefeuille` ; retourne un portefeuille vide si le fichier n'existe pas encore (premier lancement) |
| `fichierExiste()` | `boolean` | Vérifie si le fichier de sauvegarde existe déjà sur le disque |

---

## 9. Menu (classe)

### Description
Gère uniquement l'interaction avec l'utilisateur dans la console : affichage des options, lecture des saisies, affichage des résultats et des messages d'erreur ou de confirmation. Elle ne contient **aucune logique métier** : chaque action se traduit par un appel à une méthode de `Portefeuille`, qui seule décide si l'opération est valide.

C'est cette séparation stricte qui permettra, plus tard, de remplacer la console par une interface graphique sans toucher à la logique métier.

### Attributs
| Attribut | Type | Description |
|---|---|---|
| `portefeuille` | `Portefeuille` | Référence vers le portefeuille sur lequel agir |
| `scanner` | `Scanner` | Pour lire les entrées utilisateur au clavier |

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `Menu(Portefeuille portefeuille)` | constructeur | Initialise le menu avec le portefeuille à piloter |
| `lancer()` | `void` | Boucle principale : affiche le menu, lit le choix, exécute l'action correspondante, jusqu'à ce que l'utilisateur choisisse "Quitter" |
| `afficherMenuPrincipal()` | `void` | Affiche les options numérotées du menu |
| `gererVoirSolde()` | `void` | Affiche le solde disponible via `portefeuille.getSoldeDisponible()` |
| `gererAjouterDepense()` | `void` | Demande montant, catégorie (parmi les catégories actives de type DEPENSE), date, description ; avertit si le solde deviendra négatif et demande confirmation ; appelle `portefeuille.ajouterDepense(...)` |
| `gererAjouterRevenu()` | `void` | Demande montant, catégorie (type REVENU), date, description ; appelle `portefeuille.ajouterRevenu(...)` |
| `gererHistorique()` | `void` | Affiche l'historique, avec possibilité de filtrer (par date, catégorie ou type) |
| `gererModificationSuppressionTransaction()` | `void` | Demande l'id d'une transaction, propose de la modifier ou de la supprimer |
| `gererObjectifsEpargne()` | `void` | Sous-menu : créer, contribuer, retirer, consulter, supprimer un objectif |
| `gererCategories()` | `void` | Sous-menu : afficher catégories actives, activer une catégorie disponible, désactiver une catégorie active |
| `gererStatistiques()` | `void` | Demande une période, affiche le total par catégorie et le total revenus/dépenses |
| `demanderMontant(String message)` | `double` | Lit un montant au clavier avec validation (positif, format numérique valide) |
| `demanderDate(String message)` | `LocalDate` | Lit une date au clavier avec validation (format correct, non future) |
| `demanderCategorie(TypeTransaction type)` | `Categorie` | Affiche les catégories actives correspondant au type et lit le choix de l'utilisateur |
| `demanderConfirmation(String message)` | `boolean` | Affiche un message et lit une réponse oui/non |

---

## 10. Main (classe)

### Description
Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires (le `GestionnaireFichier`, le `Portefeuille` chargé ou créé, le `Menu`) et de démarrer la boucle principale. Elle ne contient aucune logique au-delà de cette initialisation.

### Attributs
Aucun (classe utilitaire avec uniquement une méthode `main`).

### Méthodes
| Méthode | Retour | Description |
|---|---|---|
| `static void main(String[] args)` | `void` | Crée le `GestionnaireFichier`, charge (ou initialise) le `Portefeuille`, crée le `Menu`, appelle `menu.lancer()` |

---

## Vue d'ensemble des dépendances entre classes

```
Main
 └── crée GestionnaireFichier
 └── crée/charge Portefeuille (dépend de GestionnaireFichier)
 └── crée Menu (dépend de Portefeuille)
      └── Menu.lancer() pilote toute l'application

Portefeuille
 ├── contient une List<Transaction>
 ├── contient un Set<Categorie>
 ├── contient une List<Epargne>
 │     └── Epargne contient une List<MouvementEpargne>
 └── utilise GestionnaireFichier pour sauvegarder après chaque opération

Transaction   → dépend de TypeTransaction et Categorie
Categorie     → dépend de TypeTransaction (chaque catégorie a un type)
MouvementEpargne → dépend de SensMouvement
```

La règle d'architecture à respecter : **Menu et Main dépendent de Portefeuille, jamais l'inverse.** Portefeuille et les classes métier (Transaction, Epargne, MouvementEpargne, Categorie) ne doivent jamais importer `Scanner` ni rien lié à l'affichage console.
