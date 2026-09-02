package infrastructure.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.enumeration.SensMouvement;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * EpargneRepository lit et écrit les tables epargne et mouvement_epargne. Les deux restent
    * dans le même repository, volontairement : un mouvement n'existe jamais sans son objectif
    * (mouvement_epargne.objectif_id, ON DELETE CASCADE dans sql/schema.sql), il n'est ni lu ni
    * écrit indépendamment. Un repository séparé pour mouvement_epargne forcerait celui-ci à en
    * dépendre pour charger l'historique de chaque objectif, une couche de plus pour deux
    * méthodes qui ne s'utilisent jamais l'une sans l'autre.
    *
    * Même principe que les autres repositories : une connexion par méthode publique
    * (try-with-resources), PreparedStatement systématique, pas d'interface.
*/
public class EpargneRepository {

    // Charge tous les objectifs avec leurs mouvements, au démarrage de l'application (voir
    // ChargeurPortefeuille). Une requête par objectif pour les mouvements plutôt qu'une jointure
    // unique : plus simple à lire et à expliquer, sans conséquence réelle sur les performances
    // pour le nombre d'objectifs d'un portefeuille personnel.
    public List<Epargne> chargerTous() {
        String requete = "SELECT id, nom, montant_cible, date_limite FROM epargne";
        List<Epargne> objectifs = new ArrayList<>();
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                int id = resultat.getInt("id");

                // date_limite est facultative (NULL possible) : contrairement aux autres dates
                // du projet, il faut vérifier avant de convertir.
                Date dateLimiteSql = resultat.getDate("date_limite");
                LocalDate dateLimite = (dateLimiteSql == null) ? null : dateLimiteSql.toLocalDate();

                Epargne objectif = new Epargne(id, resultat.getString("nom"), resultat.getDouble("montant_cible"), dateLimite);
                chargerMouvements(connexion, objectif);
                objectifs.add(objectif);
            }
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de charger les objectifs d'épargne depuis la base de données.", exception);
        }
        return objectifs;
    }

    // Appelée par chargerTous(), sur la même connexion : pas de nouvelle connexion par objectif.
    // L'identifiant technique de chaque ligne de mouvement_epargne (nécessaire à la table,
    // PostgreSQL exige une clé primaire) n'est jamais lu ici : MouvementEpargne n'en a pas
    // besoin, aucune règle du domaine ne cible un mouvement individuel par identifiant.
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

    // date_limite est facultative : setNull(..., Types.DATE) plutôt que setDate(..., null), qui
    // lèverait une NullPointerException — le pilote JDBC a besoin de savoir explicitement quel
    // type SQL donner à une valeur absente.
    public int ajouter(String nom, double montantCible, LocalDate dateLimite) {
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

    // Une seule instruction suffit : ON DELETE CASCADE (défini dans sql/schema.sql) supprime les
    // mouvements de l'objectif avec lui, la base s'en charge, pas besoin de les supprimer un par
    // un depuis Java.
    public void supprimer(int id) {
        String requete = "DELETE FROM epargne WHERE id = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, id);
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de supprimer l'objectif " + id + ".", exception);
        }
    }

    // Ne renvoie rien : aucune règle du domaine ne cible jamais un mouvement individuel par
    // identifiant, donc personne n'a besoin de celui généré par la base pour cette ligne (voir
    // la remarque de chargerMouvements()).
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
}
