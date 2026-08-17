# Gestionnaire de porte-monnaie

Application console en Java pour gérer un porte-monnaie personnel : suivi des dépenses, des
revenus et des objectifs d'épargne. Projet d'apprentissage individuel, sans interface graphique
ni base de données — les données sont sauvegardées localement dans un fichier JSON.

## Fonctionnalités

- Consulter le solde disponible (recalculé, jamais stocké).
- Enregistrer, consulter, filtrer, modifier et supprimer des dépenses et des revenus.
- Activer ou désactiver des catégories parmi une liste prédéfinie.
- Créer des objectifs d'épargne, y contribuer, en retirer de l'argent, suivre leur progression
  et les supprimer.
- Consulter des statistiques par catégorie et par période.
- Sauvegarde automatique après chaque opération validée.

## Architecture

Le projet est organisé en trois couches, avec des dépendances qui vont toujours vers le bas :

```
src/
├── presentation/   Main, Menu — affichage console et saisie utilisateur
├── metier/         Portefeuille, Transaction, Epargne, MouvementEpargne,
│                    Categorie, TypeTransaction, SensMouvement, ErreurSauvegardeException
└── persistance/     GestionnaireFichier — seule classe autorisée à lire/écrire sur le disque
```

La couche métier ne connaît rien de la console ni du fichier de sauvegarde : elle expose des
méthodes métier pures, appelées par `Menu`. La sauvegarde est déclenchée par `Portefeuille`
lui-même après chaque opération validée, jamais par `Menu`.

## Dépendances

- Java (dernière version stable du JDK)
- [Gson](https://github.com/google/gson) pour la sérialisation JSON, fournie dans `lib/gson-2.13.1.jar`
  (pas de Maven/Gradle, dépendance ajoutée manuellement)

## Compiler et lancer

Depuis la racine du projet :

```bash
javac -cp lib/gson-2.13.1.jar -d bin $(find src -name "*.java") && java -cp bin:lib/gson-2.13.1.jar presentation.Main
```

La première partie compile les sources vers `bin/` en incluant Gson dans le classpath ; la
seconde lance l'application.

VS Code : `.vscode/settings.json` déclare déjà `src/` comme dossier source et
`lib/**/*.jar` comme bibliothèque référencée.

## Données

Les données sont sauvegardées dans `portefeuille.json`, créé automatiquement à la racine du
projet (ou du dossier depuis lequel le programme est lancé) dès la première opération. Ce
fichier n'est pas versionné (voir `.gitignore`) : ce sont des données utilisateur, pas du code
source.

## Documentation

Le dossier `docs/` contient la modélisation complète du projet (contexte, cas d'utilisation,
diagrammes, règles de gestion) et la spécification détaillée de chaque classe.
