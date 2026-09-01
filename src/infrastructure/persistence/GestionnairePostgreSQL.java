package infrastructure.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.SensMouvement;
import domain.enumeration.TypeTransaction;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * GestionnairePostgreSQL est l'implémentation PostgreSQL/JDBC de PortefeuilleRepository,
    * celle qui remplacera GestionnaireFichier (JSON) une fois la migration terminée. Comme
    * GestionnaireFichier, elle isole toute la logique de lecture/écriture du reste de
    * l'application : ServicePortefeuille ne connaît que l'interface PortefeuilleRepository,
    * jamais cette classe ni le SQL qu'elle exécute.
    *
    * Chaque méthode ouvre sa propre connexion via ConnexionBaseDeDonnees.ouvrir() et la referme
    * (try-with-resources) : pas de connexion partagée gardée entre deux appels, plus simple à
    * raisonner pour une application mono-utilisateur qui n'exécute jamais deux opérations en
    * même temps.
    *
    * PreparedStatement systématiquement, même pour une requête sans paramètre : jamais de SQL
    * construit par concaténation de chaînes, c'est la règle du projet contre l'injection SQL.
*/
public class GestionnairePostgreSQL implements PortefeuilleRepository {

    // Reconstruit le portefeuille en mémoire à partir des quatre tables, en une seule fois au
    // démarrage de l'application. Contrairement à GestionnaireFichier, aucune "réparation après
    // chargement" n'est nécessaire ici : Portefeuille() initialise déjà ses listes vides dans
    // son constructeur, et chaque ligne lue passe par une méthode d'ajout de l'entité
    // (ajouterTransaction, activerCategorie, ajouterObjectif) — jamais par un contournement du
    // constructeur comme le fait Gson à la désérialisation.
    public Portefeuille charger() {
        Portefeuille portefeuille = new Portefeuille();

        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir()) {
            chargerCategoriesActives(connexion, portefeuille);
            chargerTransactions(connexion, portefeuille);
            chargerObjectifs(connexion, portefeuille);
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de charger le portefeuille depuis la base de données.", exception);
        }

        return portefeuille;
    }

    private void chargerCategoriesActives(Connection connexion, Portefeuille portefeuille) throws SQLException {
        String requete = "SELECT categorie FROM categorie_active";
        try (PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                portefeuille.activerCategorie(Categorie.valueOf(resultat.getString("categorie")));
            }
        }
    }

    private void chargerTransactions(Connection connexion, Portefeuille portefeuille) throws SQLException {
        String requete = "SELECT id, montant, type, categorie, date_transaction, description FROM transaction_financiere";
        try (PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                // java.sql.Date -> LocalDate via toLocalDate(), jamais par une chaîne formatée
                // à la main (piège connu du projet).
                Transaction transaction = new Transaction(
                        resultat.getInt("id"),
                        resultat.getDouble("montant"),
                        TypeTransaction.valueOf(resultat.getString("type")),
                        Categorie.valueOf(resultat.getString("categorie")),
                        resultat.getDate("date_transaction").toLocalDate(),
                        resultat.getString("description"));
                portefeuille.ajouterTransaction(transaction);
            }
        }
    }

    private void chargerObjectifs(Connection connexion, Portefeuille portefeuille) throws SQLException {
        String requete = "SELECT id, nom, montant_cible, date_limite FROM epargne";
        try (PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                int id = resultat.getInt("id");

                // date_limite est facultative (NULL possible) : contrairement aux autres dates
                // du projet, il faut vérifier avant de convertir.
                Date dateLimiteSql = resultat.getDate("date_limite");
                LocalDate dateLimite = (dateLimiteSql == null) ? null : dateLimiteSql.toLocalDate();

                Epargne objectif = new Epargne(id, resultat.getString("nom"), resultat.getDouble("montant_cible"), dateLimite);
                chargerMouvements(connexion, objectif);
                portefeuille.ajouterObjectif(objectif);
            }
        }
    }

    // Une requête par objectif plutôt qu'une jointure unique avec les mouvements : plus simple à
    // lire et à expliquer, sans conséquence réelle sur les performances pour le nombre
    // d'objectifs d'un portefeuille personnel. L'identifiant technique de chaque ligne de
    // mouvement_epargne (nécessaire à la table, PostgreSQL exige une clé primaire) n'est jamais
    // lu ici : MouvementEpargne n'en a pas besoin, aucune règle du domaine ne cible un mouvement
    // individuel par identifiant.
    private void chargerMouvements(Connection connexion, Epargne objectif) throws SQLException {
        String requete = "SELECT montant, sens, date_mouvement FROM mouvement_epargne WHERE objectif_id = ?";
        try (PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, objectif.getId());
            try (ResultSet resultat = instruction.executeQuery()) {
                while (resultat.next()) {
                    MouvementEpargne mouvement = new MouvementEpargne(
                            resultat.getDouble("montant"),
                            SensMouvement.valueOf(resultat.getString("sens")),
                            resultat.getDate("date_mouvement").toLocalDate());
                    objectif.ajouterMouvement(mouvement);
                }
            }
        }
    }

    // Idempotent, comme Set.add() : activer une catégorie déjà active ne doit pas échouer.
    // ON CONFLICT DO NOTHING est le mécanisme SQL standard pour ça — sans lui, la contrainte de
    // clé primaire sur categorie_active.categorie ferait échouer une seconde activation.
    public void activerCategorie(Categorie categorie) {
        String requete = "INSERT INTO categorie_active (categorie) VALUES (?) ON CONFLICT DO NOTHING";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible d'activer la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }

    // Idempotent aussi, comme Set.remove() : désactiver une catégorie déjà inactive ne supprime
    // simplement aucune ligne, sans erreur.
    public void desactiverCategorie(Categorie categorie) {
        String requete = "DELETE FROM categorie_active WHERE categorie = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de désactiver la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }

    // RETURNING id : PostgreSQL renvoie l'identifiant généré par la colonne SERIAL directement
    // dans le résultat de l'INSERT, en une seule instruction — plus simple à écrire et à lire
    // que getGeneratedKeys() (l'API JDBC générique, pensée pour rester portable entre bases de
    // données différentes, ce qui n'est pas un objectif ici).
    public int ajouterTransaction(double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description) {
        String requete = "INSERT INTO transaction_financiere (montant, type, categorie, date_transaction, description) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setDouble(1, montant);
            instruction.setString(2, type.name());
            instruction.setString(3, categorie.name());
            instruction.setDate(4, Date.valueOf(date));
            instruction.setString(5, description);
            try (ResultSet resultat = instruction.executeQuery()) {
                resultat.next();
                return resultat.getInt("id");
            }
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible d'ajouter la transaction.", exception);
        }
    }

    public void modifierTransaction(int id, double montant, Categorie categorie, LocalDate date, String description) {
        String requete = "UPDATE transaction_financiere SET montant = ?, categorie = ?, date_transaction = ?, description = ? WHERE id = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setDouble(1, montant);
            instruction.setString(2, categorie.name());
            instruction.setDate(3, Date.valueOf(date));
            instruction.setString(4, description);
            instruction.setInt(5, id);
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de modifier la transaction " + id + ".", exception);
        }
    }

    public void supprimerTransaction(int id) {
        String requete = "DELETE FROM transaction_financiere WHERE id = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, id);
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de supprimer la transaction " + id + ".", exception);
        }
    }

    // date_limite est facultative : setNull(..., Types.DATE) plutôt que setDate(..., null), qui
    // lèverait une NullPointerException — le pilote JDBC a besoin de savoir explicitement quel
    // type SQL donner à une valeur absente.
    public int ajouterObjectif(String nom, double montantCible, LocalDate dateLimite) {
        String requete = "INSERT INTO epargne (nom, montant_cible, date_limite) VALUES (?, ?, ?) RETURNING id";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, nom);
            instruction.setDouble(2, montantCible);
            if (dateLimite == null) {
                instruction.setNull(3, Types.DATE);
            } else {
                instruction.setDate(3, Date.valueOf(dateLimite));
            }
            try (ResultSet resultat = instruction.executeQuery()) {
                resultat.next();
                return resultat.getInt("id");
            }
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de créer l'objectif d'épargne.", exception);
        }
    }

    // Ne renvoie rien : contrairement à ajouterTransaction()/ajouterObjectif(), aucune règle du
    // domaine ne cible jamais un mouvement individuel par identifiant, donc personne n'a besoin
    // de celui généré par la base pour cette ligne (voir la remarque de chargerMouvements()).
    public void ajouterMouvement(int idObjectif, double montant, SensMouvement sens, LocalDate date) {
        String requete = "INSERT INTO mouvement_epargne (objectif_id, montant, sens, date_mouvement) VALUES (?, ?, ?, ?)";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, idObjectif);
            instruction.setDouble(2, montant);
            instruction.setString(3, sens.name());
            instruction.setDate(4, Date.valueOf(date));
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible d'enregistrer le mouvement sur l'objectif " + idObjectif + ".", exception);
        }
    }

    // Une seule instruction suffit : ON DELETE CASCADE (défini dans sql/schema.sql) supprime les
    // mouvements de l'objectif avec lui, la base s'en charge, pas besoin de les supprimer un par
    // un depuis Java.
    public void supprimerObjectif(int id) {
        String requete = "DELETE FROM epargne WHERE id = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, id);
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de supprimer l'objectif " + id + ".", exception);
        }
    }

    // Pas encore implémentée. La sous-étape suivante remplace sauvegarder(Portefeuille) par des
    // méthodes granulaires sur PortefeuilleRepository (une par mutation), conformément à
    // l'analyse validée — ce corps temporaire ne doit jamais être appelé avant cette réécriture :
    // GestionnairePostgreSQL n'est pas encore branchée dans Main, qui continue d'utiliser
    // GestionnaireFichier pour l'écriture.
    public void sauvegarder(Portefeuille portefeuille) {
        throw new UnsupportedOperationException(
                "GestionnairePostgreSQL.sauvegarder(Portefeuille) sera remplacée par des méthodes granulaires à la sous-étape suivante.");
    }
}
