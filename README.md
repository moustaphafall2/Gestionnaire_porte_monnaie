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

Le projet suit une architecture en couches, avec un contrôleur et une vue par écran, et une
interface par service :

```
src/
├── Main.java                   Point d'entrée : construit services, vues et contrôleurs, démarre la boucle
├── presentation/
│   ├── controller/              Enchaîne saisie (vue) → règle métier (service) → affichage (vue), un
│   │                             contrôleur par écran, plus ControleurConsole (reprise sur échec de
│   │                             sauvegarde, commune aux contrôleurs qui modifient des données)
│   └── view/                    Affichage console et saisie utilisateur, une classe par écran
├── application/
│   ├── service/
│   │   ├── interfaces/           Une interface IServiceXxx par service, déclarant ses méthodes publiques
│   │   └── implementation/       Toute la logique métier et les calculs (ServiceTransaction, ServiceEpargne,
│   │                             ServiceCategorie, ServiceStatistique, ServicePortefeuille, CalculEpargne)
│   └── dto/                      Objets de transfert vers la présentation (à venir)
├── domain/
│   ├── entity/                   Portefeuille, Transaction, Epargne, MouvementEpargne — structure seulement
│   └── enumeration/              Categorie, TypeTransaction, SensMouvement
├── infrastructure/
│   └── persistence/              GestionnaireFichier — seule classe autorisée à lire/écrire sur le disque
└── exception/                    ErreurSauvegardeException, ErreurChargementException
```

Les entités ne portent que leur structure (attributs, constructeur, getters) : aucun calcul,
aucune règle qui dépend d'un autre objet. Les vues n'importent jamais une implémentation de
service — elles affichent et lisent, sans déclencher aucun traitement. Les contrôleurs
n'affichent jamais rien directement et ne contiennent aucun traitement : ils déclarent leurs
dépendances de service par le type de l'interface (`IServiceXxx`), appellent un service et
transmettent le résultat à la vue. La sauvegarde est déclenchée par les services
(`ServicePortefeuille.sauvegarder()`), jamais par une entité ni par un contrôleur directement.

## Dépendances

- Java (dernière version stable du JDK)
- [Gson](https://github.com/google/gson) pour la sérialisation JSON, fournie dans `lib/gson-2.13.1.jar`
  (pas de Maven/Gradle, dépendance ajoutée manuellement)

## Compiler et lancer

Depuis la racine du projet :

```bash
javac -cp lib/gson-2.13.1.jar -d bin $(find src -name "*.java") && java -cp bin:lib/gson-2.13.1.jar Main
```

La première partie compile les sources vers `bin/` en incluant Gson dans le classpath ; la
seconde lance l'application.

VS Code : `.vscode/settings.json` déclare déjà `src/` comme dossier source et
`lib/**/*.jar` comme bibliothèque référencée.

## Données

Les données sont sauvegardées dans `portefeuille.json`, créé automatiquement à la racine du
projet (ou du dossier depuis lequel le programme est lancé) dès la première opération. Ce
fichier n'est pas versionné (voir `.gitignore`) : ce sont des données utilisateur, pas du code
source. L'écriture est atomique (fichier temporaire puis renommage) : une coupure en pleine
sauvegarde ne peut pas corrompre le fichier existant.

## Documentation

Le dossier `docs/` contient la modélisation complète du projet (contexte, cas d'utilisation,
diagrammes, règles de gestion) et la spécification détaillée de chaque classe.
`docs/journal-developpement.md` retrace, étape par étape, les choix de conception faits pendant
le développement — en particulier la migration vers l'architecture en couches actuelle.
