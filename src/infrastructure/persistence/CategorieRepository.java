package infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import domain.enumeration.Categorie;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * CategorieRepository lit et écrit la table categorie_active.
*/
public class CategorieRepository {

    public Set<Categorie> chargerActives() {
        String requete = "SELECT categorie FROM categorie_active";
        Set<Categorie> actives = new HashSet<>();
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                actives.add(Categorie.valueOf(resultat.getString("categorie")));
            }
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de charger les catégories actives depuis la base de données.", exception);
        }
        return actives;
    }

    // ON CONFLICT DO NOTHING : activer une catégorie déjà active ne doit pas échouer.
    public void activer(Categorie categorie) {
        String requete = "INSERT INTO categorie_active (categorie) VALUES (?) ON CONFLICT DO NOTHING";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible d'activer la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }

    public void desactiver(Categorie categorie) {
        String requete = "DELETE FROM categorie_active WHERE categorie = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de désactiver la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }
}
