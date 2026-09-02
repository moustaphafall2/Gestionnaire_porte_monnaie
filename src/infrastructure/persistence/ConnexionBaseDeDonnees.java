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
    * dans db.properties — jamais écrits en dur dans le code, voir db.properties.example à la
    * racine du projet pour le modèle. C'est le point d'entrée que chaque repository
    * (CategorieRepository, TransactionRepository, EpargneRepository) appelle pour obtenir sa
    * connexion, une par méthode.
    *
    * Le pilote PostgreSQL (lib/postgresql-42.7.4.jar) s'enregistre lui-même auprès de
    * DriverManager au chargement de la classe : mécanisme standard du JDBC depuis la version 4,
    * aucun Class.forName(...) explicite à écrire.
*/
public class ConnexionBaseDeDonnees {

    private static final String FICHIER_CONFIGURATION = "db.properties";

    // Classe utilitaire : uniquement des méthodes statiques, pas d'instance à créer.
    private ConnexionBaseDeDonnees() {
    }

    // Ouvre une nouvelle connexion JDBC vers PostgreSQL. Chaque appelant est responsable de la
    // refermer (try-with-resources), cette méthode ne garde aucune connexion ouverte pour elle-même.
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

    // Lit db.properties depuis la racine du projet. Absent ou illisible : ce n'est pas une
    // situation qu'on peut réparer en repartant d'une valeur par défaut (contrairement au JSON
    // absent au premier lancement), donc ErreurChargementException, pas de valeur de secours.
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
