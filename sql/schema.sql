-- Schéma PostgreSQL du Gestionnaire de porte-monnaie.

-- La présence d'une ligne vaut état actif.
CREATE TABLE categorie_active (
    categorie VARCHAR(30) PRIMARY KEY CHECK (categorie IN (
        'ALIMENTATION', 'TRANSPORT', 'LOGEMENT', 'LOISIRS', 'SANTE', 'ABONNEMENTS',
        'ENTRETIEN_VESTIMENTAIRE', 'COUTURE', 'AUTRE_DEPENSE', 'SALAIRE', 'BOURSE', 'AUTRE_REVENU'
    ))
);

-- categorie n'a volontairement aucune clé étrangère vers categorie_active : désactiver une
-- catégorie ne doit pas invalider les transactions déjà enregistrées.
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

CREATE TABLE epargne (
    id            SERIAL PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL CHECK (nom <> ''),
    montant_cible NUMERIC(12,2) NOT NULL CHECK (montant_cible > 0),
    date_limite   DATE NULL
);

-- ON DELETE CASCADE : supprimer un objectif supprime ses mouvements.
CREATE TABLE mouvement_epargne (
    id             SERIAL PRIMARY KEY,
    objectif_id    INTEGER NOT NULL REFERENCES epargne(id) ON DELETE CASCADE,
    montant        NUMERIC(12,2) NOT NULL CHECK (montant > 0),
    sens           VARCHAR(15) NOT NULL CHECK (sens IN ('CONTRIBUTION', 'RETRAIT')),
    date_mouvement DATE NOT NULL CHECK (date_mouvement <= CURRENT_DATE)
);
