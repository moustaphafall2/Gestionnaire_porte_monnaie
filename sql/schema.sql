-- Schéma PostgreSQL du Gestionnaire de porte-monnaie.
-- Une entité du domaine devient une table ; une composition (Epargne -> MouvementEpargne)
-- devient une clé étrangère avec ON DELETE CASCADE, puisqu'un mouvement n'a aucun sens sans
-- l'objectif auquel il appartient.
--
-- Les énumérations Java (type, categorie, sens) deviennent des colonnes VARCHAR contraintes par
-- CHECK plutôt que des types ENUM natifs PostgreSQL : un ENUM natif imposerait des conversions
-- explicites (CAST) à certains endroits du code JDBC, pour un bénéfice nul ici.
--
-- Aucune table "portefeuille" : l'application n'a et n'aura jamais qu'un seul portefeuille
-- (règle du projet, pas une limite provisoire), donc aucune table à ligne unique référencée
-- partout n'apporterait rien.

-- Catégories actuellement actives. Ce n'est pas une entité mais un ensemble (Set<Categorie> côté
-- Java) : la présence d'une ligne EST l'état "active", exactement comme un élément présent dans
-- un Set. Volontairement AUCUNE clé étrangère depuis transaction_financiere.categorie vers cette
-- table : la règle de gestion "désactiver une catégorie n'a aucun effet sur les transactions
-- déjà enregistrées" interdit qu'une transaction existante devienne invalide simplement parce
-- que sa catégorie a été désactivée depuis.
CREATE TABLE categorie_active (
    categorie VARCHAR(30) PRIMARY KEY CHECK (categorie IN (
        'ALIMENTATION', 'TRANSPORT', 'LOGEMENT', 'LOISIRS', 'SANTE', 'ABONNEMENTS',
        'ENTRETIEN_VESTIMENTAIRE', 'COUTURE', 'AUTRE_DEPENSE', 'SALAIRE', 'BOURSE', 'AUTRE_REVENU'
    ))
);

-- Renommée depuis "transaction" (mot réservé SQL, qu'il aurait fallu échapper dans certaines
-- requêtes) : transaction_financiere, sans ambiguïté avec le sens habituel du mot en base de
-- données (une transaction SQL).
-- L'identifiant est généré par la base (SERIAL) : les compteurs manuels de Portefeuille
-- disparaissent avec cette migration, PostgreSQL sait le faire lui-même.
CREATE TABLE transaction_financiere (
    id               SERIAL PRIMARY KEY,
    montant          NUMERIC(12,2) NOT NULL CHECK (montant > 0),
    type             VARCHAR(10) NOT NULL CHECK (type IN ('DEPENSE', 'REVENU')),
    categorie        VARCHAR(30) NOT NULL CHECK (categorie IN (
                         'ALIMENTATION', 'TRANSPORT', 'LOGEMENT', 'LOISIRS', 'SANTE', 'ABONNEMENTS',
                         'ENTRETIEN_VESTIMENTAIRE', 'COUTURE', 'AUTRE_DEPENSE', 'SALAIRE', 'BOURSE', 'AUTRE_REVENU'
                     )),
    date_transaction DATE NOT NULL CHECK (date_transaction <= CURRENT_DATE),
    description      VARCHAR(255) NOT NULL DEFAULT ''
);

-- Les mouvements d'épargne ne sont jamais mélangés aux transactions (règle de gestion) : ils
-- vivent dans leur propre table, jamais dans transaction_financiere.
CREATE TABLE epargne (
    id            SERIAL PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL CHECK (nom <> ''),
    montant_cible NUMERIC(12,2) NOT NULL CHECK (montant_cible > 0),
    date_limite   DATE NULL
);

-- Contrairement à l'entité Java actuelle, cette table a besoin d'un identifiant propre (une
-- clé primaire par ligne) : MouvementEpargne devra en gagner un aussi, ce qu'il n'avait pas
-- jusqu'ici (l'ordre dans la liste suffisait en JSON, plus en base).
-- ON DELETE CASCADE : supprimer un objectif supprime ses mouvements avec lui. Sans effet sur la
-- règle de gestion "suppression uniquement si vide" (vérifiée par le service avant l'appel), qui
-- ne dit pas "sans mouvement" : un objectif vide peut avoir eu des contributions et des retraits
-- qui s'annulent exactement, et ces lignes disparaissent avec l'objectif supprimé.
CREATE TABLE mouvement_epargne (
    id             SERIAL PRIMARY KEY,
    objectif_id    INTEGER NOT NULL REFERENCES epargne(id) ON DELETE CASCADE,
    montant        NUMERIC(12,2) NOT NULL CHECK (montant > 0),
    sens           VARCHAR(15) NOT NULL CHECK (sens IN ('CONTRIBUTION', 'RETRAIT')),
    date_mouvement DATE NOT NULL CHECK (date_mouvement <= CURRENT_DATE)
);
