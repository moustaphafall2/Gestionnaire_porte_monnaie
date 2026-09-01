# Gestionnaire de porte-monnaie

Application console en Java pour gérer un porte-monnaie personnel : suivi des dépenses, des
revenus et des objectifs d'épargne. Projet d'apprentissage individuel, sans interface graphique.
Les données sont stockées dans une base PostgreSQL, accédée en JDBC sans ORM, et exécutée dans
un conteneur Docker.

## Fonctionnalités

- Consulter le solde disponible (recalculé, jamais stocké).
- Enregistrer, consulter, filtrer, modifier et supprimer des dépenses et des revenus.
- Activer ou désactiver des catégories parmi une liste prédéfinie.
- Créer des objectifs d'épargne, y contribuer, en retirer de l'argent, suivre leur progression
  et les supprimer.
- Consulter des statistiques par catégorie et par période.
- Chaque opération validée est persistée avant d'être appliquée en mémoire.

## Architecture

Le projet suit une architecture en couches, avec un contrôleur et une vue par écran, et une
interface par service métier exposé aux contrôleurs (`ServicePortefeuille` fait exception, voir
plus bas) :

```
src/
├── Main.java                   Point d'entrée : construit services, vues et contrôleurs, démarre la boucle
├── presentation/
│   ├── controller/              Enchaîne saisie (vue) → règle métier (service) → affichage (vue), un
│   │                             contrôleur par écran (Portefeuille, Transaction, Epargne, Categorie,
│   │                             Statistique), sans aucun traitement ni appel à un autre contrôleur
│   └── view/                    Affichage console et saisie utilisateur, une classe par écran
├── application/
│   ├── service/
│   │   ├── interfaces/           Une interface IServiceXxx par service consommé par un contrôleur
│   │   └── implementation/       Toute la logique métier et les calculs (ServiceTransaction, ServiceEpargne,
│   │                             ServiceCategorie, ServiceStatistique, ServiceSolde, ServicePortefeuille,
│   │                             CalculEpargne)
│   └── dto/                      Objets de transfert vers la présentation (TransactionDTO, ObjectifDTO,
│                                  MouvementDTO, StatistiqueDTO)
├── domain/
│   ├── entity/                   Portefeuille, Transaction, Epargne, MouvementEpargne — structure seulement
│   └── enumeration/              Categorie, TypeTransaction, SensMouvement
├── infrastructure/
│   └── persistence/              PortefeuilleRepository (interface), GestionnairePostgreSQL (implémentation
│                                  JDBC), ConnexionBaseDeDonnees — seules classes autorisées à parler à
│                                  PostgreSQL
└── exception/                    ErreurSauvegardeException, ErreurChargementException
```

Les entités ne portent que leur structure (attributs, constructeur, getters) : aucun calcul,
aucune règle qui dépend d'un autre objet. Les vues n'importent jamais une implémentation de
service — elles affichent et lisent, sans déclencher aucun traitement. Les contrôleurs
n'affichent jamais rien directement et ne contiennent aucun traitement : ils déclarent leurs
dépendances de service par le type de l'interface (`IServiceXxx`), appellent un service et
transmettent le résultat à la vue.

`ServicePortefeuille` détient le `Portefeuille` en mémoire et relaie chaque mutation déjà validée
vers `PortefeuilleRepository` (une méthode par opération : `enregistrerNouvelleTransaction()`,
`enregistrerNouvelObjectif()`...), toujours avant de faire évoluer la structure en mémoire. Il
n'a pas d'interface `IServiceXxx` : sa méthode `getDonnees()`, à visibilité de paquet, permet aux
autres services du même paquet de manipuler le `Portefeuille` — une interface Java ne peut pas
déclarer de méthode à cette visibilité.

## Dépendances

- Java (dernière version stable du JDK)
- [Pilote JDBC PostgreSQL](https://jdbc.postgresql.org/) fourni dans `lib/postgresql-42.7.4.jar`
  (pas de Maven/Gradle, dépendance ajoutée manuellement)
- [Docker](https://www.docker.com/) et Docker Compose, pour exécuter PostgreSQL et pgAdmin en
  conteneurs (voir `docker-compose.yml`)

## Compiler et lancer

Démarrer PostgreSQL (et pgAdmin) en conteneur, depuis la racine du projet :

```bash
docker compose up -d
```

Copier `db.properties.example` en `db.properties` (non versionné, voir `.gitignore`) et adapter
si besoin les paramètres de connexion aux valeurs choisies dans `docker-compose.yml`.

Compiler puis lancer l'application :

```bash
javac -cp lib/postgresql-42.7.4.jar -d bin $(find src -name "*.java") && java -cp bin:lib/postgresql-42.7.4.jar Main
```

La première partie compile les sources vers `bin/` en incluant le pilote JDBC dans le classpath ;
la seconde lance l'application.

VS Code : `.vscode/settings.json` déclare déjà `src/` comme dossier source et
`lib/**/*.jar` comme bibliothèque référencée.

## Données

Les données sont stockées dans une base PostgreSQL, exécutée dans le conteneur `db` déclaré par
`docker-compose.yml` (volume nommé, les données survivent au redémarrage du conteneur). Le schéma
est décrit dans `sql/schema.sql`. `GestionnairePostgreSQL` est la seule classe qui exécute du SQL,
toujours par requêtes préparées (`PreparedStatement`), jamais par concaténation de chaînes.

Les paramètres de connexion (`db.properties`, voir `db.properties.example` pour le modèle) ne
sont jamais écrits en dur dans le code et ne sont pas versionnés : ce sont des données locales à
chaque machine, pas du code source.

pgAdmin (interface web d'administration de PostgreSQL) est accessible sur
[http://localhost:5050](http://localhost:5050) une fois les conteneurs démarrés, avec les
identifiants déclarés dans `docker-compose.yml`.

## Documentation

Le dossier `docs/` contient la modélisation complète du projet (contexte, cas d'utilisation,
diagrammes, règles de gestion) et la spécification détaillée de chaque classe.
`docs/journal-developpement.md` retrace, étape par étape, les choix de conception faits pendant
le développement — en particulier la migration vers l'architecture en couches actuelle et le
passage du stockage JSON à PostgreSQL.
