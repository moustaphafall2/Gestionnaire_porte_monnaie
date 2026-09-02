package infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import exception.ErreurChargementException;

/*
    * ConnexionBaseDeDonnees ouvre la connexion JDBC vers PostgreSQL, à partir des paramètres lus
    * dans db.properties (jamais écrits en dur, voir db.properties.example). Point d'entrée que
    * chaque repository appelle pour obtenir sa connexion, une par méthode.
*/
public class ConnexionBaseDeDonnees {

    private static final String FICHIER_CONFIGURATION = "db.properties";

    private ConnexionBaseDeDonnees() {
    }

    public static Connection ouvrir() {
        Properties parametres = lireConfiguration();
        String url = parametres.getProperty("url");
        String utilisateur = parametres.getProperty("utilisateur");
        String motDePasse = parametres.getProperty("motDePasse");

        try {
            return DriverManager.getConnection(url, utilisateur, motDePasse);
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de se connecter à la base de données.", exception);
        }
    }

    private static Properties lireConfiguration() {
        Properties parametres = new Properties();
        try (InputStream flux = Files.newInputStream(Path.of(FICHIER_CONFIGURATION))) {
            parametres.load(flux);
        } catch (IOException exception) {
            throw new ErreurChargementException("Impossible de lire le fichier de configuration : " + FICHIER_CONFIGURATION, exception);
        }
        return parametres;
    }
}
