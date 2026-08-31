import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import exception.ErreurChargementException;
import infrastructure.persistence.ConnexionBaseDeDonnees;

/*
    * Programme de vérification, à lancer une seule fois pour confirmer que la connexion JDBC
    * vers PostgreSQL fonctionne, avant d'écrire le vrai repository. Ce n'est pas une classe de
    * l'architecture (ni domain, ni application, ni infrastructure, ni presentation) : un script
    * de test, à supprimer une fois la connexion validée.
*/
public class TesterConnexion {
    public static void main(String[] args) {
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                Statement instruction = connexion.createStatement();
                ResultSet resultat = instruction.executeQuery("SELECT 1")) {
            resultat.next();
            System.out.println("Connexion réussie. SELECT 1 renvoie : " + resultat.getInt(1));
        } catch (ErreurChargementException | SQLException exception) {
            System.out.println("Échec de connexion : " + exception.getMessage());
        }
    }
}
