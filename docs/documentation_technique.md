**GESTIONNAIRE DE PORTE-MONNAIE**

Documentation technique

# **1\. Contexte et objectifs**

Ce projet consiste à développer une application console en Java permettant à un utilisateur unique de gérer son porte-monnaie personnel : suivi des dépenses, des revenus et des objectifs d'épargne.

Il s'agit d'un projet d'apprentissage individuel, sans interface graphique dans un premier temps, avec pour objectifs pédagogiques :

- manipuler la programmation orientée objet en Java : classes, encapsulation, énumérations, collections d'objets ;
- apprendre la sérialisation de données au format JSON avec la bibliothèque Gson ;
- structurer un projet de façon propre et évolutive, en séparant la logique métier de l'affichage.

L'usage est strictement personnel : un seul utilisateur, un seul portefeuille, aucune synchronisation en ligne.

# **2\. Fonctionnalités**

## **2.1. Version 1**

- Consulter le solde disponible.
- Enregistrer une dépense : montant, catégorie, date, description facultative.
- Enregistrer un revenu : montant, catégorie, date, description facultative.
- Consulter l'historique complet des transactions.
- Filtrer l'historique par date, par catégorie ou par type.
- Modifier ou supprimer une transaction existante.
- Activer ou désactiver des catégories.
- Créer un objectif d'épargne.
- Contribuer à un objectif d'épargne, ou en retirer de l'argent.
- Consulter la progression d'un objectif : montant épargné, montant cible, pourcentage.
- Supprimer un objectif d'épargne vide.
- Consulter les statistiques : total dépensé par catégorie et par période.
- Sauvegarder et charger automatiquement les données dans un fichier JSON.

## **2.2. Évolutions envisagées**

- Budget mensuel par catégorie, avec alerte en cas de dépassement.
- Comparaison des revenus et des dépenses sur une période choisie.
- Interface graphique remplaçant la console.

# **3\. Règles de gestion**

Ces règles conditionnent directement le comportement du programme et doivent être respectées à l'implémentation.

| **Règle**                        | **Comportement attendu**                                                                                                                                                        |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Solde disponible                 | Total des revenus, diminué des dépenses et des sommes actuellement placées dans les objectifs d'épargne. Il n'est jamais stocké : il est recalculé.                             |
| Dépense supérieure au solde      | Autorisée. Le système avertit que le solde deviendra négatif, mais l'utilisateur peut confirmer : une dépense passée est un fait, la refuser conduirait à ne pas l'enregistrer. |
| Contribution supérieure au solde | Refusée. Un objectif d'épargne fonctionne comme un coffre : il est impossible d'y placer une somme dont on ne dispose pas.                                                      |
| Dépassement du montant cible     | Autorisé. Le système signale que la cible sera dépassée, sans bloquer l'opération.                                                                                              |
| Date d'une transaction           | Ne peut pas être postérieure à la date du jour. L'application enregistre ce qui a eu lieu, non ce qui est prévu.                                                                |
| Montant d'une opération          | Toujours strictement positif. Le sens de l'opération est porté par son type, jamais par le signe du montant.                                                                    |
| Suppression d'un objectif        | Possible uniquement si l'objectif est vide. L'utilisateur doit d'abord retirer les sommes épargnées, ce qui l'oblige à décider de leur destination.                             |
| Désactivation d'une catégorie    | Sans effet sur les transactions déjà enregistrées. La catégorie disparaît seulement des choix proposés.                                                                         |
| Validation et sauvegarde         | Chaque opération est confirmée par l'utilisateur, puis les données sont sauvegardées automatiquement. Aucune sauvegarde manuelle n'est demandée.                                |

# **4\. Gestion des catégories**

Les catégories sont prédéfinies dans le code : aucune saisie libre n'est possible, ce qui évite les doublons et les fautes de frappe. Le mécanisme fonctionne en deux temps.

- Une liste complète de catégories disponibles existe en arrière-plan, sous forme d'énumération.
- Au premier lancement, l'utilisateur sélectionne parmi cette liste celles qu'il souhaite utiliser : elles deviennent ses catégories actives.
- Au quotidien, il ne choisit que parmi ses catégories actives, ce qui réduit le menu à l'essentiel.
- À tout moment, il peut activer une catégorie supplémentaire en la sélectionnant par son numéro dans la liste complète, ou en désactiver une.

## **4.1. Liste complète des catégories**

| **Type** | **Catégories**                                                        |
| -------- | --------------------------------------------------------------------- |
| Dépenses | Alimentation, Transport, Logement, Loisirs, Santé, Abonnements, Autre |
| Revenus  | Salaire, Cadeau, Autre                                                |

# **5\. Modèle de données**

## **5.1. Transaction**

| **Champ**   | **Type**        | **Description**                                          |
| ----------- | --------------- | -------------------------------------------------------- |
| id          | int             | Identifiant unique de la transaction                     |
| montant     | double          | Montant de l'opération, toujours positif                 |
| type        | TypeTransaction | DEPENSE ou REVENU                                        |
| categorie   | Categorie       | Catégorie associée, choisie parmi les catégories actives |
| date        | LocalDate       | Date de l'opération                                      |
| description | String          | Note libre, facultative                                  |

## **5.2. Objectif d'épargne**

| **Champ**    | **Type**                     | **Description**                              |
| ------------ | ---------------------------- | -------------------------------------------- |
| id           | int                          | Identifiant unique de l'objectif             |
| nom          | String                       | Nom de l'objectif, par exemple Vacances      |
| montantCible | double                       | Montant à atteindre                          |
| dateLimite   | LocalDate                    | Date limite, facultative                     |
| mouvements   | List&lt;MouvementEpargne&gt; | Historique des contributions et des retraits |

Le montant actuellement épargné n'est pas un champ stocké : il est calculé à partir des mouvements, en soustrayant les retraits des contributions. Cette solution garantit qu'il ne peut jamais être en contradiction avec l'historique.

## **5.3. Mouvement d'épargne**

| **Champ** | **Type**      | **Description**                        |
| --------- | ------------- | -------------------------------------- |
| montant   | double        | Montant du mouvement, toujours positif |
| sens      | SensMouvement | CONTRIBUTION ou RETRAIT                |
| date      | LocalDate     | Date du mouvement                      |

Ces mouvements ne figurent pas dans l'historique des transactions : ils ne constituent ni une dépense ni un revenu, mais un déplacement d'argent entre le solde disponible et un objectif. Les inclure fausserait les statistiques de dépenses.

## **5.4. Portefeuille**

Le portefeuille regroupe l'ensemble des données de l'utilisateur : la liste des transactions, la liste des catégories actives et la liste des objectifs d'épargne. Le solde disponible et le total épargné sont des valeurs calculées, jamais stockées.

## **5.5. Énumérations**

| **Énumération** | **Valeurs**                                                                                                       |
| --------------- | ----------------------------------------------------------------------------------------------------------------- |
| TypeTransaction | DEPENSE, REVENU                                                                                                   |
| SensMouvement   | CONTRIBUTION, RETRAIT                                                                                             |
| Categorie       | Une valeur par catégorie disponible, chacune portant son libellé et le type de transaction auquel elle s'applique |

# **6\. Structure du fichier de sauvegarde**

Les données sont sérialisées avec Gson dans un fichier portefeuille.json, structuré comme suit :

{

"categoriesActives": \["ALIMENTATION", "TRANSPORT", "SALAIRE"\],

"transactions": \[

{

"id": 1,

"montant": 15000.0,

"type": "DEPENSE",

"categorie": "ALIMENTATION",

"date": "2026-08-10",

"description": "Courses de la semaine"

}

\],

"objectifs": \[

{

"id": 1,

"nom": "Vacances",

"montantCible": 500000.0,

"dateLimite": "2026-12-01",

"mouvements": \[

{ "montant": 120000.0, "sens": "CONTRIBUTION", "date": "2026-08-10" },

{ "montant": 20000.0, "sens": "RETRAIT", "date": "2026-08-12" }

\]

}

\]

}

# **7\. Architecture des classes**

| **Classe**          | **Rôle**                                                                                  |
| ------------------- | ----------------------------------------------------------------------------------------- |
| Main                | Point d'entrée du programme, lance le menu principal                                      |
| Menu                | Gère l'affichage console et la saisie utilisateur, sans aucune logique métier             |
| Portefeuille        | Classe centrale : transactions, catégories actives, objectifs, calcul du solde disponible |
| Transaction         | Représente une dépense ou un revenu                                                       |
| TypeTransaction     | Énumération : DEPENSE, REVENU                                                             |
| Categorie           | Énumération des catégories disponibles, avec leur libellé et leur type                    |
| Epargne             | Représente un objectif d'épargne et son historique de mouvements                          |
| MouvementEpargne    | Représente une contribution ou un retrait sur un objectif                                 |
| SensMouvement       | Énumération : CONTRIBUTION, RETRAIT                                                       |
| GestionnaireFichier | Sauvegarde et chargement du portefeuille au format JSON via Gson                          |

Le code est organisé en trois couches. La couche présentation regroupe Main et Menu, la couche métier regroupe Portefeuille, Transaction, Epargne, MouvementEpargne et les énumérations, la couche persistance se limite à GestionnaireFichier et au fichier JSON.

Les dépendances vont toujours du haut vers le bas : la couche métier ne connaît pas l'existence de la console. C'est ce qui permettra de remplacer l'interface console par une interface graphique sans réécrire la logique. La sauvegarde est déclenchée par le portefeuille lui-même après chaque opération validée, et non par le menu, afin qu'aucune modification ne puisse être oubliée.

# **8\. Menu console**

\=== GESTION DE PORTE-MONNAIE ===

1\. Voir le solde

2\. Ajouter une dépense

3\. Ajouter un revenu

4\. Voir l'historique des transactions

5\. Gérer mes objectifs d'épargne

6\. Gérer mes catégories

7\. Voir les statistiques

8\. Quitter

# **9\. Dépendances techniques**

- Java, dernière version stable du JDK.
- Gson, pour la sérialisation et la désérialisation JSON.
- Visual Studio Code comme environnement de développement.
- Git pour la gestion de versions.
- Aucune base de données : la persistance repose sur un fichier JSON local.

# **10\. Roadmap**

| **Étape** | **Contenu**                                                                                 |
| --------- | ------------------------------------------------------------------------------------------- |
| 1         | Classes de base : Transaction, Categorie, TypeTransaction, Portefeuille, et calcul du solde |
| 2         | Menu console, ajout et consultation des transactions                                        |
| 3         | Gestion des catégories actives, filtres, modification et suppression                        |
| 4         | Objectifs d'épargne et mouvements associés                                                  |
| 5         | Persistance JSON avec Gson                                                                  |
| 6         | Statistiques et finitions                                                                   |