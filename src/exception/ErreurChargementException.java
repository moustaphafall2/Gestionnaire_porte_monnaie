package exception;

/*
    * Levée quand le chargement échoue pour une raison qui ne peut pas s'absorber en repartant
    * simplement d'un portefeuille vide : connexion à PostgreSQL refusée, fichier db.properties
    * absent ou illisible, erreur SQL pendant la lecture (voir ConnexionBaseDeDonnees et
    * GestionnairePostgreSQL.charger()). À distinguer d'une base neuve sans aucune donnée : ce
    * cas-là est défensif par nature et ne lève jamais cette exception, charger() y répond
    * toujours par un portefeuille vide exploitable.
*/
public class ErreurChargementException extends RuntimeException {
    public ErreurChargementException(String message, Throwable cause) {
        super(message, cause);
    }
}
