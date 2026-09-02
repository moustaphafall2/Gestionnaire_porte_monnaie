package infrastructure.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * TransactionRepository lit et écrit la table transaction_financiere.
*/
public class TransactionRepository {

    public List<Transaction> chargerToutes() {
        String requete = "SELECT id, montant, type, categorie, date_transaction, description FROM transaction_financiere";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                // java.sql.Date -> LocalDate via toLocalDate(), jamais par une chaîne formatée
                // à la main.
                transactions.add(new Transaction(
                        resultat.getInt("id"),
                        resultat.getDouble("montant"),
                        TypeTransaction.valueOf(resultat.getString("type")),
                        Categorie.valueOf(resultat.getString("categorie")),
                        resultat.getDate("date_transaction").toLocalDate(),
                        resultat.getString("description")));
            }
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de charger les transactions depuis la base de données.", exception);
        }
        return transactions;
    }

    // RETURNING id : PostgreSQL renvoie l'identifiant généré par la colonne SERIAL directement
    // dans le résultat de l'INSERT.
    public int ajouter(double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description) {
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

    public void modifier(int id, double montant, Categorie categorie, LocalDate date, String description) {
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

    public void supprimer(int id) {
        String requete = "DELETE FROM transaction_financiere WHERE id = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setInt(1, id);
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de supprimer la transaction " + id + ".", exception);
        }
    }
}
